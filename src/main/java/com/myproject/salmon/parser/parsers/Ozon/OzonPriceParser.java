package com.myproject.salmon.parser.parsers.Ozon;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.PriceParser;
import com.myproject.salmon.parser.parsers.scrapingbee.ScrapingBeeClient;
import com.myproject.salmon.parser.parsers.scrapingbee.ScrapingBeeFetch;
import org.springframework.stereotype.Component;

@Component
public class OzonPriceParser implements PriceParser {

    private final ScrapingBeeClient scrapingBeeClient;
    private final OzonHtmlExtractor extractor = new OzonHtmlExtractor();

    public OzonPriceParser(ScrapingBeeClient scrapingBeeClient) {
        this.scrapingBeeClient = scrapingBeeClient;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("ozon.ru");
    }

    @Override
    public ParsedProduct parse(String url) {
        try {
            String html = scrapingBeeClient.fetchHtml(ScrapingBeeFetch.ozon(url));
            return extractor.extract(html);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга Ozon: " + url, e);
        }
    }
}
