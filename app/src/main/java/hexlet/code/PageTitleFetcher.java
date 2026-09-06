package hexlet.code;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class PageTitleFetcher {

    public static String fetchTitle(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        return document.title();
    }
}

