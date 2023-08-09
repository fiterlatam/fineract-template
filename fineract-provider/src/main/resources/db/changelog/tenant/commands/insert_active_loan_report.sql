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
VALUES ("Active Loans", "Table", "Loan", "SELECT '' AS 'Center No',g.account_no AS 'Group No',l.account_no AS 'No. Loan Agreement', pl.name AS 'Product', CONCAT(c.firstname,' ',c.lastname) AS 'Customer Name', l.submittedon_date as 'First Date',
l.approved_principal AS 'Approved Amount', l.net_disbursal_amount AS 'Amount Disbursement', 0 AS 'Guarantee Amount', 0 AS 'Bills Administrative', pl.description AS 'Description'
FROM m_office o
JOIN m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%')
AND ounder.hierarchy like concat('${currentUserHierarchy}', '%')
JOIN m_client c on c.office_id = ounder.id
JOIN  m_loan l on l.client_id = c.id
JOIN m_group g ON g.id = l.group_id
JOIN m_product_loan pl ON pl.id = l.product_id
WHERE
o.id = ${officeId}
AND (l.product_id = '${productId}' or '-1' = '${productId}')
AND (l.client_id = '${clientId}' or '-1' = '${clientId}')
AND (l.group_id = '${groupId}' or '-1' = '${groupId}')
AND (l.disbursedon_date <= '${disbursedOnStartDate}' AND l.disbursedon_date >= '${disbursedOnEndDate}')
AND l.loan_status_id = 100", "Get Active Loans", 1, 1, 0);



INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, selectAll, parameter_sql)
VALUES("productIdSelectAll", "productId", "Product", "select", "number", 0, "Y", "SELECT pl.id, pl.name as `Name` FROM m_product_loan pl");
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, selectAll, parameter_sql,parent_id)
VALUES("groupIdSelectAll", "groupId", "Group", "select", "number", 0, "Y", "(SELECT g.id,g.display_name as `Name`
from m_office o
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%')
join m_group g on g.office_id = ounder.id
and o.id = ${officeId})
union all
(select -10, '-')
order by 2",(SELECT p.id FROM stretchy_parameter p WHERE p.parameter_name = "OfficeIdSelectOne"));
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, selectAll, parameter_sql,parent_id)
VALUES("clientIdSelectAll", "clientId", "Client", "select", "number", 0, "Y", "(SELECT c.id,c.display_name as `Name`
from m_office o
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%')
join m_client c on c.office_id = ounder.id
and o.id = ${officeId})
union all
(select -10, '-')
order by 2",(SELECT p.id FROM stretchy_parameter p WHERE p.parameter_name = "OfficeIdSelectOne"));
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("disbursedOnStartDate", "disbursedOnStartDate", "Disbursed Start Date", "date", "date", "n/a");
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("disbursedOnEndDate", "disbursedOnEndDate", "Disbursed End Date", "date", "date", "n/a");



INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "productIdSelectAll"), "productIdSelectAll");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "OfficeIdSelectOne"), "OfficeIdSelectOne");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "groupIdSelectAll"), "groupIdSelectAll");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "clientIdSelectAll"), "clientIdSelectAll");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "disbursedOnStartDate"), "disbursedOnStartDate");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "disbursedOnEndDate"), "disbursedOnEndDate");
