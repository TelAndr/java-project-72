package hexlet.code;

public class UrlRow {
    private final Long id;
    private final String baseUrl;

    public UrlRow(Long id, String baseUrl) {
        this.id = id;
        this.baseUrl = baseUrl;
    }

    /**
     * метод определяет, добавлено ли новое значение.
     *
     * @return возвращает значение автоинкремента
     */
    public Long getId() {
        return id;
    }

    /**
     * метод определяет, добавлено ли значение базового url.
     *
     * @return возвращает значение базового url
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}
