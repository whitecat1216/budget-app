package com.example.budget.service;

public class RetryableAccountingSyncException extends RuntimeException {
    public RetryableAccountingSyncException(String message) { super(message); }
}
