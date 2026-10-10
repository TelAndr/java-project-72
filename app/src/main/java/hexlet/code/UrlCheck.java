package hexlet.code;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
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

    /**
     * устанавливает значение даты и времени, если оно не установлено.
     *
     */
    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    private boolean insertedNew;

    public boolean insertedNew() {
        /**
         * метод определяет, добавлено ли новое значение.
         *
         * @return возвращает признак вставки нового значения
         */
        return insertedNew;
    }
    // --- getters/setters ---

    public Long getId() {
        /**
         * метод определяет, добавлено ли новое значение.
         *
         * @return возвращает значение автоинкремента
         */
        return id;
    }

    public Integer getStatusCode() {
        /**
         * метод определяет, какое значение статус кода в данный момент.
         *
         * @return возвращает код статуса
         */
        return statusCode;
    }
    public void setStatusCode(Integer statusCode) {
        /**
         * принимает значение статус кода для установки внутри класса
         *
         * @param statusCode входное значение статус кода
         */
        this.statusCode = statusCode;
    }

    public String getTitle() {
        /**
         * метод определяет, какое значение названия в данный момент.
         *
         * @return возвращает название
         */
        return title;
    }
    public void setTitle(String title) {
        /**
         * принимает значение названия для установки внутри класса
         *
         * @param title входное значение названия
         */
        this.title = title;
    }

    public String getH1() {
        /**
         * метод определяет, какое значение заголовка h1 в данный момент.
         *
         * @return возвращает заголовок h1
         */
        return h1;
    }
    public void setH1(String h1) {
        /**
         * принимает значение заголовка h1 для установки внутри класса
         *
         * @param h1 входное значение заголовка h1
         */
        this.h1 = h1;
    }

    public String getDescription() {
        /**
         * метод определяет, какое значение описания description в данный момент.
         *
         * @return возвращает описание description
         */
        return description;
    }
    public void setDescription(String description) {
        /**
         * принимает значение описания description для установки внутри класса
         *
         * @param description входное значение описания description
         */
        this.description = description;
    }

    public Url getUrl() {
        /**
         * метод определяет, какое значение url в данный момент.
         *
         * @return возвращает url
         */
        return url;
    }
    public void setUrl(Url url) {
        /**
         * принимает значение url для установки внутри класса
         *
         * @param url входное значение url
         */
        this.url = url;
    }

    public OffsetDateTime getCreatedAt() {
        /**
         * метод определяет, какое значение даты создания createdAt в данный момент.
         *
         * @return возвращает дату создания createdAt
         */
        return createdAt;
    }
    public void setCreatedAt(OffsetDateTime createdAt) {
        /**
         * принимает значение даты создания createdAt для установки внутри класса
         *
         * @param createdAt входное значение даты создания createdAt
         */
        this.createdAt = createdAt;
    }
}
