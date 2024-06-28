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
-- changeset fineract:1
-- MySQL dump 10.13  Distrib 5.1.60, for Win32 (ia32)
--
-- Host: localhost    Database: fineract_default
-- ------------------------------------------------------
-- Server version	5.1.60-community

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES UTF8MB4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report,self_service_user_report)
VALUES ("Reporte de Garantias vinculado a Prestamos", "Table", "Loan", "select sat.transaction_date as 'Fecha depósito', sa.client_id as 'Clienta', concat(c.firstname,' ', c.lastname) as 'Clienta Nombre',
ifnull(g.id, 'N/A') as 'Código de grupo', ifnull(g.display_name, 'N/A') as 'Grupo',a.name  as 'Agencia',
case
    when releaseTbl.id is not null then 'Released'
    WHEN releaseTbl.id is null then 'In Reserve'
end as Status, releaseTbl.transaction_date as 'Fecha de Liberación', sa.account_balance_derived as 'Saldo:',
ifnull(creditTbl.loanCount, 0) as 'Crédito vigente'
from m_savings_account_transaction sat
join m_savings_account sa on sa.id = sat.savings_account_id
left join m_client c on c.id = sa.client_id
left join m_group g on g.id = sa.group_id
left join m_group center ON center.id = g.parent_id
left join m_portfolio mp ON mp.id = center.portfolio_id
left join m_supervision ms ON ms.id = mp.supervision_id
left join m_agency a ON a.id = ms.agency_id
left join (select sat.id, sat.loan_id, sat.transaction_date from m_savings_account_transaction sat where sat.transaction_type_enum = 20 and sat.loan_id is not null
        and sat.release_id_of_hold_amount is not null) as releaseTbl on releaseTbl.loan_id = sat.loan_id
left join (select count(l.id) as loanCount, c.id as clientId, g.id as groupId from m_loan l
left join m_client c on c.id = l.client_id
left join m_group g on g.id = l.group_id
where l.loan_status_id = 300 group by c.id, g.id) as creditTbl on creditTbl.clientId = c.id or creditTbl.groupId = g.id
where sat.loan_id is not null and sat.transaction_type_enum = 1
and sat.transaction_date <= '${depositDate}'", "Reporte de Garantías vinculado a Préstamos", 1, 1, 0);


INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("depositDateSelect", "depositDate", "Deposit or Release Date", "date", "date", "n/a");



INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Reporte de Garantias vinculado a Prestamos"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "depositDateSelect"), "depositDateSelect");
