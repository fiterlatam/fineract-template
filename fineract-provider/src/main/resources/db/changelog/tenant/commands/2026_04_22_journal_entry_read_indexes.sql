--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

-- liquibase formatted sql
-- changeset fineract:20260422-journal-entry-read-indexes

-- 1) Note:
-- entry_date_index visibility toggle is not included here for cross-version compatibility
-- (ALTER INDEX ... VISIBLE is MySQL 8 specific).

-- 2) Fast path for transaction-id lookups.
SET @idx_aje_transaction_id := (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'CREATE INDEX idx_aje_transaction_id ON acc_gl_journal_entry (transaction_id)'
        ELSE 'SELECT 1'
    END
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'acc_gl_journal_entry'
      AND index_name = 'idx_aje_transaction_id'
);
PREPARE stmt_idx_aje_transaction_id FROM @idx_aje_transaction_id;
EXECUTE stmt_idx_aje_transaction_id;
DEALLOCATE PREPARE stmt_idx_aje_transaction_id;

-- 3) Fast path for office filtered + default ordered reads.
SET @idx_aje_office_entrydate_id := (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'CREATE INDEX idx_aje_office_entrydate_id ON acc_gl_journal_entry (office_id, entry_date, id)'
        ELSE 'SELECT 1'
    END
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'acc_gl_journal_entry'
      AND index_name = 'idx_aje_office_entrydate_id'
);
PREPARE stmt_idx_aje_office_entrydate_id FROM @idx_aje_office_entrydate_id;
EXECUTE stmt_idx_aje_office_entrydate_id;
DEALLOCATE PREPARE stmt_idx_aje_office_entrydate_id;

-- 4) Fast path for account filtered + default ordered reads.
SET @idx_aje_account_entrydate_id := (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'CREATE INDEX idx_aje_account_entrydate_id ON acc_gl_journal_entry (account_id, entry_date, id)'
        ELSE 'SELECT 1'
    END
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'acc_gl_journal_entry'
      AND index_name = 'idx_aje_account_entrydate_id'
);
PREPARE stmt_idx_aje_account_entrydate_id FROM @idx_aje_account_entrydate_id;
EXECUTE stmt_idx_aje_account_entrydate_id;
DEALLOCATE PREPARE stmt_idx_aje_account_entrydate_id;

-- 5) Fast path for entity type filtered + default ordered reads.
SET @idx_aje_entitytype_entrydate_id := (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'CREATE INDEX idx_aje_entitytype_entrydate_id ON acc_gl_journal_entry (entity_type_enum, entry_date, id)'
        ELSE 'SELECT 1'
    END
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'acc_gl_journal_entry'
      AND index_name = 'idx_aje_entitytype_entrydate_id'
);
PREPARE stmt_idx_aje_entitytype_entrydate_id FROM @idx_aje_entitytype_entrydate_id;
EXECUTE stmt_idx_aje_entitytype_entrydate_id;
DEALLOCATE PREPARE stmt_idx_aje_entitytype_entrydate_id;

-- 6) Support IN-subquery lookups used by loan and savings filters.
SET @idx_mlt_loanid_id := (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'CREATE INDEX idx_mlt_loanid_id ON m_loan_transaction (loan_id, id)'
        ELSE 'SELECT 1'
    END
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'm_loan_transaction'
      AND index_name = 'idx_mlt_loanid_id'
);
PREPARE stmt_idx_mlt_loanid_id FROM @idx_mlt_loanid_id;
EXECUTE stmt_idx_mlt_loanid_id;
DEALLOCATE PREPARE stmt_idx_mlt_loanid_id;

SET @idx_msat_savingsid_id := (
    SELECT CASE
        WHEN COUNT(*) = 0 THEN 'CREATE INDEX idx_msat_savingsid_id ON m_savings_account_transaction (savings_account_id, id)'
        ELSE 'SELECT 1'
    END
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'm_savings_account_transaction'
      AND index_name = 'idx_msat_savingsid_id'
);
PREPARE stmt_idx_msat_savingsid_id FROM @idx_msat_savingsid_id;
EXECUTE stmt_idx_msat_savingsid_id;
DEALLOCATE PREPARE stmt_idx_msat_savingsid_id;
