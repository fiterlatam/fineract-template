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
UPDATE stretchy_report SET report_sql = concat('SELECT
          CASE
              WHEN (${isTopup} = true) AND (${loanProductId} IN (2,8,9,',@productId,')) AND (${requestedAmount} BETWEEN 1500 AND 20000) THEN ''GREEN''
              WHEN (${isTopup} = true) AND (${loanProductId} IN (4,5)) AND (${requestedAmount} BETWEEN 5000 AND 25000) THEN ''GREEN''

              WHEN (${isTopup} = false) AND (${loanProductId} = 2) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 1500 AND 5000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 2) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 1500 AND 20000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 2) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 1500 AND 5000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 2) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 1500 AND 20000) THEN ''GREEN''

              WHEN (${isTopup} = false) AND (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 1500 AND 5000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 1500 AND 20000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 1500 AND 5000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = ',@productId,') AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 1500 AND 20000) THEN ''GREEN''

              WHEN (${isTopup} = false) AND (${loanProductId} = 8) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 1500 AND 5000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 8) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 1500 AND 20000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 8) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 1500 AND 5000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 8) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 1500 AND 20000) THEN ''GREEN''

              WHEN (${isTopup} = false) AND (${loanProductId} = 9) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 1500 AND 5000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 9) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 1500 AND 20000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 9) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 1500 AND 5000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 9) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 1500 AND 20000) THEN ''GREEN''

              WHEN (${isTopup} = false) AND (${loanProductId} = 4) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 5000 AND 10000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 4) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 5000 AND 25000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 4) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 5000 AND 10000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 4) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 5000 AND 25000) THEN ''GREEN''

              WHEN (${isTopup} = false) AND (${loanProductId} = 5) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 5000 AND 10000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 5) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''URBAN'') AND (${requestedAmount} BETWEEN 5000 AND 20000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 5) AND (''${categorization}'' = ''NUEVO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 5000 AND 10000) THEN ''GREEN''
              WHEN (${isTopup} = false) AND (${loanProductId} = 5) AND (''${categorization}'' = ''RECREDITO'') AND (''${clientArea}'' = ''RURAL'') AND (${requestedAmount} BETWEEN 5000 AND 20000) THEN ''GREEN''

              ELSE ''RED''
          END AS color')
WHERE report_name = 'Minimum and maximum amount Policy Check';
