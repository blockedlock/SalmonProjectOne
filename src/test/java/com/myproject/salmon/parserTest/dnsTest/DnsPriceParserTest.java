package com.myproject.salmon.parserTest.dnsTest;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.dns.DnsPriceParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DnsPriceParserTest {

    @Autowired
    private DnsPriceParser parser;

    @Test
    void parseDnsProduct() {
        String url = "https://www.dns-shop.ru/product/f89a801b62bbd21a/69-smartfon-xiaomi-redmi-15-256-gb-cernyj/";
        ParsedProduct result = parser.parse(url);
        System.out.println(result.name() + " → " + result.price());
    }
}
