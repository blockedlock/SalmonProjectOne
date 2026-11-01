package com.myproject.salmon.parserTest.ozonTest;

import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.parsers.Ozon.OzonPriceParser;
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
class OzonPriceParserTest {

    private static final String URL = "https://www.ozon.ru/product/test-1";

    @Mock
    private ScrapingBeeClient scrapingBeeClient;

    private OzonPriceParser parser;

    @BeforeEach
    void setUp() {
        parser = new OzonPriceParser(scrapingBeeClient);
    }

    @Test
    void parse_jsonLd_returnsNameAndPrice() throws Exception {
        stubHtml(readFixture("ozon/product-jsonld.html"));

        ParsedProduct result = parser.parse(URL);

        assertEquals("Очиститель для стиральных машин TM04N92", result.name());
        assertEquals(new BigDecimal("349"), result.price());
    }

    @Test
    void parse_webPriceWidget_prefersHeadingAndStatePrice() throws Exception {
        stubHtml(readFixture("ozon/product-webprice.html"));

        ParsedProduct result = parser.parse(URL);

        assertEquals("Наушники беспроводные", result.name());
        assertEquals(new BigDecimal("2846"), result.price());
    }

    @Test
    void parse_jsonLdGraph_readsNameAndLowPrice() {
        stubHtml("""
                <html><head>
                <script type="application/ld+json">
                {"@graph":[
                  {"@type":"BreadcrumbList"},
                  {"@type":["Product"],"name":"Товар из графа","offers":[
                    {"@type":"Offer"},
                    {"price":"null","lowPrice":"450"}
                  ]}
                ]}
                </script>
                </head><body><h1>Другое имя</h1></body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Товар из графа", result.name());
        assertEquals(new BigDecimal("450"), result.price());
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
    void parse_blankJsonName_usesH1() {
        stubHtml("""
                <html><head><title>Карточка</title>
                <script type="application/ld+json">
                {"@type":"Product","name":"   ","offers":{"price":"10"}}
                </script>
                </head><body><h1>Имя из заголовка</h1></body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Имя из заголовка", result.name());
        assertEquals(new BigDecimal("10"), result.price());
    }

    @Test
    void parse_ogTitleAndItemprop_whenWidgetsMissing() {
        stubHtml("""
                <html><head>
                <title>Карточка</title>
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
    void parse_webPrice_usesCardPriceWhenPriceMissing() {
        stubHtml("""
                <html><head><title>Карточка</title></head><body>
                <div data-widget="webProductHeading"><h1>Цена по карте</h1></div>
                <div data-widget="webPrice" data-state='{"cardPrice":"2 592 ₽","originalPrice":"18900"}'></div>
                </body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Цена по карте", result.name());
        assertEquals(new BigDecimal("2592"), result.price());
    }

    @Test
    void parse_webPrice_brokenState_readsHeadline() {
        stubHtml("""
                <html><head><title>Карточка</title></head><body>
                <h1>Видимая цена</h1>
                <div data-widget="webPrice" data-state="broken">
                  <span class="tsHeadline600Large">1\u00A0234\u202F₽</span>
                </div>
                </body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Видимая цена", result.name());
        assertEquals(new BigDecimal("1234"), result.price());
    }

    @Test
    void parse_webPrice_readsOwnText() {
        stubHtml("""
                <html><head><title>Карточка</title></head>
                <body><h1>Свой текст</h1>
                <div data-widget="webPrice">3 000 ₽</div>
                </body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Свой текст", result.name());
        assertEquals(new BigDecimal("3000"), result.price());
    }

    @Test
    void parse_widgetWithoutPrice_fallsThroughToItemprop() {
        stubHtml("""
                <html><head><title>Карточка</title></head><body>
                <h1>Снаружи виджета</h1>
                <div data-widget="webPrice" data-state='{"originalPrice":"100"}'></div>
                <span itemprop="price" content="55"></span>
                </body></html>
                """);

        ParsedProduct result = parser.parse(URL);

        assertEquals("Снаружи виджета", result.name());
        assertEquals(new BigDecimal("55"), result.price());
    }

    @Test
    void parse_noPrice_throws() {
        stubHtml("""
                <html><head><title>Карточка</title></head>
                <body><h1>Без цены</h1><span itemprop="price">нет в наличии</span></body></html>
                """);

        RuntimeException error = assertThrows(RuntimeException.class, () -> parser.parse(URL));

        assertTrue(error.getCause().getMessage().contains("Цена не найдена"));
    }

    @Test
    void parse_realOzonHtml_usesJsonLdPrice() throws Exception {
        stubHtml(readFixture("ozon/product-real-stealth.html"));

        ParsedProduct result = parser.parse(URL);

        assertEquals("Очиститель для стиральных машин.TM04N92", result.name());
        assertEquals(new BigDecimal("180"), result.price());
    }

    @Test
    void parse_captcha_throws() throws Exception {
        stubHtml(readFixture("ozon/captcha.html"));

        RuntimeException error = assertThrows(RuntimeException.class, () -> parser.parse(URL));
        assertTrue(error.getCause().getMessage().contains("капчу"));
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
