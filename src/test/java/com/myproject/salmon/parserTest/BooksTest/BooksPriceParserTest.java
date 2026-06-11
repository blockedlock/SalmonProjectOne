package com.myproject.salmon.parserTest.BooksTest;

import com.myproject.salmon.parser.Books.BooksPriceParser;
import com.myproject.salmon.parser.ParsedProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "file:.env")
public class BooksPriceParserTest {

    @Autowired
    private BooksPriceParser parser;

    @Test
    void parseBooksProduct() {
        String url = "https://books.toscrape.com/catalogue/a-light-in-the-attic_1000/index.html";
        ParsedProduct result = parser.parse(url);
        System.out.println(result.name() + " → " + result.price());
    }
}
