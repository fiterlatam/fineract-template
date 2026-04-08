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

INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'En caso de consolidación de deuda, adjunar documentos como comprobantes de la deuda.', 'En caso de consolidación de deuda, adjunar documentos como comprobantes de la deuda.', 'PDF/IMAGE', 5, '2026-04-08 15:17:36', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Formato de monto a retener', 'Formato de monto a retener', 'PDF', 5, '2026-04-08 15:17:20', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), '\"Informe de desarrollo empresarial, detalle de ventas y plan de acción.													\"', '\"Informe de desarrollo empresarial, detalle de ventas y plan de acción.													\"', 'PDF', 5, '2026-04-08 15:17:12', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Contrato de arrendamiento (crédito agrícola)', 'Contrato de arrendamiento (crédito agrícola)', 'PDF', 5, '2026-04-08 15:16:48', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Informe técnico agrícola (Cuando aplique)', 'Informe técnico agrícola (Cuando aplique)', 'PDF', 5, '2026-04-08 15:16:39', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Foto de libreta de ahorros (sólo en caso de transferencia)', 'Foto de libreta de ahorros (sólo en caso de transferencia)', 'PDF/IMAGE', 5, '2026-04-08 15:16:29', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Fotografias de la vivienda(con coordenadas)', 'Fotografias de la vivienda(con coordenadas)', 'PDF/IMAGE', 5, '2026-04-08 15:16:19', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Fotografias del Negocio de Crédito Individual, debe figurar clienta y asesor (minimo 3 fotografías con coordenadas)', 'Fotografias del Negocio de Crédito Individual, debe figurar clienta y asesor (minimo 3 fotografías con coordenadas)', 'PDF/IMAGE', 5, '2026-04-08 15:16:07', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Fotocopia Patente de Sociedad', 'Fotocopia Patente de Sociedad', 'PDF/IMAGE', 5, '2026-04-08 15:15:28', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Fotocopia Patente de Comercio', 'Fotocopia Patente de Comercio', 'PDF/IMAGE', 5, '2026-04-08 15:15:20', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Copia del RTU', 'Copia del RTU', 'PDF/IMAGE', 5, '2026-04-08 15:15:10', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Fotocopia de facturas de compra/ venta de mercadería o materia prima', 'Fotocopia de facturas de compra/ venta de mercadería o materia prima', 'PDF/IMAGE', 5, '2026-04-08 15:15:02', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Segmentación', 'Segmentación', 'PDF/IMAGE', 5, '2026-04-08 15:14:53', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Carta de compromiso a participar en el programa firmado por clienta y facilitador.', 'Carta de compromiso a participar en el programa firmado por clienta y facilitador.', 'PDF/IMAGE', 5, '2026-04-08 15:14:44', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Fotocopia de tarjeta de salud (donde lo requiera las normas guatemaltecas)', 'Fotocopia de tarjeta de salud (donde lo requiera las normas guatemaltecas)', 'PDF/IMAGE', 5, '2026-04-08 15:14:33', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'En caso no tenga servicios, constancia de risidencia emitida por el COCODE', 'En caso no tenga servicios, constancia de risidencia emitida por el COCODE', 'PDF/IMAGE', 5, '2026-04-08 15:14:17', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Recibo de servicios públicos máximo dos meses atrasados donde esté la dirección exacta', 'Recibo de servicios públicos máximo dos meses atrasados donde esté la dirección exacta', 'PDF/IMAGE', 5, '2026-04-08 15:13:54', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Fotocopia completa de DPI de la deudora', 'Fotocopia completa de DPI de la deudora', 'PDF/IMAGE', 5, '2026-04-08 15:12:58', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ((select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Documentacion Deudora'), 'Formularios de solicitud de crédito digitales', 'Formularios de solicitud de crédito digitales', 'EXCEL', 5, '2026-04-08 15:08:14', b'1');
