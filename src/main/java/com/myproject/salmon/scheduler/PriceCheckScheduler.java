package com.myproject.salmon.scheduler;

import com.myproject.salmon.bot.MyBot;
import com.myproject.salmon.model.Product;
import com.myproject.salmon.parser.ParsedProduct;
import com.myproject.salmon.parser.PriceParser;
import com.myproject.salmon.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class PriceCheckScheduler {
    private final ProductService productService;
    private final List<PriceParser> priceParsers;
    private final MyBot bot;

    public PriceCheckScheduler(ProductService productService, List<PriceParser> priceParsers, MyBot bot) {
        this.productService = productService;
        this.priceParsers = priceParsers;
        this.bot = bot;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkPrices() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<Product> products = productService.getProductsNeedingCheck(threshold);
        log.info("Starting price check for {} products", products.size());

        int updatedCount = 0;
        for (Product product : products) {
            try {
                PriceParser parser = priceParsers.stream()
                        .filter(p -> p.supports(product.getUrl()))
                        .findFirst()
                        .orElse(null);

                if (parser == null) {
                    log.warn("No parser for url {}", product.getUrl());
                    continue;
                }

                ParsedProduct parsed = parser.parse(product.getUrl());
                BigDecimal oldPrice = product.getCurrentPrice();
                product.setLastCheckedAt(LocalDateTime.now());
                if ((product.getName() == null || product.getName().isBlank()) && parsed.name() != null) {
                    product.setName(parsed.name());
                }

                if (oldPrice == null) {
                    product.setCurrentPrice(parsed.price());
                    productService.createProduct(product);
                    updatedCount++;
                    log.info("Filled missing price for product {}: {}", product.getId(), parsed.price());
                    continue;
                }

                if (parsed.price().compareTo(oldPrice) != 0) {
                    product.setCurrentPrice(parsed.price());
                    productService.createProduct(product);
                    updatedCount++;
                    log.info("Price changed for product {}: {} -> {}", product.getId(), oldPrice, parsed.price());
                    notifyUser(product, "Цена на товар по ссылке " + product.getUrl()
                            + " изменилась. Старая цена: " + oldPrice + " Новая цена: " + parsed.price());
                } else {
                    productService.createProduct(product);
                }
            } catch (Exception e) {
                log.error("Failed to check product: {}", product.getUrl(), e);
            }
        }

        log.info("Price check completed. Updated {} products", updatedCount);
    }

    private void notifyUser(Product product, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(product.getUser().getChatId());
        message.setText(text);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send notification for product {}: {}", product.getId(), e.getMessage());
        }
    }
}
