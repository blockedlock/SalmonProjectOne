package com.myproject.salmon.parser.parsers.Avito;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.PriceParser;
import com.myproject.salmon.parser.parsers.scrapingbee.ScrapingBeeClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class AvitoPriceParser implements PriceParser {

    private final ScrapingBeeClient scrapingBeeClient;

    public AvitoPriceParser(ScrapingBeeClient scrapingBeeClient) {
        this.scrapingBeeClient = scrapingBeeClient;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("avito.ru");
    }

    @Override
    public ParsedProduct parse(String url) {
        try {
            String html = scrapingBeeClient.fetchHtml(url, true, true);
            Document doc = Jsoup.parse(html);

            Element priceEl = doc.selectFirst("[data-marker=\"item-view/item-price\"]");
            Element nameEl = doc.selectFirst("[data-marker=\"item-view/title-info\"]");

            if (priceEl == null) {
                throw new RuntimeException("Цена не найдена");
            }

            String priceText = priceEl.text().replaceAll("[^0-9]", "");
            BigDecimal price = new BigDecimal(priceText);
            String name = nameEl != null ? nameEl.text() : "Без названия";

            return new ParsedProduct(name, price);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга Avito: " + url, e);
        }
    }
}