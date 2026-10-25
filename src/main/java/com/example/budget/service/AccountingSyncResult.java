package com.example.budget.service;

public record AccountingSyncResult(boolean partialSuccess) {
    public static AccountingSyncResult success() { return new AccountingSyncResult(false); }
    public static AccountingSyncResult partial() { return new AccountingSyncResult(true); }
}
