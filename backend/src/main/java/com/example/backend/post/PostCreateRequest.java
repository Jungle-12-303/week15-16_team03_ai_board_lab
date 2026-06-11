package com.example.backend.post;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PostCreateRequest {
    
    private String title;
    private String content;
    private String authorName;
    private List<String> tagNames;
}
