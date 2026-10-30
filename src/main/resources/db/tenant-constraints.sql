DO $$
DECLARE tenant_table TEXT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_request_approvals_tenant_request') THEN
        ALTER TABLE spending_request_approvals ADD CONSTRAINT fk_request_approvals_tenant_request
            FOREIGN KEY (tenant_id, request_id) REFERENCES spending_requests (tenant_id, id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_request_approvals_tenant_reviewer') THEN
        ALTER TABLE spending_request_approvals ADD CONSTRAINT fk_request_approvals_tenant_reviewer
            FOREIGN KEY (tenant_id, reviewer_id) REFERENCES users (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_notifications_tenant_recipient') THEN
        ALTER TABLE notifications ADD CONSTRAINT fk_notifications_tenant_recipient
            FOREIGN KEY (tenant_id, recipient_id) REFERENCES users (tenant_id, id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_account_codes_tenant_category') THEN
        ALTER TABLE account_codes ADD CONSTRAINT fk_account_codes_tenant_category
            FOREIGN KEY (tenant_id, category_id) REFERENCES categories (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_projects_tenant_department') THEN
        ALTER TABLE projects ADD CONSTRAINT fk_projects_tenant_department
            FOREIGN KEY (tenant_id, department_id) REFERENCES departments (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_scopes_tenant_user') THEN
        ALTER TABLE user_scopes ADD CONSTRAINT fk_scopes_tenant_user
            FOREIGN KEY (tenant_id, user_id) REFERENCES users (tenant_id, id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_spending_tenant_requester') THEN
        ALTER TABLE spending_requests ADD CONSTRAINT fk_spending_tenant_requester
            FOREIGN KEY (tenant_id, requester_id) REFERENCES users (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_spending_tenant_department') THEN
        ALTER TABLE spending_requests ADD CONSTRAINT fk_spending_tenant_department
            FOREIGN KEY (tenant_id, department_id) REFERENCES departments (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_spending_tenant_project') THEN
        ALTER TABLE spending_requests ADD CONSTRAINT fk_spending_tenant_project
            FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_spending_tenant_category') THEN
        ALTER TABLE spending_requests ADD CONSTRAINT fk_spending_tenant_category
            FOREIGN KEY (tenant_id, category_id) REFERENCES categories (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_approval_tenant_request') THEN
        ALTER TABLE approval_history ADD CONSTRAINT fk_approval_tenant_request
            FOREIGN KEY (tenant_id, request_id) REFERENCES spending_requests (tenant_id, id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_approval_tenant_actor') THEN
        ALTER TABLE approval_history ADD CONSTRAINT fk_approval_tenant_actor
            FOREIGN KEY (tenant_id, actor_id) REFERENCES users (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_audit_tenant_actor') THEN
        ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_tenant_actor
            FOREIGN KEY (tenant_id, actor_id) REFERENCES users (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_budget_changes_tenant_budget') THEN
        ALTER TABLE budget_change_requests ADD CONSTRAINT fk_budget_changes_tenant_budget
            FOREIGN KEY (tenant_id, budget_id) REFERENCES budgets (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_budget_changes_tenant_requester') THEN
        ALTER TABLE budget_change_requests ADD CONSTRAINT fk_budget_changes_tenant_requester
            FOREIGN KEY (tenant_id, requester_id) REFERENCES users (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_budget_changes_tenant_reviewer') THEN
        ALTER TABLE budget_change_requests ADD CONSTRAINT fk_budget_changes_tenant_reviewer
            FOREIGN KEY (tenant_id, reviewer_id) REFERENCES users (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sync_runs_tenant_connection') THEN
        ALTER TABLE accounting_sync_runs ADD CONSTRAINT fk_sync_runs_tenant_connection
            FOREIGN KEY (tenant_id, connection_id) REFERENCES accounting_connections (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sync_runs_tenant_requester') THEN
        ALTER TABLE accounting_sync_runs ADD CONSTRAINT fk_sync_runs_tenant_requester
            FOREIGN KEY (tenant_id, requested_by) REFERENCES users (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_external_tenant_connection') THEN
        ALTER TABLE external_actuals ADD CONSTRAINT fk_external_tenant_connection
            FOREIGN KEY (tenant_id, connection_id) REFERENCES accounting_connections (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_external_tenant_category') THEN
        ALTER TABLE external_actuals ADD CONSTRAINT fk_external_tenant_category
            FOREIGN KEY (tenant_id, category_id) REFERENCES categories (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_external_tenant_department') THEN
        ALTER TABLE external_actuals ADD CONSTRAINT fk_external_tenant_department
            FOREIGN KEY (tenant_id, department_id) REFERENCES departments (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_external_tenant_project') THEN
        ALTER TABLE external_actuals ADD CONSTRAINT fk_external_tenant_project
            FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transactions_tenant_category') THEN
        ALTER TABLE transactions ADD CONSTRAINT fk_transactions_tenant_category
            FOREIGN KEY (tenant_id, category_id) REFERENCES categories (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transactions_tenant_department') THEN
        ALTER TABLE transactions ADD CONSTRAINT fk_transactions_tenant_department
            FOREIGN KEY (tenant_id, department_id) REFERENCES departments (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transactions_tenant_project') THEN
        ALTER TABLE transactions ADD CONSTRAINT fk_transactions_tenant_project
            FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_transactions_tenant_creator') THEN
        ALTER TABLE transactions ADD CONSTRAINT fk_transactions_tenant_creator
            FOREIGN KEY (tenant_id, created_by) REFERENCES users (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_budgets_tenant_category') THEN
        ALTER TABLE budgets ADD CONSTRAINT fk_budgets_tenant_category
            FOREIGN KEY (tenant_id, category_id) REFERENCES categories (tenant_id, id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_budgets_tenant_department') THEN
        ALTER TABLE budgets ADD CONSTRAINT fk_budgets_tenant_department
            FOREIGN KEY (tenant_id, department_id) REFERENCES departments (tenant_id, id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_budgets_tenant_project') THEN
        ALTER TABLE budgets ADD CONSTRAINT fk_budgets_tenant_project
            FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id);
    END IF;

    FOREACH tenant_table IN ARRAY ARRAY[
        'tenants','departments','projects','users','user_scopes','spending_requests','approval_policies',
        'spending_request_approvals','approval_history','audit_logs','notifications','budget_change_requests',
        'accounting_connections','accounting_sync_runs','external_actuals','categories','account_codes','transactions','budgets'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tenant_table);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', tenant_table);
        IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE schemaname = current_schema()
                       AND tablename = tenant_table AND policyname = 'tenant_isolation') THEN
            EXECUTE format('CREATE POLICY tenant_isolation ON %I USING ' ||
                '(current_setting(''app.rls_bypass'', true) = ''on'' OR tenant_id = NULLIF(current_setting(''app.tenant_id'', true), '''')::BIGINT) ' ||
                'WITH CHECK (current_setting(''app.rls_bypass'', true) = ''on'' OR tenant_id = NULLIF(current_setting(''app.tenant_id'', true), '''')::BIGINT)',
                tenant_table);
        END IF;
    END LOOP;
END $$;
