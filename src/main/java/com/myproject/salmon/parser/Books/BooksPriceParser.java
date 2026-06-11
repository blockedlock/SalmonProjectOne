package com.myproject.salmon.parser.Books;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.PriceParser;
import com.myproject.salmon.parser.scrapingbee.ScrapingBeeClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class BooksPriceParser implements PriceParser {

    private final ScrapingBeeClient scrapingBeeClient;

    public BooksPriceParser(ScrapingBeeClient scrapingBeeClient) {
        this.scrapingBeeClient = scrapingBeeClient;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("books.com");
    }

    @Override
    public ParsedProduct parse(String url) {
        try {
            String html = scrapingBeeClient.fetchHtml(url);
            Document doc = Jsoup.parse(html);

            Element priceEl = doc.selectFirst(".price_color");
            Element nameEl = doc.selectFirst("div.col-sm-6:nth-child(2) > h1:nth-child(1)");

            if (priceEl == null) {
                throw new RuntimeException("Цена не найдена");
            }

            String priceText = priceEl.text().replaceAll("[^0-9]", "");
            BigDecimal price = new BigDecimal(priceText);
            String name = nameEl != null ? nameEl.text() : "Без названия";

            return new ParsedProduct(name, price);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга books: " + url, e);
        }
    }
}