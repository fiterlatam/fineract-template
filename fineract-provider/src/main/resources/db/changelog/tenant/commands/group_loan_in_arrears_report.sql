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

-- Group Loan In Arrears Report --

INSERT INTO fineract_default.stretchy_report
(report_name, report_type, report_subtype, report_category, report_sql, description, core_report, use_report, self_service_user_report)
VALUES('Group Loans in Arrears', 'Table', NULL, 'Loan', 'SELECT
ml.id AS ''ID de préstamo'',
mag.name AS Agencia,
faci.username  AS ''Nombre facilitador'',
mpg.display_name AS ''Centro'',
mg.display_name AS ''Nombre de Grupo'',
mg.id AS ''Grupo No.'',
ml.account_no  AS ''Código de Préstamo'',
mg.display_name AS ''Nombre del Cliente'',
mg.group_location AS ''Directión'',
mg.legacy_number AS ''No. de Celular'',
mlt.last_transaction_date AS ''Última Fecha de Pago'',
mlrs.duedate AS ''Próxima Fecha de Pago'',
laa.total_overdue_derived AS ''Capital Pendiente''
FROM m_office mo
INNER JOIN m_office ounder ON ounder.HIERARCHY LIKE CONCAT(mo.hierarchy, ''%'')
INNER JOIN m_group mg ON mg.office_id = ounder.id
INNER JOIN m_loan ml ON ml.group_id = mg.id
INNER JOIN m_loan_arrears_aging laa ON laa.loan_id = ml.id
LEFT JOIN m_group mpg ON mpg.id = mg.parent_id
LEFT JOIN m_staff lo ON lo.id = ml.loan_officer_id
LEFT JOIN m_guarantor gua ON gua.loan_id = ml.id
LEFT JOIN m_agency mag ON mag.id = mg.responsible_user_id
LEFT JOIN m_appuser faci ON faci.id = mpg.submittedon_userid
LEFT JOIN (SELECT
            loan_id,
            transaction_date last_transaction_date,
            amount last_transaction_amount
            FROM m_loan_transaction
            WHERE transaction_type_enum IN (1, 2, 4, 5, 9) and is_reversed = false
            order by loan_id, transaction_date DESC
            ) mlt on mlt.loan_id = ml.id
LEFT JOIN (
            SELECT DISTINCT sc.loan_id AS loan_id, sc.duedate AS duedate
            FROM m_loan_repayment_schedule sc
            WHERE sc.completed_derived = FALSE
            ORDER BY sc.duedate DESC
            ) mlrs ON mlrs.loan_id = ml.id
GROUP BY ml.id
WHERE mo.id = = ''${officeId}''
AND (ml.product_id = ''${productId}'' or ''-1'' = ''${productId}'')
AND (ml.group_id = ''${groupId}'' or ''-1'' = ''${groupId}'')
AND (ml.disbursedon_date  BETWEEN ''${disbursedOnStartDate}'' AND ''${disbursedOnEndDate}'')
AND ml.loan_status_id = 300', 'Group Loans that are in arrears', 1, 1, 0);


INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Group Loans in Arrears"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "productIdSelectAll"), "productIdSelectAll");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Group Loans in Arrears"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "OfficeIdSelectOne"), "OfficeIdSelectOne");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Group Loans in Arrears"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "groupIdSelectAll"), "groupIdSelectAll");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Group Loans in Arrears"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "disbursedOnStartDate"), "disbursedOnStartDate");
INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id
                                                                                               FROM stretchy_report sr WHERE sr.report_name = "Group Loans in Arrears"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "disbursedOnEndDate"), "disbursedOnEndDate");
