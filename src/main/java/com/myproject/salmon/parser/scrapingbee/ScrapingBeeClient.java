package com.myproject.salmon.parser.scrapingbee;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class ScrapingBeeClient {

    private static final String BASE_URL = "https://app.scrapingbee.com/api/v1/";

    @Value("${scrapingbee.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String fetchHtml(String targetUrl) {
        return fetchHtml(targetUrl, false, false);
    }

    public String fetchHtml(String targetUrl, boolean renderJs) {
        return fetchHtml(targetUrl, renderJs, false);
    }

    public String fetchHtml(String targetUrl, boolean renderJs, boolean premiumProxy) {
        StringBuilder requestUrl = new StringBuilder(BASE_URL)
                .append("?api_key=").append(apiKey)
                .append("&url=").append(URLEncoder.encode(targetUrl, StandardCharsets.UTF_8))
                .append("&render_js=").append(renderJs)
                .append("&premium_proxy=").append(premiumProxy)
                .append("&block_resources=false");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl.toString()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ScrapingBeeException(
                        "ScrapingBee вернул код " + response.statusCode()
                                + " для url=" + targetUrl
                                + ": " + response.body()
                );
            }

            return response.body();
        } catch (ScrapingBeeException e) {
            throw e;
        } catch (Exception e) {
            throw new ScrapingBeeException("Ошибка запроса к ScrapingBee для url=" + targetUrl, e);
        }
    }
}