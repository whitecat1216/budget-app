package com.example.budget.service;

/** Provider adapter SPI. Implementations are added after an accounting service is selected. */
public interface AccountingSyncProcessor {
    String provider();

    AccountingSyncResult synchronize(AccountingSyncContext context, AccountingSyncProgress progress);
}
