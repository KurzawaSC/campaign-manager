package com.kurzawski.campaigns.exception;

public class ProductInUseException extends RuntimeException {

    public ProductInUseException(String name) {
        super("Product \"" + name + "\" has campaigns. Delete its campaigns first.");
    }
}
