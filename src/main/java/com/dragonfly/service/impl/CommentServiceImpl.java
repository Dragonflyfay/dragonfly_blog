package com.dragonfly.service.impl;
import com.dragonfly.mapper.CommentMapper;
import com.dragonfly.mapper.NoteMapper;
import com.dragonfly.pojo.Comment;
import com.dragonfly.pojo.Note;
import com.dragonfly.pojo.PageBean;
import com.dragonfly.service.CommentService;
import com.dragonfly.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 描述：评论service实现类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 17:11
 */
@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private NoteMapper noteMapper;


    @Override
    public void add(Comment comment) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");

        comment.setUserId(userId);
        comment.setLikesCount(0);
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.now());


        commentMapper.add(comment);


        Note note = noteMapper.findById(comment.getNoteId());
        if (note != null) {
            note.setCommentsCount(note.getCommentsCount() + 1);
            note.setUpdateTime(LocalDateTime.now());
            noteMapper.update(note);
        }
    }

    @Override
    public List<Comment> listByNoteId(Integer noteId) {
        List<Comment> flatList = commentMapper.listByNoteId(noteId);
        return buildCommentTree(flatList);
    }

    /**
     * 将平级评论列表构建为树形结构
     */
    private List<Comment> buildCommentTree(List<Comment> flatList) {
        if (flatList == null || flatList.isEmpty()) {
            return new ArrayList<>();
        }
        // parentId -> children 映射
        Map<Integer, List<Comment>> childrenMap = flatList.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 设置子评论
        for (Comment comment : flatList) {
            List<Comment> children = childrenMap.get(comment.getId());
            comment.setChildren(children != null ? children : new ArrayList<>());
        }

        // 返回顶层评论（parentId == null 或 parentId == 0）
        return flatList.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());
    }

    @Override
    public void update(Comment comment) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");

        Comment existingComment = commentMapper.findById(comment.getId());
        if (existingComment == null) {
            throw new RuntimeException("评论不存在");
        }

        if (!existingComment.getUserId().equals(userId)) {
            throw new RuntimeException("只能修改自己的评论");
        }


        commentMapper.update(comment);

    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Map<String ,Object>map=ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("id");
        String role=(String) map.get("role");

        Comment comment=commentMapper.findById(id);
        if(comment==null){
            throw new RuntimeException("评论不存在");
        }
        //只有笔记的作者或者发表评论的人或者管理员可以删除

        //是否是笔记作者
        Note note = noteMapper.findById(comment.getNoteId());
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }
        boolean isAuthor = note.getCreateUser().equals(userId);
        boolean isOwner = comment.getUserId().equals(userId);
        boolean isAdmin = role != null && role.equals("ADMIN");

        if(!isAuthor&&!isOwner&&!isAdmin){
            throw new RuntimeException("无权限删除");
        }
        commentMapper.delete(id);

        if(note!=null&&note.getCommentsCount()>0){
            note.setCommentsCount(note.getCommentsCount()-1);
            note.setUpdateTime(LocalDateTime.now());
            noteMapper.update(note);
        }



    }

    @Override
    @Transactional
    public void like(Integer id) {
        //查看用户是否点赞，一点赞则操作
        Comment comment = commentMapper.findById(id);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");


        commentMapper.incrementLikesCount(id);

    }

    @Override
    @Transactional
    public void unlike(Integer id) {
        commentMapper.decrementLikesCount(id);

    }

    @Override
    public PageBean<Comment> pageList(Integer pageNum, Integer pageSize, String keyword) {
        int offset = (pageNum - 1) * pageSize;
        List<Comment> items = commentMapper.listAll(offset, pageSize, keyword);
        int total = commentMapper.countAll(keyword);
        return new PageBean<>((long) total, items);
    }
}
