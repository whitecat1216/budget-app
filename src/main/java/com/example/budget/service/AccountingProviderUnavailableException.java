package com.example.budget.service;

public class AccountingProviderUnavailableException extends RuntimeException {
    public AccountingProviderUnavailableException(String provider) {
        super("会計サービス「" + provider + "」のコネクタはまだ登録されていません。");
    }
}
