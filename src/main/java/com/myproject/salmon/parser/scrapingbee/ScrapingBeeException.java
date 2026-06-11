package com.myproject.salmon.parser.scrapingbee;

public class ScrapingBeeException extends RuntimeException {

    public ScrapingBeeException(String message) {
        super(message);
    }

    public ScrapingBeeException(String message, Throwable cause) {
        super(message, cause);
    }
}