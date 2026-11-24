package hexlet.code;

//import javax.persistence.Entity;
//import javax.persistence.Table;
//import javax.persistence.Id;
//import javax.persistence.GeneratedValue;
//import javax.persistence.Column;
//import javax.persistence.GenerationType;
//import javax.persistence.PrePersist;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import jakarta.persistence.Transient;

@Entity
@Table(name = "urls")
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // id — генерируется БД

    @Column(name = "address", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // дата добавления на сайт

    /**
     * Устанавливает время создания перед сохранением сущности, если оно ещё не задано.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Url() {

    }
    @Transient
    private boolean insertedNew;

    public boolean insertedNew() {
        return insertedNew;
    }

    public void setInsertedNew(boolean insertedNew) {
        this.insertedNew = insertedNew;
    }

    public Url(String name) {
        this.name = name;
    }

    /**
     * Возвращает идентификатор сущности.
     *
     * @return идентификатор сущности или {@code null}, если он не задан
     */
    public Long getId() {
        return id;
    }
    /**
     * Возвращает имя.
     *
     * @return имя
     */
    public String getName() {
        return name;
    }
    /**
     * Возвращает дату и время создания.
     *
     * @return дата и время создания
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime inpCreatedAt) {
        createdAt = inpCreatedAt;
    }
    /**
     * Устанавливает имя.
     *
     * @param name новое имя
     */
    public void setName(String name) {
        this.name = name;
    }
}
