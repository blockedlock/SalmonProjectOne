package com.myproject.salmon.parser.parsers.Avito;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.PriceParser;
import com.myproject.salmon.parser.parsers.scrapingbee.ScrapingBeeClient;
import com.myproject.salmon.parser.parsers.scrapingbee.ScrapingBeeFetch;
import org.springframework.stereotype.Component;

@Component
public class AvitoPriceParser implements PriceParser {

    private final ScrapingBeeClient scrapingBeeClient;
    private final AvitoHtmlExtractor extractor = new AvitoHtmlExtractor();

    public AvitoPriceParser(ScrapingBeeClient scrapingBeeClient) {
        this.scrapingBeeClient = scrapingBeeClient;
    }

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("avito.ru");
    }

    @Override
    public ParsedProduct parse(String url) {
        try {
            String html = scrapingBeeClient.fetchHtml(ScrapingBeeFetch.avito(url));
            return extractor.extract(html);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга Avito: " + url, e);
        }
    }
}
