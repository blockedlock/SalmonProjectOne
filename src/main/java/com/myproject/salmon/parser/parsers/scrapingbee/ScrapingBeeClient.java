package com.myproject.salmon.parser.parsers.scrapingbee;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class ScrapingBeeClient {

    private static final String BASE_URL = "https://app.scrapingbee.com/api/v1/";

    @Value("${scrapingbee.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String fetchHtml(String targetUrl) {
        return fetchHtml(ScrapingBeeFetch.of(targetUrl));
    }

    public String fetchHtml(String targetUrl, boolean renderJs) {
        return fetchHtml(new ScrapingBeeFetch(targetUrl, renderJs, false, false, 0, null));
    }

    public String fetchHtml(String targetUrl, boolean renderJs, boolean premiumProxy) {
        return fetchHtml(new ScrapingBeeFetch(targetUrl, renderJs, premiumProxy, false, 0, null));
    }

    public String fetchHtml(String targetUrl, boolean renderJs, boolean premiumProxy, int waitMs) {
        return fetchHtml(new ScrapingBeeFetch(targetUrl, renderJs, premiumProxy, false, waitMs, null));
    }

    public String fetchHtml(ScrapingBeeFetch fetch) {
        StringBuilder requestUrl = new StringBuilder(BASE_URL)
                .append("?api_key=").append(apiKey)
                .append("&url=").append(URLEncoder.encode(fetch.url(), StandardCharsets.UTF_8))
                .append("&render_js=").append(fetch.renderJs())
                .append("&block_resources=false");

        if (fetch.stealthProxy()) {
            requestUrl.append("&stealth_proxy=true");
        } else {
            requestUrl.append("&premium_proxy=").append(fetch.premiumProxy());
        }
        if (fetch.waitMs() > 0) {
            requestUrl.append("&wait=").append(fetch.waitMs());
        }
        if (fetch.countryCode() != null && !fetch.countryCode().isBlank()) {
            requestUrl.append("&country_code=").append(URLEncoder.encode(fetch.countryCode(), StandardCharsets.UTF_8));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl.toString()))
                    .timeout(Duration.ofSeconds(90))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ScrapingBeeException(
                        "ScrapingBee вернул код " + response.statusCode()
                                + " для url=" + fetch.url()
                                + ": " + response.body()
                );
            }

            return response.body();
        } catch (ScrapingBeeException e) {
            throw e;
        } catch (Exception e) {
            throw new ScrapingBeeException("Ошибка запроса к ScrapingBee для url=" + fetch.url(), e);
        }
    }
}
