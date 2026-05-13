package app.entities;

import app.entities.enums.ContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "content")
@NamedQueries({
        @NamedQuery(
                name = "Content.findAllActive",
                query = "SELECT c FROM Content c WHERE c.active = true"
        ),
        @NamedQuery(
                name = "Content.findByType",
                query = "SELECT c FROM Content c WHERE c.contentType = :type"
        )
})
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    private String category;

    private String source;

    private String author;

    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        active = true;
    }
}
