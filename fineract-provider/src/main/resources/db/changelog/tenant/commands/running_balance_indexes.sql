-- Database Indexing Optimizations for ACCOUNTING_RUNNING_BALANCE_UPDATE Job
-- These indexes will significantly improve the performance of the running balance update queries

-- 1. Composite index for finding unprocessed journal entries
-- This index supports the main query that finds the earliest unprocessed date
CREATE INDEX IF NOT EXISTS idx_gl_journal_entry_unprocessed
ON acc_gl_journal_entry (is_running_balance_calculated, entry_date)
WHERE is_running_balance_calculated = false;

-- 2. Composite index for organization running balance queries
-- This index supports the window function queries for organization-level balances
CREATE INDEX IF NOT EXISTS idx_gl_journal_entry_org_balance
ON acc_gl_journal_entry (entry_date DESC, id DESC, account_id, organization_running_balance);

-- 3. Composite index for office running balance queries
-- This index supports the window function queries for office-level balances
CREATE INDEX IF NOT EXISTS idx_gl_journal_entry_office_balance
ON acc_gl_journal_entry (office_id, entry_date DESC, id DESC, account_id, office_running_balance);

-- 4. Index for journal entry processing queries
-- This index supports the main processing queries that fetch entries to update
CREATE INDEX IF NOT EXISTS idx_gl_journal_entry_processing
ON acc_gl_journal_entry (entry_date, id, account_id, office_id);

-- 5. Index for GL account classification lookups
-- This index supports the JOIN with acc_gl_account table
CREATE INDEX IF NOT EXISTS idx_gl_account_classification
ON acc_gl_account (id, classification_enum);

-- 6. Partial index for non-null running balances
-- This index helps with queries that filter out null running balances
CREATE INDEX IF NOT EXISTS idx_gl_journal_entry_org_balance_not_null
ON acc_gl_journal_entry (account_id, entry_date DESC, id DESC)
WHERE organization_running_balance IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_gl_journal_entry_office_balance_not_null
ON acc_gl_journal_entry (office_id, account_id, entry_date DESC, id DESC)
WHERE office_running_balance IS NOT NULL;

-- 7. Index for batch update operations
-- This index supports the WHERE clause in UPDATE statements
CREATE INDEX IF NOT EXISTS idx_gl_journal_entry_id
ON acc_gl_journal_entry (id);

-- Performance monitoring queries (optional - for monitoring index usage)
-- Uncomment these to monitor index effectiveness:

-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE tablename = 'acc_gl_journal_entry'
-- ORDER BY idx_scan DESC;

-- SELECT schemaname, tablename, seq_scan, seq_tup_read, idx_scan, idx_tup_read
-- FROM pg_stat_user_tables
-- WHERE tablename = 'acc_gl_journal_entry';
