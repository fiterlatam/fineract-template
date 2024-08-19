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
   prequalification_details.recreditCategorization,
   CASE
       WHEN (${loanProductId} = 2) AND (''${buroCheckClassification}'' = ''A'') THEN ''GREEN''
       WHEN (${loanProductId} = 2) AND (''${buroCheckClassification}'' = ''B'') THEN ''GREEN''
       WHEN (${loanProductId} = 2) AND (''${buroCheckClassification}'' = ''C'') THEN ''YELLOW''
       WHEN (${loanProductId} = 2) AND (''${buroCheckClassification}'' = ''D'') THEN ''ORANGE''

       WHEN (${loanProductId} = ',@productId,') AND (''${buroCheckClassification}'' = ''A'') THEN ''GREEN''
       WHEN (${loanProductId} = ',@productId,') AND (''${buroCheckClassification}'' = ''B'') THEN ''GREEN''
       WHEN (${loanProductId} = ',@productId,') AND (''${buroCheckClassification}'' = ''C'') THEN ''YELLOW''
       WHEN (${loanProductId} = ',@productId,') AND (''${buroCheckClassification}'' = ''D'') THEN ''ORANGE''

       WHEN (${loanProductId} = 9) AND (''${buroCheckClassification}'' = ''A'') THEN ''GREEN''
       WHEN (${loanProductId} = 9) AND (''${buroCheckClassification}'' = ''B'') THEN ''GREEN''
       WHEN (${loanProductId} = 9) AND (''${buroCheckClassification}'' = ''C'') THEN ''YELLOW''
       WHEN (${loanProductId} = 9) AND (''${buroCheckClassification}'' = ''D'') THEN ''ORANGE''

       WHEN (${loanProductId} = 8) AND (''${buroCheckClassification}'' = ''A'') THEN ''GREEN''
       WHEN (${loanProductId} = 8) AND (''${buroCheckClassification}'' = ''B'') THEN ''GREEN''
       WHEN (${loanProductId} = 8) AND (''${buroCheckClassification}'' = ''C'') THEN ''YELLOW''
       WHEN (${loanProductId} = 8) AND (''${buroCheckClassification}'' = ''D'') THEN ''ORANGE''

       WHEN (${loanProductId} = 4) AND (''${buroCheckClassification}'' = ''A'') THEN ''GREEN''
       WHEN (${loanProductId} = 4) AND (''${buroCheckClassification}'' = ''B'') THEN ''GREEN''
       WHEN (${loanProductId} = 4) AND (''${buroCheckClassification}'' = ''C'') THEN ''YELLOW''
       WHEN (${loanProductId} = 4) AND (''${buroCheckClassification}'' = ''D'') THEN ''ORANGE''

       WHEN (${loanProductId} = 5) AND (''${buroCheckClassification}'' = ''A'') THEN ''GREEN''
       WHEN (${loanProductId} = 5) AND (''${buroCheckClassification}'' = ''B'') THEN ''GREEN''
       WHEN (${loanProductId} = 5) AND (''${buroCheckClassification}'' = ''C'') THEN ''YELLOW''
       WHEN (${loanProductId} = 5) AND (''${buroCheckClassification}'' = ''D'') THEN ''ORANGE''
   END AS color
   FROM m_prequalification_group mpg
   INNER JOIN (
       SELECT p.id AS prequalification_id,
       (CASE WHEN p.previous_prequalification IS NOT NULL THEN ''RECREDITO'' ELSE ''NUEVO'' END) AS recreditCategorization
       FROM m_prequalification_group p
   ) prequalification_details ON prequalification_details.prequalification_id = mpg.id
   WHERE mpg.id = ${prequalificationId}')
WHERE report_name = 'President of the Board of Directors of the BC Policy Check';
