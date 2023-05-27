package com.goodjob.domain.comment.entity;

import com.goodjob.domain.article.entity.Article;
import com.goodjob.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@SuperBuilder
@Getter
public class Comment {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @CreatedDate
    private LocalDateTime createDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;
    @ManyToOne
    private Member member;
    @ManyToOne
    private Article article;
    private String content;
    private Long likeCount;
    private boolean isDeleted;

    // TODO: 의존관계
}
