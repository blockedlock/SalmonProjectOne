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
        List<Product> products = productService.getAllProducts();
        log.info("Starting price check for {} products", products.size());

        int updatedCount = 0;
        for (Product product : products) {
            try {
                PriceParser parser = priceParsers.stream()
                        .filter(p -> p.supports(product.getUrl()))
                        .findFirst()
                        .orElse(null);

                if (parser != null) {
                    ParsedProduct parsed = parser.parse(product.getUrl());
                    BigDecimal oldPrice = product.getCurrentPrice();

                    if (product.getCurrentPrice() == null || !parsed.price().equals(product.getCurrentPrice())) {
                        product.setCurrentPrice(parsed.price());
                        product.setLastCheckedAt(LocalDateTime.now());
                        productService.createProduct(product);
                        updatedCount++;

                        log.info("Price changed for product {}: {} -> {}", product.getId(), oldPrice, parsed.price());

                        SendMessage message = new SendMessage();
                        message.setChatId(product.getUser().getChatId());
                        message.setText("Цена на товар по ссылке " + product.getUrl() + " изменилась. Старая цена: " + oldPrice + " Новая цена: " + parsed.price());

                        try {
                            bot.execute(message);
                        } catch (TelegramApiException e) {
                            log.error("Failed to send notification for product {}: {}", product.getId(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to check product: {}", product.getUrl(), e);
            }
        }

        log.info("Price check completed. Updated {} products", updatedCount);
    }

}
