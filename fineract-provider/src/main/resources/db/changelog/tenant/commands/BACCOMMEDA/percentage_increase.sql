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
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) <= 1500) AND (IFNULL(${percentageIncrease}, 0) <= 200)  THEN ''GREEN''
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) > 1500) AND (IFNULL(${currentCreditValue}, 0) <= 3000) AND (IFNULL(${percentageIncrease}, 0) <= 150)  THEN ''GREEN''
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) > 3000) AND (IFNULL(${currentCreditValue}, 0) <= 3500) AND (IFNULL(${percentageIncrease}, 0) <= 100)  THEN ''GREEN''
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) > 3500) AND (IFNULL(${currentCreditValue}, 0) <= 4000) AND (IFNULL(${percentageIncrease}, 0) <= 80)  THEN ''GREEN''
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) > 4000) AND (IFNULL(${percentageIncrease}, 0) <= 60)  THEN ''GREEN''
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) <= 1500) AND (IFNULL(${percentageIncrease}, 0) > 200)  THEN ''RED''
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) > 1500) AND (IFNULL(${currentCreditValue}, 0) <= 3000) AND (IFNULL(${percentageIncrease}, 0) > 150)  THEN ''RED''
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) > 3000) AND (IFNULL(${currentCreditValue}, 0) <= 3500) AND (IFNULL(${percentageIncrease}, 0) > 100)  THEN ''RED''
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) > 3500) AND (IFNULL(${currentCreditValue}, 0) <= 4000) AND (IFNULL(${percentageIncrease}, 0) > 80)  THEN ''RED''
		     WHEN (${loanProductId} = 2) AND (IFNULL(${currentCreditValue}, 0) > 4000) AND (IFNULL(${percentageIncrease}, 0) > 60)  THEN ''RED''

		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) <= 1500) AND (IFNULL(${percentageIncrease}, 0) <= 200)  THEN ''GREEN''
		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) > 1500) AND (IFNULL(${currentCreditValue}, 0) <= 3000) AND (IFNULL(${percentageIncrease}, 0) <= 150)  THEN ''GREEN''
		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) > 3000) AND (IFNULL(${currentCreditValue}, 0) <= 3500) AND (IFNULL(${percentageIncrease}, 0) <= 100)  THEN ''GREEN''
		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) > 3500) AND (IFNULL(${currentCreditValue}, 0) <= 4000) AND (IFNULL(${percentageIncrease}, 0) <= 80)  THEN ''GREEN''
		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) > 4000) AND (IFNULL(${percentageIncrease}, 0) <= 60)  THEN ''GREEN''
		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) <= 1500) AND (IFNULL(${percentageIncrease}, 0) > 200)  THEN ''RED''
		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) > 1500) AND (IFNULL(${currentCreditValue}, 0) <= 3000) AND (IFNULL(${percentageIncrease}, 0) > 150)  THEN ''RED''
		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) > 3000) AND (IFNULL(${currentCreditValue}, 0) <= 3500) AND (IFNULL(${percentageIncrease}, 0) > 100)  THEN ''RED''
		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) > 3500) AND (IFNULL(${currentCreditValue}, 0) <= 4000) AND (IFNULL(${percentageIncrease}, 0) > 80)  THEN ''RED''
		     WHEN (${loanProductId} = ',@productId,') AND (IFNULL(${currentCreditValue}, 0) > 4000) AND (IFNULL(${percentageIncrease}, 0) > 60)  THEN ''RED''

		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) <= 1500) AND (IFNULL(${percentageIncrease}, 0) <= 200)  THEN ''GREEN''
		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) > 1500) AND (IFNULL(${currentCreditValue}, 0) <= 3000) AND (IFNULL(${percentageIncrease}, 0) <= 150)  THEN ''GREEN''
		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) > 3000) AND (IFNULL(${currentCreditValue}, 0) <= 3500) AND (IFNULL(${percentageIncrease}, 0) <= 100)  THEN ''GREEN''
		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) > 3500) AND (IFNULL(${currentCreditValue}, 0) <= 4000) AND (IFNULL(${percentageIncrease}, 0) <= 80)  THEN ''GREEN''
		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) > 4000) AND (IFNULL(${percentageIncrease}, 0) <= 60)  THEN ''GREEN''
		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) <= 1500) AND (IFNULL(${percentageIncrease}, 0) > 200)  THEN ''RED''
		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) > 1500) AND (IFNULL(${currentCreditValue}, 0) <= 3000) AND (IFNULL(${percentageIncrease}, 0) > 150)  THEN ''RED''
		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) > 3000) AND (IFNULL(${currentCreditValue}, 0) <= 3500) AND (IFNULL(${percentageIncrease}, 0) > 100)  THEN ''RED''
		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) > 3500) AND (IFNULL(${currentCreditValue}, 0) <= 4000) AND (IFNULL(${percentageIncrease}, 0) > 80)  THEN ''RED''
		     WHEN (${loanProductId} = 9) AND (IFNULL(${currentCreditValue}, 0) > 4000) AND (IFNULL(${percentageIncrease}, 0) > 60)  THEN ''RED''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) <= 1500) AND (IFNULL(${percentageIncrease}, 0) <= 200)  THEN ''GREEN''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) > 1500) AND (IFNULL(${currentCreditValue}, 0) <= 3000) AND (IFNULL(${percentageIncrease}, 0) <= 150)  THEN ''GREEN''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) > 3000) AND (IFNULL(${currentCreditValue}, 0) <= 3500) AND (IFNULL(${percentageIncrease}, 0) <= 100)  THEN ''GREEN''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) > 3500) AND (IFNULL(${currentCreditValue}, 0) <= 4000) AND (IFNULL(${percentageIncrease}, 0) <= 80)  THEN ''GREEN''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) > 4000) AND (IFNULL(${percentageIncrease}, 0) <= 60)  THEN ''GREEN''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) <= 1500) AND (IFNULL(${percentageIncrease}, 0) > 200)  THEN ''RED''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) > 1500) AND (IFNULL(${currentCreditValue}, 0) <= 3000) AND (IFNULL(${percentageIncrease}, 0) > 150)  THEN ''RED''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) > 3000) AND (IFNULL(${currentCreditValue}, 0) <= 3500) AND (IFNULL(${percentageIncrease}, 0) > 100)  THEN ''RED''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) > 3500) AND (IFNULL(${currentCreditValue}, 0) <= 4000) AND (IFNULL(${percentageIncrease}, 0) > 80)  THEN ''RED''
		     WHEN (${loanProductId} = 8) AND (IFNULL(${currentCreditValue}, 0) > 4000) AND (IFNULL(${percentageIncrease}, 0) > 60)  THEN ''RED''
		     WHEN (${loanProductId} = 4) AND (IFNULL(${percentageIncrease}, 0) <= 60) THEN ''GREEN''
		     WHEN (${loanProductId} = 4) AND (IFNULL(${percentageIncrease}, 0) > 60) THEN ''ORANGE''
		     WHEN (${loanProductId} = 5) AND (IFNULL(${percentageIncrease}, 0) <= 60) THEN ''GREEN''
		     WHEN (${loanProductId} = 5) AND (IFNULL(${percentageIncrease}, 0) > 60) THEN ''ORANGE''
		 END AS color
		 FROM m_client mc
		 WHERE mc.id = ${clientId}
		 GROUP BY mc.id')
WHERE report_name = 'Increase percentage Policy Check';
