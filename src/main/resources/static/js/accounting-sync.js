(() => {
    const page = document.querySelector("#accountingSyncPage");
    if (!page) return;

    const feedback = document.querySelector("#syncFeedback");
    const body = document.querySelector("#syncRunsBody");
    const showFeedback = (message, kind) => {
        feedback.textContent = message;
        feedback.className = `alert alert-${kind}`;
    };

    const addCell = (row, text, className = "") => {
        const cell = document.createElement("td");
        cell.textContent = text ?? "";
        if (className) cell.className = className;
        row.append(cell);
    };

    const refreshRuns = async () => {
        const response = await fetch("/api/accounting/sync-runs", {
            headers: { Accept: "application/json" },
            credentials: "same-origin"
        });
        if (!response.ok) throw new Error("同期履歴を取得できませんでした。");
        const runs = await response.json();
        body.replaceChildren();

        if (runs.length === 0) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 6;
            cell.className = "text-center text-muted py-4";
            cell.textContent = "同期履歴はありません。";
            row.append(cell);
            body.append(row);
            return;
        }

        runs.forEach((run) => {
            const row = document.createElement("tr");
            row.dataset.runId = run.id;
            addCell(row, run.queuedAt ? new Date(run.queuedAt).toLocaleString("ja-JP") : "");
            addCell(row, run.provider);
            addCell(row, run.status);
            addCell(row, String(run.fetchedCount), "text-end");
            addCell(row, String(run.importedCount), "text-end");
            addCell(row, run.errorMessage, "small text-muted");
            body.append(row);
        });
    };

    document.querySelectorAll("[data-sync-form], [data-retry-form]").forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const button = form.querySelector("button[type='submit']");
            button.disabled = true;
            try {
                const response = await fetch(form.action, {
                    method: "POST",
                    body: new URLSearchParams(new FormData(form)),
                    headers: { Accept: "application/json" },
                    credentials: "same-origin"
                });
                const result = await response.json();
                if (!response.ok) throw new Error(result.message || "同期を受付できませんでした。");
                const action = form.matches("[data-retry-form]") ? "再試行" : "同期";
                showFeedback(`${action}を受け付けました（実行ID: ${result.id}）。`, "success");
                await refreshRuns();
            } catch (error) {
                showFeedback(error.message || "同期を受付できませんでした。", "warning");
            } finally {
                button.disabled = false;
            }
        });
    });

    document.querySelector("#refreshSyncRuns")?.addEventListener("click", () => {
        refreshRuns().catch((error) => showFeedback(error.message, "warning"));
    });

    window.setInterval(() => {
        if (Array.from(body.querySelectorAll("tr")).some((row) =>
            ["QUEUED", "RUNNING", "RETRY_WAIT"].includes(row.children[2]?.textContent?.trim()))) {
            refreshRuns().catch(() => { /* Keep the current history visible during temporary polling errors. */ });
        }
    }, 5000);
})();
