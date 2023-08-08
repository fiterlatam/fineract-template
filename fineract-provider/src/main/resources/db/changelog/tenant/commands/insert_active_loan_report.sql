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
VALUES ("Active Loans (FBR)", "Table", "Loan", "SELECT g.account_no AS centerNumber,g.account_no AS groupNumber,l.account_no AS loanAgreementNumber, pl.name AS productName, c.fullname AS customerName , l.submittedon_date as firstDate,
l.approved_principal AS approvedAmount, l.net_disbursal_amount AS amountDisbursed, l.approved_principal AS approvedAmount, 0 AS guaranteeAmount, 0 AS billsAdministrative,
pl.description AS description, l.loan_status_id
FROM m_loan l
LEFT JOIN m_group g ON g.id = l.group_id
LEFT JOIN m_product_loan pl ON pl.id = l.product_id
LEFT JOIN m_client c ON c.id = l.client_id
WHERE   l.loan_status_id = ${loanStatusId} AND l.disbursedon_date <= ${disbursedonStartDate} AND l.disbursedon_date >= ${disbursedonEndDate} AND g.account_no = ${groupNumber}
 AND g.account_no = ${centerNumber} AND c.account_no  = ${clientNumber}", "Get Active Loans", 1, 1, 0);

INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("loanStatusId", "loanStatusId", "Loan Status Id", "number", "number", "n/a");
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("centerNumber", "centerNumber", "Center No", "text", "string", "n/a");
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("groupNumber", "groupNumber", "Group No", "text", "string", "n/a");
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("productNumber", "productNumber", "Product No", "text", "string", "n/a");
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("clientNumber", "clientNumber", "Client No", "text", "string", "n/a");
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("disbursedonStartDate", "disbursedonStartDate", "Disbursed Start Date", "date", "date", "n/a");
INSERT INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default) VALUES("disbursedonEndDate", "disbursedonEndDate", "Disbursed End Date", "date", "date", "n/a");

INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "loanStatusId"), "loanStatusId");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "centerNumber"), "centerNumber");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "groupNumber"), "groupNumber");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "productNumber"), "productNumber");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "clientNumber"), "clientNumber");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "disbursedonStartDate"), "disbursedonStartDate");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Active Loans"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "disbursedonEndDate"), "disbursedonEndDate");
