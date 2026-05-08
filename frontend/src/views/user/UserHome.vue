<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
const topics = ref([])
const loadTopic = async () => {
  try {
    const res = await request.get('/topic')
    topics.value = res.data || []

    // 添加"全部"选项到最前面
    topics.value.unshift({
      id: 0,
      topicName: '全部',
    })
  } catch (error) {
    console.error('获取话题列表失败:', error)
  }
}
onMounted(() => {
  loadTopic()
})

const searchForm = reactive({
  topicId: 0,
  keyword: '',
})

const changeTopicId = (id) => {
  searchForm.topicId = id
}
</script>

<template>
  <div class="type-container">
    <div class="type-scroll-wrapper">
      <div
        v-for="topic in topics"
        :key="topic.id"
        class="type-item"
        :class="{ active: searchForm.topicId === topic.id }"
        @click="changeTopicId(topic.id)"
      >
        {{ topic.topicName }}
      </div>
    </div>
  </div>
</template>
<style scoped>
.type-container {
  padding: 20px 0;
  overflow: hidden;
}
.type-scroll-wrapper {
  display: flex;
  flex-wrap: nowrap;
  gap: 10px;
  overflow-x: auto;
  padding: 5px 10px;
  /* 自定义滚动条样式 */
  &::-webkit-scrollbar {
    height: 6px;
  }
  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }
  &::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;
  }
  &::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
  }
}
.type-item {
  padding: 10px 20px;
  flex-shrink: 0; /* 防止项目收缩 */
  border-radius: 20px;
  cursor: pointer;
  transition:
    background-color 0.3s,
    color 0.3s;
  background-color: #f1f1f1;
  color: #333;
  font-size: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  white-space: nowrap; /* 防止文本换行 */
}
.type-item:hover {
  background-color: #e0e0e0;
}

.type-item.active {
  background-color: #ff6a00;
  color: #fff;
  font-weight: bold; /* 修正拼写错误 */
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}
</style>
