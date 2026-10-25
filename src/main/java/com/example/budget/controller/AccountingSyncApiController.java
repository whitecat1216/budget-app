package com.example.budget.controller;

import com.example.budget.model.AccountingSyncRunResponse;
import com.example.budget.service.AccountingProviderUnavailableException;
import com.example.budget.service.AccountingSyncAlreadyRunningException;
import com.example.budget.service.AccountingSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountingSyncApiController {
    private final AccountingSyncService syncService;

    @PostMapping("/connections/{connectionId}/sync")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public ResponseEntity<?> requestSync(@PathVariable long connectionId) {
        try {
            var run = syncService.requestSync(connectionId);
            return ResponseEntity.accepted().body(AccountingSyncRunResponse.from(run));
        } catch (AccountingProviderUnavailableException | AccountingSyncAlreadyRunningException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会計接続が見つかりません。");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/sync-runs")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN','AUDITOR')")
    public List<AccountingSyncRunResponse> recentRuns() {
        return syncService.recentRuns().stream().map(AccountingSyncRunResponse::from).toList();
    }

    @PostMapping("/sync-runs/{runId}/retry")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','FINANCE_ADMIN')")
    public ResponseEntity<?> retry(@PathVariable long runId) {
        try {
            var run = syncService.retry(runId);
            return ResponseEntity.accepted().body(AccountingSyncRunResponse.from(run));
        } catch (AccountingSyncAlreadyRunningException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (AccountingProviderUnavailableException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "同期履歴が見つかりません。");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }
}
