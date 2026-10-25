package com.example.budget.service;

public class AccountingSyncAlreadyRunningException extends RuntimeException {
    public AccountingSyncAlreadyRunningException() {
        super("この会計接続の同期はすでに実行中または待機中です。");
    }
}
