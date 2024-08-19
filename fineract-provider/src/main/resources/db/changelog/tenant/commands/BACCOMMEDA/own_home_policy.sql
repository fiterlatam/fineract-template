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
                housing_type.owner_percent,
                CASE
                    WHEN (${loanProductId} = 2) AND (housing_type.owner_percent >=50) THEN ''GREEN''
                    WHEN (${loanProductId} = 2) AND (housing_type.owner_percent <50) THEN ''YELLOW''

                    WHEN (${loanProductId} = ',@productId,') AND (housing_type.owner_percent >=50) THEN ''GREEN''
                    WHEN (${loanProductId} = ',@productId,') AND (housing_type.owner_percent <50) THEN ''YELLOW''

                    WHEN (${loanProductId} = 8) AND (housing_type.owner_percent >=50) THEN ''GREEN''
                    WHEN (${loanProductId} = 8) AND (housing_type.owner_percent <50) THEN ''YELLOW''

                END AS color
                FROM m_prequalification_group mpg
                INNER JOIN ( select (((select count(*) from m_prequalification_group_members mpm INNER JOIN m_client mc on mc.dpi = mpm.dpi
                INNER JOIN m_client_contact_info mcinf on mcinf.client_id = mc.id INNER JOIN m_code_value mcv on mcv.id = mcinf.housing_type where mcv.code_value = ''Propia'' and mpm.group_id = ${prequalificationId})/
                (select count(*) from m_prequalification_group_members mpg where mpg.group_id = ${prequalificationId}))*100) as owner_percent, ${prequalificationId} as grp_id )
                housing_type ON housing_type.grp_id = mpg.id
                WHERE mpg.id = ${prequalificationId}')
WHERE report_name = 'Percentage of members with their own home Policy Check';
