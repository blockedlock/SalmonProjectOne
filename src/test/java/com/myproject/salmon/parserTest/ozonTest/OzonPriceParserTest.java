package com.myproject.salmon.parserTest.ozonTest;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.parsers.Ozon.OzonPriceParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "file:.env")
class OzonPriceParserTest {

    @Autowired
    private OzonPriceParser parser;

    @Test
    void parseOzonProduct() {
        String url = "https://www.ozon.ru/product/ochistitel-dlya-stiralnyh-mashin-tm04n92-4020335290/?at=1uGp1ZUy5K451sFXUR8IdKkakchrVJyE&sh=goawjHj_JA";
        ParsedProduct result = parser.parse(url);
        System.out.println(result.name() + " → " + result.price());
    }
}
