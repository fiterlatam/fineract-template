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

-- Loan Table Report --
INSERT INTO stretchy_report
(report_name, report_type, report_subtype, report_category, report_sql, description, core_report, use_report, self_service_user_report)
VALUES('Loan Portfolio Table Report', 'Table', NULL, 'Loan', 'SELECT
ml.id AS ''ID de préstamo'',
ml.contract AS ''Contrato de Prestamo No'',
mpl.name AS ''Producto'',
mc.display_name AS ''Nombre de Cliente'',
'' '' AS ''Solicitud No'',
ml.total_outstanding_derived  AS ''Saldo Ppal'',
0 AS ''Prov Interes'',
ml.interest_repaid_derived AS ''Intereses Pagados'',
0 AS ''Prov Otros Cargos'',
0 AS ''Otros Cargos Pagados'',
0 AS ''Prov Int Moratorios'',
0 AS ''Int Moratorios Pagados'',
ml.principal_disbursed_derived AS ''Monto Desembolsado'',
ml.total_outstanding_derived AS ''Total Pendiente'',
ml.interest_outstanding_derived AS ''Intereses Pendientes'',
ml.principal_outstanding_derived AS ''Monto Capital Vencido'',
laa.total_overdue_derived AS ''Monto Vencido'',
laa.interest_overdue_derived AS  ''Intereses Vencidos'',
0 AS ''Pago por Adelantado'',
0 AS ''Int Prest Dif Recuperacion'',
ml.approvedon_date AS ''Fecha procesado'',
ml.approvedon_date AS ''Fecha Aprovado'',
ml.approved_principal AS ''Monto_Aprobado'',
ml.maturedon_date AS ''Fecha Vencimiento'',
ml.number_of_repayments AS ''No de Cuotas'',
ml.principal_disbursed_derived AS ''CBL Monto Desembolsado'',
ml.disbursedon_date AS ''Fecha Desembolso'',
mlrs.first_installment_date  AS ''Fecha Primera Cuota'',
0 AS ''Restructurado SN'',
laa.overdue_since_date_derived AS ''Vencido Desde'',
DATEDIFF(CURDATE(), laa.overdue_since_date_derived) AS ''Dias de Vencido'',
mlt.last_repayment_date AS ''Fecha Ultimo Pago'',
facil.id  AS ''Cod Facilitador'',
mpf.name AS ''Nombre Cartera'',
facil.username AS ''Nombre Facilitador'',
CASE WHEN ml.loan_status_id = 0   THEN ''INVALID''
     WHEN ml.loan_status_id = 100 THEN ''SUBMITTED''
     WHEN ml.loan_status_id = 200 THEN ''APPROVED''
     WHEN ml.loan_status_id = 300 THEN ''ACTIVE''
     WHEN ml.loan_status_id = 303 THEN ''TRANSFER IN PROGRESS''
     WHEN ml.loan_status_id = 304 THEN ''TRANSFER ON HOLD''
     WHEN ml.loan_status_id = 400 THEN ''WITHDRAWN BY CLIENT''
     WHEN ml.loan_status_id = 500 THEN ''REJECTED''
     WHEN ml.loan_status_id = 600 THEN ''CLOSED''
     WHEN ml.loan_status_id = 601 THEN ''WRITTEN OFF''
     WHEN ml.loan_status_id = 602 THEN ''RESCHEDULED''
     WHEN ml.loan_status_id = 700 THEN ''OVERPAID''
END AS ''Estado de Cuenta'',
'' '' AS ''Fecha Saneamiento'',
0 AS ''No de Prestatarios'',
0 AS ''Monto Saneado'',
0 AS ''Recuperacion Saneamiento'',
ml.nominal_interest_rate_per_period AS ''Tasa Int'',
mpg.meeting_day AS ''Dia de Reunion'',
mpg.meeting_day AS ''Fecha de Reunion'',
mpg.display_name AS ''Nombre de Centro'',
mpg.id AS ''Centro No'',
mc.id AS ''Cliente No'',
mg.id AS ''Grupo No'',
ml.fee_charges_charged_derived AS ''Monto de Cuota'',
mg.display_name AS ''Nombre de Grupo'',
0 AS ''Clasif Activo'',
0 AS ''Pendiente desde Cuota No'',
0 AS ''Pendiente hasta Cuota No'',
0 AS ''Capital Pend al Inicio de Mes'',
0 AS ''Int Pend al Inicio de Mes'',
0 AS ''Capital Pagado en el mes'',
0 AS ''Int Pagados en el mes'',
0 AS ''Capital Adeudado en el mes'',
0 AS ''Int Adeudados en el mes'',
0 AS ''Capital Vencido Ppio de Anio'',
0 AS ''Int Vencidos Ppio de Anio'',
0 AS ''Capital Pagado en el Anio'',
0 AS ''Int Pagado en el Anio'',
0 AS ''Capital Adeudado en el Anio'',
0 AS ''Int Adeudados en el Anio'',
0 AS ''Ciclo de Prest'',
'' '' AS ''Celular'',
'' '' AS ''Dep No'',
'' '' AS ''Departamento'',
'' '' AS ''Mun No'',
'' '' AS ''Municipio'',
'' '' AS ''Direccion'',
'' '' AS ''Tipo de Documento'',
'' '' AS ''No Id Nac'',
'' '' AS ''No Doc Adicional'',
'' '' AS ''Estado Civil'',
mc.date_of_birth  AS ''Fecha Nacimiento'',
'' '' AS ''Genero'',
'' '' AS ''Tel'',
'' '' AS ''Sec Celular'',
'' '' AS ''Sec Dep No'',
'' '' AS ''Sec Departamento'',
'' '' AS ''Sec Mun No'',
'' '' AS ''Sec Municipio'',
'' '' AS ''Sec Direccion'',
'' '' AS ''Sec Tel'',
'' '' AS ''actividad'',
'' '' AS ''Rol del Grupo''
FROM m_office mo
INNER JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, ''%'') AND ounder.hierarchy LIKE CONCAT(''${currentUserHierarchy}'', ''%'')
INNER JOIN m_client mc ON mc.office_id=ounder.id
INNER JOIN m_loan ml ON ml.client_id = mc.id
LEFT JOIN m_product_loan mpl ON mpl.id = ml.product_id
LEFT JOIN m_loan_arrears_aging laa ON laa.loan_id = ml.id
LEFT JOIN (
     SELECT DISTINCT sc.loan_id AS loan_id, sc.duedate AS first_installment_date
    FROM m_loan_repayment_schedule sc
    ORDER BY sc.installment, sc.duedate
    )mlrs ON mlrs.loan_id = ml.id
