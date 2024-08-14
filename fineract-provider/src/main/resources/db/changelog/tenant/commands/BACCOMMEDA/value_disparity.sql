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
UPDATE stretchy_report SET report_sql = concat(' SELECT
           CASE
               WHEN (${loanProductId} = 2) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} BETWEEN 1 AND 3) THEN ''GREEN''
               WHEN (${loanProductId} = 2) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} > 3) THEN ''YELLOW''
               WHEN (${loanProductId} = 2) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} BETWEEN 1 AND 4) THEN ''GREEN''
               WHEN (${loanProductId} = 2) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} > 4) THEN ''YELLOW''
               WHEN (${loanProductId} = 2) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} BETWEEN 1 AND 3) THEN ''GREEN''
               WHEN (${loanProductId} = 2) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} > 3) THEN ''YELLOW''
               WHEN (${loanProductId} = 2) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} BETWEEN 1 AND 4) THEN ''GREEN''
               WHEN (${loanProductId} = 2) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} > 4) THEN ''YELLOW''

               WHEN (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} BETWEEN 1 AND 3) THEN ''GREEN''
               WHEN (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} > 3) THEN ''YELLOW''
               WHEN (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} BETWEEN 1 AND 4) THEN ''GREEN''
               WHEN (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} > 4) THEN ''YELLOW''
               WHEN (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} BETWEEN 1 AND 3) THEN ''GREEN''
               WHEN (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} > 3) THEN ''YELLOW''
               WHEN (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} BETWEEN 1 AND 4) THEN ''GREEN''
               WHEN (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} > 4) THEN ''YELLOW''

               WHEN (${loanProductId} = 9) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} BETWEEN 1 AND 4) THEN ''GREEN''
               WHEN (${loanProductId} = 9) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} > 4) THEN ''YELLOW''
               WHEN (${loanProductId} = 9) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} BETWEEN 1 AND 4) THEN ''GREEN''
               WHEN (${loanProductId} = 9) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} > 4) THEN ''YELLOW''

               WHEN (${loanProductId} = 9) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} BETWEEN 1 AND 3) THEN ''GREEN''
               WHEN (${loanProductId} = 9) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} > 3) THEN ''YELLOW''
               WHEN (${loanProductId} = 9) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} BETWEEN 1 AND 3) THEN ''GREEN''
               WHEN (${loanProductId} = 9) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} > 3) THEN ''YELLOW''

               WHEN (${loanProductId} = 8) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} BETWEEN 1 AND 3) THEN ''GREEN''
               WHEN (${loanProductId} = 8) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} > 3) THEN ''YELLOW''
               WHEN (${loanProductId} = 8) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} BETWEEN 1 AND 4) THEN ''GREEN''
               WHEN (${loanProductId} = 8) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBANA'') AND (${disparityRatio} > 4) THEN ''YELLOW''
               WHEN (${loanProductId} = 8) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} BETWEEN 1 AND 3) THEN ''GREEN''
               WHEN (${loanProductId} = 8) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} > 3) THEN ''YELLOW''
               WHEN (${loanProductId} = 8) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} BETWEEN 1 AND 4) THEN ''GREEN''
               WHEN (${loanProductId} = 8) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${disparityRatio} > 4) THEN ''YELLOW''
                ELSE ''RED''

           END AS color
           FROM m_prequalification_group mpg
           LEFT JOIN (
              SELECT p.id AS prequalification_id,
              (select count(ml.id) FROM m_prequalification_group mp
             LEFT JOIN m_prequalification_group_members mpgm ON mpgm.group_id = mp.id
             LEFT JOIN m_client mc ON mc.dpi = mpgm.dpi
             LEFT JOIN m_loan ml ON ml.client_id = mc.id
             WHERE
             ml.loan_status_id < 300 AND ml.product_id = ${loanProductId} AND ml.is_topup = 1
             AND mp.id = ${prequalificationId}
             ) recredit_count
              FROM m_prequalification_group p
          ) recredit_loan ON recredit_loan.prequalification_id = mpg.id
           WHERE mpg.id = ${prequalificationId}')
WHERE report_name = 'Value disparity Policy Check';
