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
import java.util.*;
import java.util.stream.Collectors;

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

        // 设置 parentId，如果为 null 则设为 0
        if (comment.getParentId() == null) {
            comment.setParentId(0);
        }

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
        if (flatList == null || flatList.isEmpty()) {
           return new ArrayList<>();
       }
       return buildCommentTree(flatList);

    }



    /**
     * 将平级评论列表构建为树形结构（支持多级嵌套）
     */
    private List<Comment> buildCommentTree(List<Comment> flatList) {
        if (flatList == null || flatList.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 创建 id -> Comment 的映射
        Map<Integer, Comment> commentMap = new HashMap<>();
        for (Comment comment : flatList) {
            comment.setChildren(new ArrayList<>());
            commentMap.put(comment.getId(), comment);
        }

        // 2. 构建树形结构
        List<Comment> rootComments = new ArrayList<>();
        for (Comment comment : flatList) {
            Integer parentId = comment.getParentId();
            // 如果 parentId 为 null 或 0，则是根评论
            if (parentId == null || parentId == 0) {
                rootComments.add(comment);
            } else {
                // 找到父评论，将当前评论添加到父评论的 children 中
                Comment parent = commentMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(comment);
                } else {
                    // 如果父评论不存在，也作为根评论处理
                    rootComments.add(comment);
                }
            }
        }

        // 3. 对每一层按时间排序
        sortCommentsByTime(rootComments);

        return rootComments;
    }

    /**
     * 递归按时间排序评论
     */
    private void sortCommentsByTime(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        // 按创建时间升序排序（旧的在上，新的在下）
        comments.sort(Comparator.comparing(Comment::getCreateTime));
        for (Comment comment : comments) {
            if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
                sortCommentsByTime(comment.getChildren());
            }
        }
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
        Map<String ,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        String role = (String) map.get("role");

        Comment comment = commentMapper.findById(id);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        Note note = noteMapper.findById(comment.getNoteId());
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }

        boolean isAuthor = note.getCreateUser().equals(userId);
        boolean isOwner = comment.getUserId().equals(userId);
        boolean isAdmin = role != null && role.equals("ADMIN");

        if (!isAuthor && !isOwner && !isAdmin) {
            throw new RuntimeException("无权限删除");
        }

        // 递归删除所有子评论
        deleteCommentAndChildren(id);

        if (note != null && note.getCommentsCount() > 0) {
            // 重新计算评论数
            int remainingCount = commentMapper.countByNoteId(comment.getNoteId());
            note.setCommentsCount(remainingCount);
            note.setUpdateTime(LocalDateTime.now());
            noteMapper.update(note);
        }
    }

    /**
     * 递归删除评论及其所有子评论
     */
    private void deleteCommentAndChildren(Integer commentId) {
        // 获取所有子评论
        List<Comment> children = commentMapper.findByParentId(commentId);
        if (children != null && !children.isEmpty()) {
            for (Comment child : children) {
                deleteCommentAndChildren(child.getId());
            }
        }
        // 删除当前评论
        commentMapper.delete(commentId);
    }

    @Override
    @Transactional
    public void like(Integer id) {
        Comment comment = commentMapper.findById(id);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
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