LEFT JOIN (SELECT
            DISTINCT tx.loan_id AS loan_id, tx.transaction_date AS last_repayment_date
            FROM m_loan_transaction tx
            WHERE tx.transaction_type_enum IN (2) and tx.is_reversed = false
            ORDER BY tx.transaction_date DESC, tx.created_on_utc DESC, tx.id DESC
            ) mlt ON mlt.loan_id = ml.id
LEFT JOIN m_appuser facil ON facil.id = mc.activatedon_userid
LEFT JOIN m_portfolio mpf ON mpf.responsible_user_id = facil.id
LEFT JOIN m_group_client mgc ON mgc.client_id = mc.id
LEFT JOIN m_group mg ON mg.id = mgc.group_id
LEFT JOIN m_group mpg ON mpg.id = mg.parent_id
WHERE mo.id = ''${officeId}''
AND (ml.product_id = ''${productId}'' OR ''-1'' = ''${productId}'')
AND (ml.submittedon_date BETWEEN ''${submittedOnStartDate}'' AND ''${submittedOnEndDate}'')
AND (''-1'' = ''${loanOwnerType}'' OR ''1'' = ''${loanOwnerType}'')
GROUP BY ml.id, ounder.HIERARCHY

UNION ALL

SELECT
ml.id AS ''Loan ID'',
ml.contract AS ''Contrato de Prestamo No'',
mpl.name AS ''Producto'',
mg.display_name  AS ''Nombre de Cliente'',
'' '' AS ''Solicitud No'',
ml.total_outstanding_derived  AS ''Saldo Ppal'',
0 AS ''Prov Interes'',
ml.interest_repaid_derived AS ''Intereses Pagados'',
0 AS ''Prov Otros Cargos'',
0 AS ''Otros Cargos Pagados'',
0 AS ''Prov Int Moratorios'',
0 AS ''Int Moratorios Pagados'',
ml.principal_disbursed_derived AS ''Monto Desembolsado'',
ml.total_outstanding_derived AS ''Total Pendiente'',
ml.interest_outstanding_derived AS ''Intereses Pendientes'',
ml.principal_outstanding_derived AS ''Monto Capital Vencido'',
laa.total_overdue_derived AS ''Monto Vencido'',
laa.interest_overdue_derived AS  ''Intereses Vencidos'',
0 AS ''Pago por Adelantado'',
0 AS ''Int Prest Dif Recuperacion'',
ml.approvedon_date AS ''Fecha procesado'',
ml.approvedon_date AS ''Fecha Aprovado'',
ml.approved_principal AS ''Monto_Aprobado'',
ml.maturedon_date AS ''Fecha Vencimiento'',
ml.number_of_repayments AS ''No de Cuotas'',
ml.principal_disbursed_derived AS ''CBL Monto Desembolsado'',
ml.disbursedon_date AS ''Fecha Desembolso'',
mlrs.first_installment_date  AS ''Fecha Primera Cuota'',
0 AS ''Restructurado SN'',
laa.overdue_since_date_derived AS ''Vencido Desde'',
DATEDIFF(CURDATE(), laa.overdue_since_date_derived) AS ''Dias de Vencido'',
mlt.last_repayment_date AS ''Fecha Ultimo Pago'',
facil.id  AS ''Cod Facilitador'',
mpf.name AS ''Nombre Cartera'',
facil.username AS ''Nombre Facilitador'',
CASE WHEN ml.loan_status_id = 0   THEN ''INVALID''
     WHEN ml.loan_status_id = 100 THEN ''SUBMITTED''
     WHEN ml.loan_status_id = 200 THEN ''APPROVED''
     WHEN ml.loan_status_id = 300 THEN ''ACTIVE''
     WHEN ml.loan_status_id = 303 THEN ''TRANSFER IN PROGRESS''
     WHEN ml.loan_status_id = 304 THEN ''TRANSFER ON HOLD''
     WHEN ml.loan_status_id = 400 THEN ''WITHDRAWN BY CLIENT''
     WHEN ml.loan_status_id = 500 THEN ''REJECTED''
     WHEN ml.loan_status_id = 600 THEN ''CLOSED''
     WHEN ml.loan_status_id = 601 THEN ''WRITTEN OFF''
     WHEN ml.loan_status_id = 602 THEN ''RESCHEDULED''
     WHEN ml.loan_status_id = 700 THEN ''OVERPAID''
