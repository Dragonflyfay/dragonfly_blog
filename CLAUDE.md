# 聊天功能支持 - 数据库扩展方案

既然后续要支持聊天，需要在现有 E-R 图基础上增加 **私信/聊天** 相关表。以下是完整的设计方案：

---

## 一、新增表结构

### 1. `chat_session`（聊天会话表）

记录两个用户之间的会话。

```sql
CREATE TABLE chat_session (
  id int UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user1_id int UNSIGNED NOT NULL COMMENT '参与者1（较小ID）',
  user2_id int UNSIGNED NOT NULL COMMENT '参与者2（较大ID）',
  last_message_id int UNSIGNED DEFAULT NULL COMMENT '最后一条消息ID',
  unread_count_user1 int DEFAULT 0 COMMENT 'user1未读数',
  unread_count_user2 int DEFAULT 0 COMMENT 'user2未读数',
  last_message_time datetime DEFAULT NULL COMMENT '最后消息时间',
  status tinyint DEFAULT 1 COMMENT '1=正常 0=已删除',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_users (user1_id, user2_id),
  INDEX idx_user1 (user1_id, update_time DESC),
  INDEX idx_user2 (user2_id, update_time DESC),
  FOREIGN KEY (user1_id) REFERENCES user(id),
  FOREIGN KEY (user2_id) REFERENCES user(id)
);
```

### 2. `chat_message`（聊天消息表）

存储每条消息内容。

```sql
CREATE TABLE chat_message (
  id int UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  session_id int UNSIGNED NOT NULL COMMENT '所属会话',
  sender_id int UNSIGNED NOT NULL COMMENT '发送者ID',
  receiver_id int UNSIGNED NOT NULL COMMENT '接收者ID',
  msg_type tinyint DEFAULT 1 COMMENT '1=文本 2=图片 3=语音 4=视频 5=文件',
  content text NOT NULL COMMENT '消息内容（文本或文件URL）',
  is_read tinyint DEFAULT 0 COMMENT '0=未读 1=已读',
  read_time datetime DEFAULT NULL COMMENT '阅读时间',
  status tinyint DEFAULT 1 COMMENT '1=正常 0=撤回 2=已删除',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_session (session_id, create_time DESC),
  INDEX idx_sender (sender_id, create_time DESC),
  INDEX idx_receiver_unread (receiver_id, is_read, create_time DESC),
  FOREIGN KEY (session_id) REFERENCES chat_session(id),
  FOREIGN KEY (sender_id) REFERENCES user(id),
  FOREIGN KEY (receiver_id) REFERENCES user(id)
);
```

### 3. `chat_session` 表触发更新 `last_message_id`

```sql
-- 插入新消息时自动更新会话的最后消息
DELIMITER //
CREATE TRIGGER trg_chat_message_after_insert
AFTER INSERT ON chat_message
FOR EACH ROW
BEGIN
  -- 更新会话的最后消息ID和时间
  UPDATE chat_session 
  SET last_message_id = NEW.id, 
      last_message_time = NEW.create_time,
      update_time = NEW.create_time
  WHERE id = NEW.session_id;
END//
DELIMITER ;
```

---

## 二、E-R 图关系补充

```
┌─────────┐          ┌──────────────┐          ┌─────────┐
│  user   │1───────* │ chat_session │ *───────1│  user   │
│  (sender)│         │              │          │(receiver)│
└─────────┘          └──────┬───────┘          └─────────┘
                            │
                            │ 1
                            │
                            │ *
                    ┌───────▼───────┐
                    │ chat_message  │
                    └───────────────┘
```

---

## 三、相关 API 接口设计

| 方法   | 路径                         | 说明                 | 权限         |
| ------ | ---------------------------- | -------------------- | ------------ |
| GET    | `/chat/sessions`             | 获取会话列表         | 登录         |
| DELETE | `/chat/session/{id}`         | 删除会话             | 登录         |
| GET    | `/chat/messages/{sessionId}` | 获取会话消息（分页） | 登录         |
| POST   | `/chat/message`              | 发送消息             | 登录         |
| DELETE | `/chat/message/{id}`         | 撤回/删除消息        | 登录(发送者) |
| PUT    | `/chat/message/{id}/read`    | 标记消息已读         | 登录         |
| PUT    | `/chat/session/{id}/read`    | 标记整个会话已读     | 登录         |

---

## 四、WebSocket 支持（实时聊天）

建议使用 WebSocket 实现消息实时推送。

### WebSocket 配置类

```java
package com.dragonfly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(), "/ws/chat")
                .setAllowedOrigins("*");
    }
}
```

### WebSocket 处理器（核心逻辑）

