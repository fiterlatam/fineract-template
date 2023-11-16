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

-- Drop previous tables --
DROP TABLE IF EXISTS checklist_decision_making;
DROP TABLE IF EXISTS checklist_categories;


-- Insert policy names --
INSERT INTO m_policy (id, name, label, description) VALUES(1, 'New client categorization', 'label.category.newclient', 'Categorización de clienta nueva');
INSERT INTO m_policy (id, name, label, description) VALUES(2, 'Recurring customer categorization', 'label.category.recurringcustomer', 'Categoríazación de clienta recurruente');
INSERT INTO m_policy (id, name, label, description) VALUES(3, 'Increase percentage', 'label.category.increasepercentage', 'Porcentaje de incremento');
INSERT INTO m_policy (id, name, label, description) VALUES(4, 'Mandatory to attach photographs and investment plan', 'label.category.mandatoryphotos', 'Obligatorio adjuntar fotografías y plan de inversión');
INSERT INTO m_policy (id, name, label, description) VALUES(5, 'Client age', 'label.category.clientage', 'Edad de la cliente');
INSERT INTO m_policy (id, name, label, description) VALUES(6, 'Number of members according to policy', 'label.category.memberspolicycount', 'Cantidad de integrantes según política');
INSERT INTO m_policy (id, name, label, description) VALUES(7, 'Minimum and maximum amount', 'label.category.minmaxamount', 'Monto mínimo y máximo');
INSERT INTO m_policy (id, name, label, description) VALUES(8, 'Value disparity', 'label.category.disparity', 'Disparidad de valores');
INSERT INTO m_policy (id, name, label, description) VALUES(9, 'Percentage of members starting business', 'label.category.startingbusiness', 'Porcentaje de integrantes iniciando negocio');
INSERT INTO m_policy (id, name, label, description) VALUES(10, 'Percentage of members with their own home', 'label.category.ownhome', 'Porcentaje de integrantes con vivienda propia');
INSERT INTO m_policy (id, name, label, description) VALUES(11, 'President of the Board of Directors of the BC', 'label.category.boardofdirectors', 'Presidente de Junta Directiva del BC');
INSERT INTO m_policy (id, name, label, description) VALUES(12, 'General condition', 'label.category.overallcondition', 'Condición general');
INSERT INTO m_policy (id, name, label, description) VALUES(13, 'Categories of clients to accept', 'label.category.clientcategories', 'Categorías de clientes a aceptar');
INSERT INTO m_policy (id, name, label, description) VALUES(14, 'Amount requested in relation to the current amount of main products', 'label.category.requestedamount', 'Monto solicitado en relación al monto que tiene vigente en productos principales');
INSERT INTO m_policy (id, name, label, description) VALUES(15, 'Add endorsement', 'label.category.addendorsement', 'Agregar aval');
INSERT INTO m_policy (id, name, label, description) VALUES(16, 'Payments outside the current term of the main product', 'label.category.outsidetermpayents', 'Pagos fuera del plazo vigente del producto principal');
INSERT INTO m_policy (id, name, label, description) VALUES(17, 'Percentage of members of the same group who they can have parallel product', 'label.category.canhaveproduct', 'Porcentaje de integrantes del mismo grupo que pueden llegar a tener producto Paralelo');
INSERT INTO m_policy (id, name, label, description) VALUES(18, 'Gender', 'label.category.gender', 'Género');
INSERT INTO m_policy (id, name, label, description) VALUES(19, 'Nationality', 'label.category.nationality', 'Nacionalidad');
INSERT INTO m_policy (id, name, label, description) VALUES(20, 'Internal Credit History', 'label.category.internalcredithistory', 'Historial crédito INTERNO');
INSERT INTO m_policy (id, name, label, description) VALUES(21, 'External Credit History', 'label.category.externalcredithistory', 'Historial crédito EXTERNO');
INSERT INTO m_policy (id, name, label, description) VALUES(22, 'Do you register any lawsuit?', 'label.category.claimregistered', 'Do you register any lawsuit?');
INSERT INTO m_policy (id, name, label, description) VALUES(23, 'Housing Type', 'label.category.housingtype', 'Tipo de vivienda');
INSERT INTO m_policy (id, name, label, description) VALUES(24, 'Rental Age', 'label.category.rentalage', 'Antigüedad de alquiler');
INSERT INTO m_policy (id, name, label, description) VALUES(25, 'Age Of Business', 'label.category.businessage', 'Antigüedad del negocio');
INSERT INTO m_policy (id, name, label, description) VALUES(26, 'Credits', 'label.category.credits', 'Recréditos');
INSERT INTO m_policy (id, name, label, description) VALUES(27, 'Cancelled Cycles Count', 'label.category.cyclescount', 'Cantidade ciclos cancelados como grupo');
INSERT INTO m_policy (id, name, label, description) VALUES(28, ' Acceptance of new clients', 'label.category.newclientsacceptance', 'Aceptación de clientas nuevas');
INSERT INTO m_policy (id, name, label, description) VALUES(29, 'Present agricultural technical diagnosis (Commcare)', 'label.category.agricultural.diagnosis', 'Presentar diagnótico técnico agrícola (Commcare)');
INSERT INTO m_policy (id, name, label, description) VALUES(30, 'Age', 'label.category.clientage', 'Edad');
INSERT INTO m_policy (id, name, label, description) VALUES(31, 'Amount', 'label.category.amount', 'Monto');
INSERT INTO m_policy (id, name, label, description) VALUES(32, 'Percentage of members with agricultural business', 'label.agribusiness.percentage', 'Porcentaje de integrantes con negocio agrícola');
INSERT INTO m_policy (id, name, label, description) VALUES(33, 'Percentage of members with their own business', 'label.own.business.percentage', 'Porcentaje de integrantes con negocio propia');

