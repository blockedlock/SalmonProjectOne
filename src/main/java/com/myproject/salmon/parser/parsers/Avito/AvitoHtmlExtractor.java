package com.myproject.salmon.parser.parsers.Avito;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.salmon.parser.ParsedProduct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;

/**
 * Достаёт имя и цену из HTML карточки Avito.
 * Предпочитает отображаемую цену ({@code formattedPrice} / {@code data-marker} {@code content}),
 * а не «старую» {@code item.price} до скидки.
 */
final class AvitoHtmlExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FALLBACK_NAME = "Без названия";

    ParsedProduct extract(String html) {
        Document doc = Jsoup.parse(html);
        assertNotBlocked(doc, html);

        JsonNode buyerItem = buyerItemFromHydration(html);

        String name = firstNonBlank(
                nameFromBuyerItem(buyerItem),
                nameFromJsonLd(doc),
                textOf(doc.selectFirst("[data-marker=item-view/title-info]")),
                textOf(doc.selectFirst("h1[data-marker=item-view/title-info], h1")),
                metaContent(doc, "og:title")
        );

        BigDecimal price = firstNonNull(
                priceFromBuyerItem(buyerItem),
                priceFromMarker(doc),
                priceFromItemprop(doc),
                priceFromJsonLd(doc)
        );

        if (price == null) {
            throw new IllegalStateException("Цена не найдена в HTML Avito");
        }

        return new ParsedProduct(name != null ? name : FALLBACK_NAME, price);
    }

    private void assertNotBlocked(Document doc, String html) {
        String title = doc.title() == null ? "" : doc.title().toLowerCase();
        String lower = html == null ? "" : html.toLowerCase();

        if (title.contains("доступ ограничен")
                || title.contains("проблема с ip")
                || lower.contains("js-firewall-form")
                || lower.contains("firewall-container")
                || doc.selectFirst("#inner-captcha, .js-form-captcha-image") != null) {
            throw new IllegalStateException("Avito вернул капчу / блокировку вместо карточки объявления");
        }
    }

    private BigDecimal priceFromMarker(Document doc) {
        Element el = doc.selectFirst("[data-marker=item-view/item-price]");
        if (el == null) {
            return null;
        }
        return parsePrice(firstNonBlank(el.attr("content"), el.text()));
    }

    private BigDecimal priceFromItemprop(Document doc) {
        Element el = doc.selectFirst("[itemprop=price]");
        if (el == null) {
            return null;
        }
        return parsePrice(firstNonBlank(el.attr("content"), el.text()));
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
            } catch (Exception ignored) {
                // следующий script
            }
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

    private JsonNode buyerItemFromHydration(String html) {
        JsonNode root = hydrationRoot(html);
        if (root == null) {
            return null;
        }
        JsonNode loaderData = root.get("loaderData");
        if (loaderData == null || !loaderData.isObject()) {
            return null;
        }
        for (JsonNode value : loaderData) {
            if (value != null && value.isObject()) {
                JsonNode buyerItem = value.get("buyerItem");
                if (buyerItem != null && buyerItem.isObject()) {
                    return buyerItem;
                }
            }
        }
        return null;
    }

    private JsonNode hydrationRoot(String html) {
        if (html == null) {
            return null;
        }
        int marker = html.indexOf("__staticRouterHydrationData");
        if (marker < 0) {
            return null;
        }
        int parseAt = html.indexOf("JSON.parse(", marker);
        if (parseAt < 0) {
            return null;
        }
        try (JsonParser parser = MAPPER.getFactory()
                .createParser(html.substring(parseAt + "JSON.parse(".length()))) {
            JsonNode literal = MAPPER.readTree(parser);
            if (literal == null || !literal.isTextual()) {
                return null;
            }
            return MAPPER.readTree(literal.asText());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String nameFromBuyerItem(JsonNode buyerItem) {
        if (buyerItem == null) {
            return null;
        }
        JsonNode item = buyerItem.get("item");
        if (item == null || !item.isObject()) {
            return null;
        }
        return textOrNull(item.get("title"));
    }

    private BigDecimal priceFromBuyerItem(JsonNode buyerItem) {
        if (buyerItem == null) {
            return null;
        }
        JsonNode item = buyerItem.get("item");
        if (item == null || !item.isObject()) {
            return null;
        }
        JsonNode formatted = item.get("formattedPrice");
        if (formatted == null || !formatted.isObject()) {
            return null;
        }
        // isHasValue=false — «цена не указана»
        if (formatted.has("isHasValue") && !formatted.get("isHasValue").asBoolean(true)) {
            return null;
        }
        // value — то, что видит покупатель; item.price часто старая цена до скидки
        return parsePrice(textOrNull(formatted.get("value")));
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
        if (node.isNumber()) {
            return node.asText();
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