END AS ''Estado de Cuenta'',
'' '' AS ''Fecha Saneamiento'',
0 AS ''No de Prestatarios'',
0 AS ''Monto Saneado'',
0 AS ''Recuperacion Saneamiento'',
ml.nominal_interest_rate_per_period AS ''Tasa Int'',
mpg.meeting_day AS ''Dia de Reunion'',
mpg.meeting_day AS ''Fecha de Reunion'',
mpg.display_name AS ''Nombre de Centro'',
mpg.id AS ''Centro No'',
mg.id AS ''Cliente No'',
mg.id AS ''Grupo No'',
ml.fee_charges_charged_derived AS ''Monto de Cuota'',
mg.display_name AS ''Nombre de Grupo'',
0 AS ''Clasif Activo'',
0 AS ''Pendiente desde Cuota No'',
0 AS ''Pendiente hasta Cuota No'',
0 AS ''Capital Pend al Inicio de Mes'',
0 AS ''Int Pend al Inicio de Mes'',
0 AS ''Capital Pagado en el mes'',
0 AS ''Int Pagados en el mes'',
0 AS ''Capital Adeudado en el mes'',
0 AS ''Int Adeudados en el mes'',
0 AS ''Capital Vencido Ppio de Anio'',
0 AS ''Int Vencidos Ppio de Anio'',
0 AS ''Capital Pagado en el Anio'',
0 AS ''Int Pagado en el Anio'',
0 AS ''Capital Adeudado en el Anio'',
0 AS ''Int Adeudados en el Anio'',
0 AS ''Ciclo de Prest'',
'' '' AS ''Celular'',
'' '' AS ''Dep No'',
'' '' AS ''Departamento'',
'' '' AS ''Mun No'',
'' '' AS ''Municipio'',
'' '' AS ''Direccion'',
'' '' AS ''Tipo de Documento'',
'' '' AS ''No Id Nac'',
'' '' AS ''No Doc Adicional'',
'' '' AS ''Estado Civil'',
mg.formation_date AS ''Fecha Nacimiento'',
'' '' AS ''Genero'',
'' '' AS ''Tel'',
'' '' AS ''Sec Celular'',
'' '' AS ''Sec Dep No'',
'' '' AS ''Sec Departamento'',
'' '' AS ''Sec Mun No'',
'' '' AS ''Sec Municipio'',
'' '' AS ''Sec Direccion'',
'' '' AS ''Sec Tel'',
'' '' AS ''actividad'',
'' '' AS ''Rol del Grupo''
FROM m_office mo
INNER JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, ''%'') AND ounder.hierarchy LIKE CONCAT(''${currentUserHierarchy}'', ''%'')
INNER JOIN m_group mg ON mg.office_id = ounder.id
INNER JOIN m_loan ml ON ml.group_id = mg.id
LEFT JOIN m_product_loan mpl ON mpl.id = ml.product_id
LEFT JOIN m_loan_arrears_aging laa ON laa.loan_id = ml.id
LEFT JOIN (
     SELECT DISTINCT sc.loan_id AS loan_id, sc.duedate AS first_installment_date
    FROM m_loan_repayment_schedule sc
    ORDER BY sc.installment, sc.duedate
    )mlrs ON mlrs.loan_id = ml.id
