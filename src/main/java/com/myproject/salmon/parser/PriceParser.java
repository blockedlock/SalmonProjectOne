package com.myproject.salmon.parser;

public interface PriceParser {
    boolean supports(String url);
    ParsedProduct parse(String url);
}
