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

INSERT INTO m_code (`code_name`, `is_system_defined`)
VALUES ('loanCycleCompletedOptions', b'1'),
       ('loanPurposeOptions', b'1'),
       ('businessEvolutionOptions', b'1'),
       ('yesnoOptions', b'1'),
       ('businessExperienceOptions', b'1'),
       ('businessLocationOptions', b'1'),
       ('clientTypeOptions', b'1'),
       ('loanStatusOptions', b'1'),
       ('institutionTypeOptions', b'1'),
       ('housingTypeOptions', b'1'),
       ('classificationOptions', b'1'),
       ('jobTypeOptions', b'1'),
       ('educationLevelOptions', b'1'),
       ('maritalStatusOptions', b'1'),
       ('groupPositionOptions', b'1'),
       ('sourceOfFundsOptions', b'1'),
       ('cancellationReasonOptions', b'1');
--
-- Insert code values into m_code_value for code loanCycleCompletedOptions
--

SELECT @loanCycleCompletedOptions := (select id from m_code where code_name='loanCycleCompletedOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@loanCycleCompletedOptions, 'newCustomer', 'Cliente nuevo "0"', 0, 1001, 1, 0),
       (@loanCycleCompletedOptions, 'withdrawal', 'Retirada > 1 año', 0, 1002, 1, 0),
       (@loanCycleCompletedOptions, 'fromAnotherGroup', 'Viene de Otro grupo', 0, 1003, 1, 0);

--
-- Insert code values into m_code_value for code cancellationReasonOptions
--

SELECT @cancellationReasonOptions := (select id from m_code where code_name='cancellationReasonOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@cancellationReasonOptions, 'recredit', 'Recredito', 0, 1001, 1, 0),
       (@cancellationReasonOptions, 'restructure', 'Reestructura', 0, 1002, 1, 0),
       (@cancellationReasonOptions, 'externalInstitutionCredit', 'Crédito en otra institución', 0, 1003, 1, 0),
       (@cancellationReasonOptions, 'other', 'Otros', 0, 1004, 1, 0);

--
-- Insert code values into m_code_value for code sourceOfFundsOptions
--

SELECT @sourceOfFundsId := (select id from m_code where code_name='sourceOfFundsOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@sourceOfFundsId, 'recredit', 'Recredito', 0, 1001, 1, 0),
       (@sourceOfFundsId, 'restructure', 'Reestructura', 0, 1002, 1, 0),
       (@sourceOfFundsId, 'externalInstitutionCredit', 'Crédito en otra institución', 0, 1003, 1, 0),
       (@sourceOfFundsId, 'businessProfit', 'Utilidad del Negocio', 0, 1004, 1, 0),
       (@sourceOfFundsId, 'other', 'Otros', 0, 1005, 1, 0);

--
-- Insert code values into m_code_value for code groupPositionOptions
--

SELECT @groupPositionOptions := (select id from m_code where code_name='groupPositionOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@groupPositionOptions, 'member', 'Miembro', 0, 1001, 1, 0),
       (@groupPositionOptions, 'chairperson', 'Grupo Presidenta', 0, 1002, 1, 0),
       (@groupPositionOptions, 'secretary', 'Secretaria', 0, 1003, 1, 0),
       (@groupPositionOptions, 'treasurer', 'Tesorera', 0, 1004, 1, 0);

--
-- Insert code values into m_code_value for code maritalStatusOptions
--

SELECT @maritalStatusOptions := (select id from m_code where code_name='maritalStatusOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@maritalStatusOptions, 'married', 'Casada /Unida', 0, 1001, 1, 0),
       (@maritalStatusOptions, 'divorced', 'Separada / Divorciada', 0, 1002, 1, 0),
       (@maritalStatusOptions, 'single', 'Soltera', 0, 1003, 1, 0),
       (@maritalStatusOptions, 'widow', 'Viuda', 0, 1004, 1, 0);

--
-- Insert code values into m_code_value for code educationLevelOptions
--
SELECT @educationLevelOptions := (select id from m_code where code_name='educationLevelOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@educationLevelOptions, 'ANALF', 'ANALF', 0, 1001, 1, 0),
       (@educationLevelOptions, 'BAS', 'BAS', 0, 1002, 1, 0),
       (@educationLevelOptions, 'DIV', 'DIV', 0, 1003, 1, 0),
       (@educationLevelOptions, 'PRIM', 'PRIM', 0, 1004, 1, 0),
       (@educationLevelOptions, 'UNI', 'UNI', 0, 1005, 1, 0),
       (@educationLevelOptions, 'LIC', 'LIC', 0, 1006, 1, 0);

--
-- Insert code values into m_code_value for code jobTypeOptions
--
SELECT @jobTypeOptions := (select id from m_code where code_name='jobTypeOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@jobTypeOptions, 'employee', 'Empleada', 0, 1001, 1, 0),
       (@jobTypeOptions, 'microentreprenuer', 'Microempresaria', 0, 1002, 1, 0);


--
-- Insert code values into m_code_value for code jobTypeOptions
--
SELECT @classificationOptions := (select id from m_code where code_name='classificationOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@classificationOptions, 'Agricultura', 'Agricultura', 0, 1001, 1, 0),
       (@classificationOptions, 'Ganadería', 'Ganadería', 0, 1002, 1, 0),
       (@classificationOptions, 'Animales', 'Animales', 0, 1003, 1, 0),
       (@classificationOptions, 'Alimentos', 'Alimentos', 0, 1004, 1, 0),
       (@classificationOptions, 'Producción', 'Producción', 0, 1005, 1, 0),
       (@classificationOptions, 'Artesanías', 'Artesanías', 0, 1006, 1, 0),
       (@classificationOptions, 'Textiles', 'Textiles', 0, 1007, 1, 0),
       (@classificationOptions, 'Servicios', 'Servicios', 0, 1008, 1, 0),
       (@classificationOptions, 'Comercio', 'Comercio', 0, 1009, 1, 0),
       (@classificationOptions, 'Otros', 'Otros', 0, 1010, 1, 0);

--
-- Insert code values into m_code_value for code yesnoOptions
--
SELECT @yesnoOptions := (select id from m_code where code_name='yesnoOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@yesnoOptions, 'YES', 'Si', 0, 1001, 1, 0),
       (@yesnoOptions, 'NO', 'No', 0, 1002, 1, 0);


--
-- Insert code values into m_code_value for code housingTypeOptions
--
SELECT @housingTypeOptions := (select id from m_code where code_name='housingTypeOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@housingTypeOptions, 'RENTING', 'Alquilada', 0, 1001, 1, 0),
       (@housingTypeOptions, 'FAMILY', 'Familiar', 0, 1002, 1, 0),
       (@housingTypeOptions, 'OWN', 'Propia', 0, 1003, 1, 0),
       (@housingTypeOptions, 'PAYING', 'Pagando', 0, 1004, 1, 0);

--
-- Insert code values into m_code_value for code institutionTypeOptions
--
SELECT @institutionTypeOptions := (select id from m_code where code_name='institutionTypeOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@institutionTypeOptions, 'Banco', 'Banco', 0, 1001, 1, 0),
       (@institutionTypeOptions, 'Micro-Fin', 'Micro-Fin', 0, 1002, 1, 0),
       (@institutionTypeOptions, 'Coop', 'Coop', 0, 1003, 1, 0),
       (@institutionTypeOptions, 'Comercial', 'Comercial', 0, 1004, 1, 0);


--
-- Insert code values into m_code_value for code loanStatusOptions
--
SELECT @loanStatusOptions := (select id from m_code where code_name='loanStatusOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@loanStatusOptions, 'al_dia', 'al_dia', 0, 1001, 1, 0),
       (@loanStatusOptions, 'en_mora', 'en_mora', 0, 1002, 1, 0);

--
-- Insert code values into m_code_value for code businessLocationOptions
--
SELECT @businessLocationOptions := (select id from m_code where code_name='businessLocationOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@businessLocationOptions, 'HOUSE', 'casa', 0, 1001, 1, 0),
       (@businessLocationOptions, 'MARKETPLACE', 'mercado', 0, 1002, 1, 0),
       (@businessLocationOptions, 'LOCAL', 'local', 0, 1003, 1, 0),
       (@businessLocationOptions, 'NOPREMISES', 'no tiene local', 0, 1004, 1, 0);


--
-- Insert code values into m_code_value for code businessExperienceOptions
--
SELECT @businessExperienceOptions := (select id from m_code where code_name='businessExperienceOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@businessExperienceOptions, '<6m', '<6m', 0, 1001, 1, 0),
       (@businessExperienceOptions, '6m-1a', '6m-1a', 0, 1002, 1, 0),
       (@businessExperienceOptions, '1a- 2a', '1a- 2a', 0, 1003, 1, 0),
       (@businessExperienceOptions, '>2a', '>2a', 0, 1004, 1, 0);


--
-- Insert code values into m_code_value for code businessEvolutionOptions
--
SELECT @businessEvolutionOptions := (select id from m_code where code_name='businessEvolutionOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@businessEvolutionOptions, 'GREW', 'Creció', 0, 1001, 1, 0),
       (@businessEvolutionOptions, 'STABLE', 'Estable', 0, 1002, 1, 0),
       (@businessEvolutionOptions, 'DECREASED', 'Disminuyó', 0, 1003, 1, 0);


--
-- Insert code values into m_code_value for code loanPurposeOptions
--
SELECT @loanPurposeOptions := (select id from m_code where code_name='loanPurposeOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@loanPurposeOptions, 'WORKINGCAPITAL', 'Capital de Trabajo y Activos Fijos', 0, 1001, 1, 0),
       (@loanPurposeOptions, 'SALARY', 'Capital de Trabajo', 0, 1002, 1, 0),
       (@loanPurposeOptions, 'FIXEDASSETS', 'Activos Fijos', 0, 1003, 1, 0);

SELECT @clientTypeOptions := (select id from m_code where code_name='clientTypeOptions' limit 1);

INSERT INTO `m_code_value`(`code_id`, `code_value`, `code_description`, `order_position`, `code_score`, `is_active`,
                           `is_mandatory`)
VALUES (@clientTypeOptions, 'A', 'A', 0, 1001, 1, 0),
       (@clientTypeOptions, 'B', 'B', 0, 1002, 1, 0),
       (@clientTypeOptions, 'C', 'C', 0, 1003, 1, 0),
       (@clientTypeOptions, 'D', 'D', 0, 1004, 1, 0);