LEFT JOIN (SELECT
            DISTINCT tx.loan_id AS loan_id, tx.transaction_date AS last_repayment_date
            FROM m_loan_transaction tx
            WHERE tx.transaction_type_enum IN (2) and tx.is_reversed = false
            ORDER BY tx.transaction_date DESC, tx.created_on_utc DESC, tx.id DESC
            ) mlt ON mlt.loan_id = ml.id
LEFT JOIN m_appuser facil ON facil.id = mg.activatedon_userid
LEFT JOIN m_portfolio mpf ON mpf.id = mg.portfolio_id
LEFT JOIN m_group mpg ON mpg.id = mg.parent_id
WHERE mo.id = ''${officeId}''
AND (ml.product_id = ''${productId}'' OR ''-1'' = ''${productId}'')
AND (ml.submittedon_date BETWEEN ''${submittedOnStartDate}'' AND ''${submittedOnEndDate}'')
AND (''-1'' = ''${loanOwnerType}'' OR ''2'' = ''${loanOwnerType}'')
GROUP BY ml.id, ounder.hierarchy', 'General Loan Report', 1, 1, 0);

INSERT INTO stretchy_parameter
(parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, special, selectOne, selectAll, parameter_sql, parent_id)
VALUES('submittedOnStartDateSelect', 'submittedOnStartDate', 'Submitted Start Date', 'date', 'date', 'today', NULL, NULL, NULL, NULL, NULL);
INSERT INTO stretchy_parameter
(parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, special, selectOne, selectAll, parameter_sql, parent_id)
VALUES('submittedOnEndDateSelect', 'submittedOnEndDate', 'Submitted End Date', 'date', 'date', 'today', NULL, NULL, NULL, NULL, NULL);

INSERT INTO stretchy_parameter
(id, parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, special, selectOne, selectAll, parameter_sql, parent_id)
VALUES(NULL, 'loanOwnerTypeSelectAll', 'loanOwnerType', 'Owner Type', 'select', 'number', '-1', NULL, NULL, 'Y', 'SELECT 1 AS id, ''Client'' AS name
UNION ALL
SELECT 2 AS id, ''Group'' AS name', NULL);

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Loan Portfolio Table Report"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "submittedOnStartDateSelect"), "submittedOnEndDate");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Loan Portfolio Table Report"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "submittedOnEndDateSelect"), "submittedOnEndDate");


INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Loan Portfolio Table Report"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "OfficeIdSelectOne"), "officeId");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Loan Portfolio Table Report"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "loanOwnerTypeSelectAll"), "loanOwnerType");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Loan Portfolio Table Report"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "productIdSelectAll"), "productId");
