package com.myproject.salmon.parserTest.magnitTest;

import com.myproject.salmon.parser.parsers.Magnit.MagnitPriceParser;
import com.myproject.salmon.parser.ParsedProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "file:.env")
class MagnitPriceParserTest {

    @Autowired
    private MagnitPriceParser parser;

    @Test
    void parseMagnitProduct() {
        String url = "https://magnit.ru/product/1000456935-pf_iz_ryby_forel_raduzhnaya_500_iz_zam_syrya_kulinariya";
        ParsedProduct result = parser.parse(url);
        System.out.println(result.name() + " → " + result.price());
    }
}