-- Insert product policy data --
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'INDIVIDUAL', 1);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'INDIVIDUAL', 1);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'INDIVIDUAL', 1);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'INDIVIDUAL', 1);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'INDIVIDUAL', 1);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'INDIVIDUAL', 2);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'INDIVIDUAL', 2);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'INDIVIDUAL', 2);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'INDIVIDUAL', 2);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'INDIVIDUAL', 2);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(7, 'INDIVIDUAL', 2);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(3, 'INDIVIDUAL', 2);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'INDIVIDUAL', 3);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'INDIVIDUAL', 3);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'INDIVIDUAL', 3);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'INDIVIDUAL', 3);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'INDIVIDUAL', 3);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(7, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'INDIVIDUAL', 5);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'INDIVIDUAL', 5);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'INDIVIDUAL', 5);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'INDIVIDUAL', 5);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'INDIVIDUAL', 5);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'GROUP', 6);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'GROUP', 6);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'GROUP', 6);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'GROUP', 6);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'GROUP', 6);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'GROUP', 7);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'GROUP', 7);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'GROUP', 7);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'GROUP', 7);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'GROUP', 7);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'GROUP', 8);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'GROUP', 8);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'GROUP', 8);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'GROUP', 9);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'GROUP', 10);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'GROUP', 10);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(2, 'GROUP', 11);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'GROUP', 11);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'GROUP', 11);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'GROUP', 11);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'GROUP', 11);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(7, 'INDIVIDUAL', 12);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(3, 'INDIVIDUAL', 12);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(7, 'INDIVIDUAL', 13);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(3, 'INDIVIDUAL', 13);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(7, 'INDIVIDUAL', 14);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(3, 'INDIVIDUAL', 14);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(7, 'INDIVIDUAL', 15);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(7, 'INDIVIDUAL', 16);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(7, 'INDIVIDUAL', 17);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 18);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 19);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 20);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 21);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 22);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 23);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 24);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 25);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 26);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(9, 'GROUP', 27);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(7, 'INDIVIDUAL', 4);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'GROUP', 28);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 29);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(8, 'INDIVIDUAL', 29);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'INDIVIDUAL', 29);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 30);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(6, 'INDIVIDUAL', 31);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(5, 'GROUP', 32);
INSERT INTO m_product_policy (product_id, evaluation_type, policy_id) VALUES(4, 'GROUP', 33);
