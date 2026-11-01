package com.myproject.salmon.parser.parsers.scrapingbee;

public record ScrapingBeeFetch(
        String url,
        boolean renderJs,
        boolean premiumProxy,
        boolean stealthProxy,
        int waitMs,
        String countryCode
) {
    public static ScrapingBeeFetch of(String url) {
        return new ScrapingBeeFetch(url, false, false, false, 0, null);
    }

    public static ScrapingBeeFetch jsPremium(String url) {
        return new ScrapingBeeFetch(url, true, true, false, 0, null);
    }

    public static ScrapingBeeFetch ozon(String url) {
        return new ScrapingBeeFetch(url, true, false, true, 3000, "ru");
    }
}
