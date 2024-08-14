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
UPDATE stretchy_report SET report_sql = concat(' SELECT CASE
                       WHEN (${loanProductId} IN (9)) AND ''${categorization}'' = ''RECREDITO'' AND ${requestedAmount} <= 6000  THEN ''GREEN''
                       WHEN (${loanProductId} IN (2,',@productId,',8)) AND ${requestedAmount} <= 6000 THEN ''GREEN''
                       WHEN (${loanProductId} IN (2,',@productId,',9,8,4,5,7)) AND (${photographs} <= 0  OR ${investmentPlan} <= 0) THEN ''RED''
                       ELSE ''GREEN''
                    END AS color')
WHERE report_name = 'Mandatory to attach photographs and investment plan Policy Check';
