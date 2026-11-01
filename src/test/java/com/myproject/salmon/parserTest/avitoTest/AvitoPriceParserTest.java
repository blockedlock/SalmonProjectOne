package com.myproject.salmon.parserTest.avitoTest;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.parsers.Avito.AvitoPriceParser;
import com.myproject.salmon.parser.parsers.scrapingbee.ScrapingBeeClient;
import com.myproject.salmon.parser.parsers.scrapingbee.ScrapingBeeFetch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvitoPriceParserTest {

    private static final String URL = "https://www.avito.ru/moskva/item/1";

    @Mock
    private ScrapingBeeClient scrapingBeeClient;

    private AvitoPriceParser parser;

    @BeforeEach
    void setUp() {
        parser = new AvitoPriceParser(scrapingBeeClient);
    }

    @Test
    void parse_marker_prefersContentAttribute() throws Exception {
        stubHtml(readFixture("avito/product-marker.html"));

        ParsedProduct result = parser.parse(URL);

        assertEquals("Xbox Series S б/у с двумя геймпадами", result.name());
        assertEquals(new BigDecimal("25990"), result.price());
    }

    @Test
    void parse_jsonLd_returnsNameAndPrice() throws Exception {
        stubHtml(readFixture("avito/product-jsonld.html"));

        ParsedProduct result = parser.parse(URL);

        assertEquals("iPhone 15 128GB", result.name());
        assertEquals(new BigDecimal("74990"), result.price());
    }

    @Test
    void parse_hydration_usesFormattedPriceNotOldPrice() throws Exception {
        stubHtml(readFixture("avito/product-hydration.html"));

        ParsedProduct result = parser.parse(URL);

        assertEquals("MacBook Air M2", result.name());
        assertEquals(new BigDecimal("109990"), result.price());
    }

    @Test
    void parse_itemprop_whenMarkerMissing() {
        stubHtml("""
                <html><head><title>Карточка</title>
                <meta property="og:title" content="Из Open Graph">
                </head><body>
                <span itemprop="price" content="1500">999 ₽</span>
                </body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Из Open Graph", result.name());
        assertEquals(new BigDecimal("1500"), result.price());
    }

    @Test
    void parse_markerText_whenContentMissing() {
        stubHtml("""
                <html><head><title>Карточка</title></head><body>
                <h1 data-marker="item-view/title-info">Товар без content</h1>
                <span data-marker="item-view/item-price">12\u00A0345\u202F₽</span>
                </body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Товар без content", result.name());
        assertEquals(new BigDecimal("12345"), result.price());
    }

    @Test
    void parse_hydrationWithoutPrice_fallsThroughToMarker() {
        stubHtml("""
                <html><head><title>Карточка</title></head><body>
                <h1 data-marker="item-view/title-info">После hydration</h1>
                <span data-marker="item-view/item-price" content="777">777</span>
                <script>
                window.__staticRouterHydrationData = JSON.parse("{\\"loaderData\\":{\\"x\\":{\\"buyerItem\\":{\\"item\\":{\\"title\\":\\"Имя из state\\",\\"formattedPrice\\":{\\"isHasValue\\":false}}}}}}");
                </script>
                </body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Имя из state", result.name());
        assertEquals(new BigDecimal("777"), result.price());
    }

    @Test
    void parse_brokenJsonLd_skipsToNextScript() {
        stubHtml("""
                <html><head>
                <script type="application/ld+json">{ это не json</script>
                <script type="application/ld+json">
                {"@type":"Product","name":"Второй скрипт","offers":{"price":"99"}}
                </script>
                </head><body></body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Второй скрипт", result.name());
        assertEquals(new BigDecimal("99"), result.price());
    }

    @Test
    void parse_missingName_usesFallback() {
        stubHtml("""
                <html><head><title>Каталог</title></head>
                <body><span itemprop="price">77 ₽</span></body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Без названия", result.name());
        assertEquals(new BigDecimal("77"), result.price());
    }

    @Test
    void parse_noPrice_throws() {
        stubHtml("""
                <html><head><title>Карточка</title></head>
                <body><h1 data-marker="item-view/title-info">Без цены</h1>
                <span data-marker="item-view/item-price">Цена не указана</span>
                </body></html>
                """);

        RuntimeException error = assertThrows(RuntimeException.class, () -> parser.parse(URL));

        assertTrue(error.getCause().getMessage().contains("Цена не найдена"));
    }

    @Test
    void parse_blocked_throws() throws Exception {
        stubHtml(readFixture("avito/blocked.html"));

        RuntimeException error = assertThrows(RuntimeException.class, () -> parser.parse(URL));
        assertTrue(error.getCause().getMessage().contains("капчу")
                || error.getCause().getMessage().contains("блокировку"));
    }

    @Test
    void supports_avitoUrl() {
        assertTrue(parser.supports("https://www.avito.ru/moskva/item/1"));
    }

    private void stubHtml(String html) {
        when(scrapingBeeClient.fetchHtml(any(ScrapingBeeFetch.class))).thenReturn(html);
    }

    private String readFixture(String path) throws IOException {
        try (var in = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
