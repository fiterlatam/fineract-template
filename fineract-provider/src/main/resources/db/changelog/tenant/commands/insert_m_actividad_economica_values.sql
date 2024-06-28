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

insert into m_actividad_economica (sector_id,name) values (1,'Venta por Catalogo');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de zapatos');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de productos de cuero');
insert into m_actividad_economica (sector_id,name) values (1,'compra y venta de artesanía');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de pollos');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de cerdos');
insert into m_actividad_economica (sector_id,name) values (1,'compra y venta de pollos y cerdos');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de ovejas');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de verduras y frutas');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de productos de carne');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de medicina');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de ropa nueva');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de ropa usada');
insert into m_actividad_economica (sector_id,name) values (1,'Tienda de consumo diario');
insert into m_actividad_economica (sector_id,name) values (1,'Librería');
insert into m_actividad_economica (sector_id,name) values (1,'Fotocopias');
insert into m_actividad_economica (sector_id,name) values (1,'Compra y venta de pan');
insert into m_actividad_economica (sector_id,name) values (1,'Venta de conservas');
insert into m_actividad_economica (sector_id,name) values (1,'Venta de Gas');
insert into m_actividad_economica (sector_id,name) values (1,'Venta de leña');
insert into m_actividad_economica (sector_id,name) values (1,'Venta de granos básicos');
insert into m_actividad_economica (sector_id,name) values (1,'Ferretería');
insert into m_actividad_economica (sector_id,name) values (1,'Venta de electrodomésticos');
insert into m_actividad_economica (sector_id,name) values (1,'Venta de juguetes');
insert into m_actividad_economica (sector_id,name) values (2,'Salón de belleza');
insert into m_actividad_economica (sector_id,name) values (2,'Alquileres');
insert into m_actividad_economica (sector_id,name) values (2,'Transportes');
insert into m_actividad_economica (sector_id,name) values (2,'Molino de nixtamal');
insert into m_actividad_economica (sector_id,name) values (2,'Educación');
insert into m_actividad_economica (sector_id,name) values (2,'Servicios de salud');
insert into m_actividad_economica (sector_id,name) values (2,'Hotel');
insert into m_actividad_economica (sector_id,name) values (2,'Peluquería');
insert into m_actividad_economica (sector_id,name) values (2,'Servicios fúnebres');
insert into m_actividad_economica (sector_id,name) values (2,'Turismo');
insert into m_actividad_economica (sector_id,name) values (2,'Café Internet');
insert into m_actividad_economica (sector_id,name) values (3,'Refacciones');
insert into m_actividad_economica (sector_id,name) values (3,'Elaboración de comida');
insert into m_actividad_economica (sector_id,name) values (3,'Elaboración de productos de cuero');
insert into m_actividad_economica (sector_id,name) values (3,'Elaboración de productos de artesanía');
insert into m_actividad_economica (sector_id,name) values (3,'Crianza de pollos');
insert into m_actividad_economica (sector_id,name) values (3,'Crianza de cerdos');
insert into m_actividad_economica (sector_id,name) values (3,'Elaboración de zapatos');
insert into m_actividad_economica (sector_id,name) values (3,'Elaboración de ropa');
insert into m_actividad_economica (sector_id,name) values (3,'Tortillería');
insert into m_actividad_economica (sector_id,name) values (3,'Elaboración de pan');
insert into m_actividad_economica (sector_id,name) values (3,'Elaboración de ropa típica');
insert into m_actividad_economica (sector_id,name) values (3,'Elaboración de cortinas y manteles');
insert into m_actividad_economica (sector_id,name) values (3,'Pastelería');
insert into m_actividad_economica (sector_id,name) values (3,'Crianza de animales de patio');
insert into m_actividad_economica (sector_id,name) values (3,'Refrescos y licuados');
insert into m_actividad_economica (sector_id,name) values (3,'Crianza de Ovejas');
insert into m_actividad_economica (sector_id,name) values (3,'Crianza de Toros');
insert into m_actividad_economica (sector_id,name) values (3,'Elaboración de dulces típicos y chocolate');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo de Tomate');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo de Cebolla');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo de papa');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo de zanahoria');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo de arveja');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo de ajo');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo de repollo');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo de ejote');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo deBrocoli');
insert into m_actividad_economica (sector_id,name) values (4,'Cultivo de ejote francés');
insert into m_actividad_economica (sector_id,name) values (5,'Otros');
