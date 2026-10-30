package com.myproject.salmon.parserTest.wildberriesTest;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.parsers.Wildberries.WildberriesPriceParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "file:.env")
class WildberriesPriceParserTest {

    @Autowired
    private WildberriesPriceParser parser;

    @Test
    void parseDnsProduct() {
        String url = "https://www.wildberries.ru/catalog/275134802/detail.aspx?targetUrl=MI";
        ParsedProduct result = parser.parse(url);
        System.out.println(result.name() + " → " + result.price());
    }
}
