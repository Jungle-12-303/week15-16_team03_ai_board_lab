package com.example.backend.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.backend.post.Post;

public interface CommentRepository extends JpaRepository<Comment, Long>{
    List<Comment>findByPost(Post post);
}