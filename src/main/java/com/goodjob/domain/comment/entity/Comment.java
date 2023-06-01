package com.goodjob.domain.comment.entity;

import com.goodjob.domain.BaseEntity;
import com.goodjob.domain.article.entity.Article;
import com.goodjob.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Comment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    private Member member;

    @ManyToOne
    private Article article;

    @Setter
    private String content;

    private Long likeCount;

    @Setter
    private boolean isDeleted;

    // TODO: 의존관계
}
