package hexlet.code;

public class UrlRow {
    private final Long id;
    private final String baseUrl;

    public UrlRow(Long id, String baseUrl) {
        this.id = id;
        this.baseUrl = baseUrl;
    }

    public Long getId() {
        return id;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
