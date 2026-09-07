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
-- changeset fineract:20260907-insert-committee-approval-limits

-- Committee A (prod committee_id 404)
INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 250000.00, 250000000.00, 'GREATER_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'A'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 250000.00
        AND existing.to_amount = 250000000.00
        AND existing.`condition` = 'GREATER_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 250000.00, 250000000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'A'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 250000.00
        AND existing.to_amount = 250000000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );

-- Committee B (prod committee_id 405)
INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 250001.00, 250000000.00, 'GREATER_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'B'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 250001.00
        AND existing.to_amount = 250000000.00
        AND existing.`condition` = 'GREATER_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 80.00, 250000.00, 'GREATER_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'B'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 80.00
        AND existing.to_amount = 250000.00
        AND existing.`condition` = 'GREATER_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 250001.00, 250000000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'B'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 250001.00
        AND existing.to_amount = 250000000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 80000.00, 250000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'B'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 80000.00
        AND existing.to_amount = 250000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );

-- Committee C (prod committee_id 406)
INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 80001.00, 250000.00, 'GREATER_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'C'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 80001.00
        AND existing.to_amount = 250000.00
        AND existing.`condition` = 'GREATER_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 250001.00, 250000000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'C'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 250001.00
        AND existing.to_amount = 250000000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 0.00, 80000.00, 'GREATER_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'C'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 0.00
        AND existing.to_amount = 80000.00
        AND existing.`condition` = 'GREATER_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 250001.00, 250000000.00, 'GREATER_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'C'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 250001.00
        AND existing.to_amount = 250000000.00
        AND existing.`condition` = 'GREATER_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 80001.00, 250000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'C'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 80001.00
        AND existing.to_amount = 250000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 20000.00, 80000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'C'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 20000.00
        AND existing.to_amount = 80000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );

-- Committee D (prod committee_id 407)
INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 80001.00, 250000.00, 'GREATER_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'D'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 80001.00
        AND existing.to_amount = 250000.00
        AND existing.`condition` = 'GREATER_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 80001.00, 250000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'D'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 80001.00
        AND existing.to_amount = 250000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 0.00, 80000.00, 'GREATER_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'D'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 0.00
        AND existing.to_amount = 80000.00
        AND existing.`condition` = 'GREATER_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 20001.00, 80000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'D'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 20001.00
        AND existing.to_amount = 80000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 250001.00, 25000000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'D'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 250001.00
        AND existing.to_amount = 25000000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 250001.00, 25000000.00, 'GREATER_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'D'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 250001.00
        AND existing.to_amount = 25000000.00
        AND existing.`condition` = 'GREATER_THAN'
        AND existing.`limit` = 1
  );

INSERT INTO committee_approval_limits (committee_id, from_amount, to_amount, `condition`, `limit`, created_at, created_by)
SELECT cv.id, 0.00, 20000.00, 'LESS_THAN', 1, NOW(), 1
FROM m_code_value cv
INNER JOIN m_code c ON c.id = cv.code_id AND c.code_name = 'Committees'
WHERE cv.code_value = 'D'
  AND NOT EXISTS (
      SELECT 1 FROM committee_approval_limits existing
      WHERE existing.committee_id = cv.id
        AND existing.from_amount = 0.00
        AND existing.to_amount = 20000.00
        AND existing.`condition` = 'LESS_THAN'
        AND existing.`limit` = 1
  );
