package com.kurzawski.campaigns.exception;

public class DuplicateProductNameException extends RuntimeException {

    public DuplicateProductNameException(String name) {
        super("A product named \"" + name.strip() + "\" already exists");
    }
}
