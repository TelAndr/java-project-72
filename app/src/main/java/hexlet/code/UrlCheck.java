import jakarta.persistence.*;
        import java.time.OffsetDateTime;

@Entity
@Table(name = "url_checks")
public class UrlCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // авто-инкремент (генерирует БД)

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "title")
    private String title;

    @Column(name = "h1")
    private String h1;

    // большие объёмы текста: используем TEXT
    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // связь с Url (Url 1 -> many UrlCheck)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    // --- getters/setters ---

    public Long getId() { return id; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getH1() { return h1; }
    public void setH1(String h1) { this.h1 = h1; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Url getUrl() { return url; }
    public void setUrl(Url url) { this.url = url; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
