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
-- changeset fineract:2
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

--
-- Insert code values into m_code_value for code Ldepartamento
--

INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Alta Verapaz', '16', 1, 1101, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Baja Verapaz', '15', 2, 1102, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Chimaltenango', '4', 3, 1401, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Chiquimula', '20', 4, 1202, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'El Progreso', '2', 5, 1204, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Escuintla', '5', 6, 1403, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Guatemala', '1', 7, 1001, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Huehuetenango', '13', 8, 1601, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Izabal', '18', 9, 1201, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Jalapa', '21', 10, 1302, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Jutiapa', '22', 11, 1301, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Petén', '17', 12, 1701, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Quetzaltenango', '9', 13, 1502, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Quiché', '14', 14, 1602, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Retalhuleu', '11', 15, 1505, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Sacatepéquez', '3', 16, 1402, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'San Marcos', '12', 17, 1501, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Santa Rosa', '6', 18, 1303, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Sololá', '7', 19, 1504, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Suchitepéquez', '10', 20, 1506, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Totonicapán', '8', 21, 1503, 1, 0);
INSERT INTO `fineract_default`.`m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`, `is_mandatory`) VALUES (39, 'Zacapa', '19', 22, 1203, 1, 0);