```java
package com.dragonfish.websocket;

import com.alibaba.fastjson.JSON;
import com.dragonfish.pojo.ChatMessage;
import com.dragonfish.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    // 在线用户连接池：userId -> WebSocketSession
    private static final Map<Integer, WebSocketSession> SESSION_POOL = new ConcurrentHashMap<>();

    @Autowired
    private ChatService chatService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从URL参数获取 userId (例如: ws://host/ws/chat?userId=1)
        String query = session.getUri().getQuery();
        Integer userId = extractUserId(query);
        if (userId != null) {
            SESSION_POOL.put(userId, session);
            System.out.println("用户 " + userId + " 已连接 WebSocket");
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        ChatWebSocketDTO dto = JSON.parseObject(payload, ChatWebSocketDTO.class);

        Integer senderId = dto.getSenderId();
        Integer receiverId = dto.getReceiverId();
        String content = dto.getContent();
        Integer msgType = dto.getMsgType();

        // 1. 保存消息到数据库
        ChatMessage saved = chatService.sendMessage(senderId, receiverId, content, msgType);

        // 2. 构造响应消息
        String response = JSON.toJSONString(saved);

        // 3. 发送给接收者（在线）
        WebSocketSession receiverSession = SESSION_POOL.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(new TextMessage(response));
        }

        // 4. 发送回执给发送者
        session.sendMessage(new TextMessage(response));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 移除断开连接的用户
        SESSION_POOL.values().removeIf(s -> s.getId().equals(session.getId()));
        System.out.println("WebSocket 连接关闭: " + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket 传输错误: " + exception.getMessage());
        SESSION_POOL.values().removeIf(s -> s.getId().equals(session.getId()));
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // 提取 userId 工具方法
    private Integer extractUserId(String query) {
        if (query == null) return null;
        String[] params = query.split("&");
        for (String param : params) {
            String[] kv = param.split("=");
            if (kv.length == 2 && "userId".equals(kv[0])) {
                try {
                    return Integer.valueOf(kv[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
```

### WebSocket DTO

```java
package com.dragonfish.pojo;

import lombok.Data;

@Data
public class ChatWebSocketDTO {
    private Integer senderId;    // 发送者ID
    private Integer receiverId;  // 接收者ID
    private String content;      // 消息内容
    private Integer msgType;     // 消息类型 1=文本 2=图片 3=语音
}
```

### WebSocket 前端使用（Vue 3）

```javascript
// composables/useWebSocket.js
import { ref, onMounted, onUnmounted } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo'

export function useWebSocket() {
  const ws = ref(null)
  const isConnected = ref(false)
  const messages = ref([])
  const userInfoStore = useUserInfoStore()

  const connect = () => {
    const userId = userInfoStore.info.id
    if (!userId) return

    ws.value = new WebSocket(`ws://your-domain.com/ws/chat?userId=${userId}`)

    ws.value.onopen = () => {
      isConnected.value = true
      console.log('WebSocket 已连接')
    }

    ws.value.onmessage = (event) => {
      const data = JSON.parse(event.data)
      messages.value.push(data)
      
      // 触发全局事件，通知其他组件
      window.dispatchEvent(new CustomEvent('newMessage', { detail: data }))
    }

    ws.value.onclose = () => {
      isConnected.value = false
      // 自动重连（5秒后）
      setTimeout(connect, 5000)
    }
  }

  const sendMessage = (receiverId, content, msgType = 1) => {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket 未连接')
      return
    }

    const payload = {
      senderId: userInfoStore.info.id,
      receiverId,
      content,
      msgType
    }
    ws.value.send(JSON.stringify(payload))
  }

  onMounted(connect)

  onUnmounted(() => {
    if (ws.value) {
      ws.value.close()
    }
  })

  return { isConnected, sendMessage, messages }
}
```

---

## 五、Redis 缓存优化（聊天场景）

聊天功能对性能要求较高，建议引入 Redis 缓存：

```java
@Service
public class ChatCacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SESSION_KEY = "chat:session:";
    private static final String UNREAD_KEY = "chat:unread:";

    // 缓存会话列表（用户进入聊天页面时加载）
    public void cacheSessions(Integer userId, List<ChatSessionDTO> sessions) {
        String key = SESSION_KEY + userId;
        redisTemplate.opsForValue().set(key, JSON.toJSONString(sessions), 5, TimeUnit.MINUTES);
    }

    // 缓存未读消息数（用于红点提示）
    public void cacheUnreadCount(Integer userId, int count) {
        redisTemplate.opsForValue().set(UNREAD_KEY + userId, String.valueOf(count), 1, TimeUnit.HOURS);
    }

    // 获取未读消息数
    public int getUnreadCount(Integer userId) {
        String value = redisTemplate.opsForValue().get(UNREAD_KEY + userId);
        return value == null ? 0 : Integer.parseInt(value);
    }
}
```

---

## 六、聊天功能模块划分（后端开发视角）

```
chat/
├── controller/
│   └── ChatController.java          # REST API
├── service/
│   ├── ChatService.java             # 业务逻辑
│   └── impl/
│       └── ChatServiceImpl.java
├── mapper/
│   ├── ChatSessionMapper.java
│   └── ChatMessageMapper.java
├── websocket/
│   └── ChatWebSocketHandler.java    # WebSocket 处理器
├── config/
│   └── WebSocketConfig.java
└── pojo/
    ├── ChatSession.java
    ├── ChatMessage.java
    └── ChatWebSocketDTO.java
```

---

## 七、前端页面规划

| 页面     | 路由             | 说明                           |
| -------- | ---------------- | ------------------------------ |
| 消息列表 | `/messages`      | 显示所有会话，类似微信消息列表 |
| 聊天窗口 | `/chat/{userId}` | 与指定用户的聊天界面           |
| 消息红点 | 全局             | 导航栏显示未读消息数量         |

## 

---

> 💡 **开发建议**：先实现 REST API 基础功能（发消息、查列表、已读标记），再接入 WebSocket 实现实时推送。WebSocket 可用 Redis 发布订阅做水平扩展。

