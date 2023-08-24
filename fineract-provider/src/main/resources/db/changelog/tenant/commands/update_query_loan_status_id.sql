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

UPDATE stretchy_report SET report_sql = "SELECT '' AS 'Center No',g.account_no AS 'Group No',l.account_no AS 'No. Loan Agreement', pl.name AS 'Product',
CONCAT(c.firstname,'',c.lastname) AS 'Customer Name', l.submittedon_date as 'First Date',l.approved_principal AS 'Approved Amount', l.net_disbursal_amount AS 'Amount Disbursement', 0 AS 'Guarantee Amount',
0 AS 'Bills Administrative', pl.description AS 'Description'
FROM m_loan l
LEFT JOIN m_client c on c.id = l.client_id
LEFT JOIN m_group g ON g.id = l.group_id
JOIN m_office o ON (o.id = c.office_id OR o.id = g.office_id)
JOIN m_product_loan pl ON pl.id = l.product_id
WHERE (c.id = ${clientId} OR '-1' = '${clientId}')
AND (o.id = ${officeId} OR '-1' = '${officeId}')
AND (pl.id = ${productId} OR '-1' = '${productId}')
AND (g.id = ${groupId} OR '-1' = '${groupId}')
AND (l.loan_status_id = 300)
AND (l.disbursedon_date >= '${disbursedOnStartDate}' AND l.disbursedon_date <= '${disbursedOnEndDate}')" WHERE id = (SELECT tbl.id FROM (SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Active Loans") AS tbl);
