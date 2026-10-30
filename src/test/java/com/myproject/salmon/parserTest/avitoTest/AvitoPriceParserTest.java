package com.myproject.salmon.parserTest.avitoTest;

import com.myproject.salmon.parser.parsers.Avito.AvitoPriceParser;
import com.myproject.salmon.parser.ParsedProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "file:.env")
public class AvitoPriceParserTest {

    @Autowired
    private AvitoPriceParser parser;

    @Test
    void parseAvitoProduct() {
        String url = "https://www.avito.ru/krasnodar/igry_pristavki_i_programmy/xbox_series_s_bu_s_dvumya_geympadami_8414185383?context=H4sIAAAAAAAA_wEmANn_YToxOntzOjE6IngiO3M6MTY6Ik9Hc040dm1uWmI2R25tVlciO32m0D1kJgAAAA";
        ParsedProduct result = parser.parse(url);
        System.out.println(result.name() + " → " + result.price());
    }
}
