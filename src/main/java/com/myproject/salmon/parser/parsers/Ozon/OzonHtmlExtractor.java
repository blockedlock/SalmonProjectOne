package com.myproject.salmon.parser.parsers.Ozon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.salmon.parser.ParsedProduct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;

/**
 * Достаёт имя и цену из HTML карточки Ozon.
 * Не завязан на хеш-классы вроде {@code .pdp_j0b} — они меняются при каждой сборке фронта.
 */
final class OzonHtmlExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FALLBACK_NAME = "Без названия";

    ParsedProduct extract(String html) {
        Document doc = Jsoup.parse(html);

        String title = doc.title() == null ? "" : doc.title().toLowerCase();
        if (title.contains("captcha") || title.contains("antibot")) {
            throw new IllegalStateException("Ozon вернул капчу вместо карточки товара");
        }

        String name = firstNonBlank(
                nameFromJsonLd(doc),
                textOf(doc.selectFirst("[data-widget=webProductHeading]")),
                textOf(doc.selectFirst("h1")),
                metaContent(doc, "og:title")
        );

        BigDecimal price = firstNonNull(
                priceFromJsonLd(doc),
                priceFromWebPriceWidget(doc),
                priceFromItemprop(doc)
        );

        if (price == null) {
            throw new IllegalStateException("Цена не найдена в HTML Ozon");
        }

        return new ParsedProduct(name != null ? name : FALLBACK_NAME, price);
    }

    private BigDecimal priceFromJsonLd(Document doc) {
        for (Element script : doc.select("script[type=application/ld+json]")) {
            try {
                JsonNode product = findProduct(MAPPER.readTree(script.data()));
                if (product == null) {
                    continue;
                }
                BigDecimal price = priceFromOffers(product.get("offers"));
                if (price != null) {
                    return price;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String nameFromJsonLd(Document doc) {
        for (Element script : doc.select("script[type=application/ld+json]")) {
            try {
                JsonNode product = findProduct(MAPPER.readTree(script.data()));
                if (product != null && product.hasNonNull("name")) {
                    String name = product.get("name").asText();
                    if (!name.isBlank()) {
                        return name.trim();
                    }
                }
            } catch (Exception ignored) {
                // следующий script
            }
        }
        return null;
    }

    private JsonNode findProduct(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findProduct(child);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (!node.isObject()) {
            return null;
        }
        if (isProductType(node.get("@type"))) {
            return node;
        }
        return findProduct(node.get("@graph"));
    }

    private boolean isProductType(JsonNode typeNode) {
        if (typeNode == null || typeNode.isNull()) {
            return false;
        }
        if (typeNode.isArray()) {
            for (JsonNode item : typeNode) {
                if ("Product".equalsIgnoreCase(item.asText())) {
                    return true;
                }
            }
            return false;
        }
        return "Product".equalsIgnoreCase(typeNode.asText());
    }

    private BigDecimal priceFromOffers(JsonNode offers) {
        if (offers == null || offers.isNull()) {
            return null;
        }
        if (offers.isArray()) {
            for (JsonNode offer : offers) {
                BigDecimal price = priceFromOffers(offer);
                if (price != null) {
                    return price;
                }
            }
            return null;
        }
        BigDecimal price = parsePrice(textOrNull(offers.get("price")));
        if (price != null) {
            return price;
        }
        return parsePrice(textOrNull(offers.get("lowPrice")));
    }

    private BigDecimal priceFromWebPriceWidget(Document doc) {
        Element widget = doc.selectFirst("[data-widget=webPrice]");
        if (widget == null) {
            return null;
        }

        String state = widget.attr("data-state");
        if (!state.isBlank()) {
            try {
                JsonNode json = MAPPER.readTree(state);
                BigDecimal fromState = firstNonNull(
                        parsePrice(textOrNull(json.get("price"))),
                        parsePrice(textOrNull(json.get("cardPrice")))
                );
                if (fromState != null) {
                    return fromState;
                }
            } catch (Exception ignored) {
                // ниже пробуем видимый текст виджета
            }
        }

        Element headline = widget.selectFirst(".tsHeadline600Large, [itemprop=price]");
        if (headline != null) {
            BigDecimal fromHeadline = parsePrice(firstNonBlank(headline.attr("content"), headline.text()));
            if (fromHeadline != null) {
                return fromHeadline;
            }
        }

        return parsePrice(widget.ownText());
    }

    private BigDecimal priceFromItemprop(Document doc) {
        Element el = doc.selectFirst("[itemprop=price]");
        if (el == null) {
            return null;
        }
        return parsePrice(firstNonBlank(el.attr("content"), el.text()));
    }

    static BigDecimal parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        return new BigDecimal(digits);
    }

    private static String textOf(Element el) {
        if (el == null) {
            return null;
        }
        String text = el.text();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static String metaContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=" + property + "]");
        if (meta == null) {
            return null;
        }
        String content = meta.attr("content");
        return content.isBlank() ? null : content.trim();
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() || "null".equals(text) ? null : text;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}