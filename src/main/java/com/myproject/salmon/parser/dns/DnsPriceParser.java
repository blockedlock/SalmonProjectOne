package com.myproject.salmon.parser.dns;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.PriceParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DnsPriceParser implements PriceParser{

    @Override
    public boolean supports(String url) {
        return url.contains("dns-shop.ru");
    }

    @Override
    public ParsedProduct parse(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();
            Element priceEl = doc.selectFirst(".product-buy__price");
            Element nameEl = doc.selectFirst(".product-card-top__title");

            if (priceEl == null) {
                throw new RuntimeException("Цена не найдена");
            }

            String priceText = priceEl.text().replaceAll("[^0-9]","");
            BigDecimal price = new BigDecimal(priceText);
            String name = nameEl != null ? nameEl.text() : "Без названия";

            return new ParsedProduct(name, price);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга DNS: " + url, e);
        }
    }
}
