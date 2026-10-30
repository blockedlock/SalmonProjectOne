package com.myproject.salmon.parser.parsers.Wildberries;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.PriceParser;
import com.myproject.salmon.parser.parsers.scrapingbee.ScrapingBeeClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class WildberriesPriceParser implements PriceParser {

    private final ScrapingBeeClient scrapingBeeClient;

    public WildberriesPriceParser(ScrapingBeeClient scrapingBeeClient) {
        this.scrapingBeeClient = scrapingBeeClient;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("wildberries.ru");
    }

    @Override
    public ParsedProduct parse(String url) {
        try {
            String html = scrapingBeeClient.fetchHtml(url);
            Document doc = Jsoup.parse(html);

            Element priceEl = doc.selectFirst(".priceBlockFinalPrice");
            Element nameEl = doc.selectFirst(".productTitle");

            if (priceEl == null) {
                throw new RuntimeException("Цена не найдена");
            }

            String priceText = priceEl.text().replaceAll("[^0-9]", "");
            BigDecimal price = new BigDecimal(priceText);
            String name = nameEl != null ? nameEl.text() : "Без названия";

            return new ParsedProduct(name, price);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга Wildberries: " + url, e);
        }
    }
}