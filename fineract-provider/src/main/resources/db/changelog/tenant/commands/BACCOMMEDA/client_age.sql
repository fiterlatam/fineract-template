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

SELECT @productId := (select id from m_product_loan where short_name='BCOMMEDA' limit 1);
UPDATE stretchy_report SET report_sql = concat('SELECT mc.id,
            client_age.client_age AS client_age,
            mc.date_of_birth AS date_of_birth,
            CASE
            -- Banco Comunal normal
            WHEN (${loanProductId} = 2) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN ''GREEN''
            WHEN (${loanProductId} = 2) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN ''YELLOW''
            WHEN (${loanProductId} = 2) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''
            WHEN (${loanProductId} = 2) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN ''GREEN''
            WHEN (${loanProductId} = 2) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''

            -- Banco Comunal normal meda
            WHEN (${loanProductId} = ',@productId,') AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN ''GREEN''
            WHEN (${loanProductId} = ',@productId,') AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN ''YELLOW''
            WHEN (${loanProductId} = ',@productId,') AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''
            WHEN (${loanProductId} = ',@productId,') AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN ''GREEN''
            WHEN (${loanProductId} = ',@productId,') AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''

            -- Banco Comunal temporal
            WHEN (${loanProductId} = 9) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN ''GREEN''
            WHEN (${loanProductId} = 9) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN ''YELLOW''
            WHEN (${loanProductId} = 9) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''
            WHEN (${loanProductId} = 9) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN ''GREEN''
            WHEN (${loanProductId} = 9) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''


            -- Banco Comunal Agricola
            WHEN (${loanProductId} = 8) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN ''GREEN''
            WHEN (${loanProductId} = 8) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN ''YELLOW''
            WHEN (${loanProductId} = 8) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''
            WHEN (${loanProductId} = 8) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN ''GREEN''
            WHEN (${loanProductId} = 8) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''


            -- Grupo Solidario
            WHEN (${loanProductId} = 4) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN ''GREEN''
            WHEN (${loanProductId} = 4) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN ''YELLOW''
            WHEN (${loanProductId} = 4) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''
            WHEN (${loanProductId} = 4) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN ''GREEN''
            WHEN (${loanProductId} = 4) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''


            -- Grupo Solidario Agricola
            WHEN (${loanProductId} = 5) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN ''GREEN''
            WHEN (${loanProductId} = 5) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN ''YELLOW''
            WHEN (${loanProductId} = 5) AND (''${clientCategorization}'' = ''NEW'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''
            WHEN (${loanProductId} = 5) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN ''GREEN''
            WHEN (${loanProductId} = 5) AND (''${clientCategorization}'' = ''RECURRING'') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN ''RED''
            END AS color
            FROM m_client mc
            INNER JOIN (
            SELECT mct.id AS client_id, mct.date_of_birth AS date_of_birth, IFNULL(TIMESTAMPDIFF(YEAR, mct.date_of_birth, CURDATE()), 0) AS client_age
            FROM m_client mct
            ) client_age ON client_age.client_id = mc.id
            WHERE mc.id = ${clientId}')
WHERE report_name = 'Client age Policy Check';
