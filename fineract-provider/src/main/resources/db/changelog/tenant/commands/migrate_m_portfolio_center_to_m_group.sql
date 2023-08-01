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

--migrate m_portfolio_center to m_group
insert into m_group(external_id, display_name,office_id,status_enum, level_id,submittedon_userid,activatedon_userid,hierarchy,submittedon_date, activation_date, account_no, meeting_day, meeting_start_date, meeting_end_date, meeting_start_time, meeting_end_time,
                    legacy_number,formation_date,responsible_user_id,createdby_id,created_date,lastmodifiedby_id,lastmodified_date,portfolio_id,city_id,state_province_id,type_id,distance_from_agency,reference_point)

select pc.id, pc.name as portfolio_name, mp.linked_office_id,300 as group_status,1 as group_level, pc.createdby_id, pc.createdby_id as activated_by, '.1.' as hierarchy, DATE_FORMAT(pc.created_date, "%y-%m-%d") as created_at, DATE_FORMAT(pc.created_date, "%y-%m-%d") as activation_date,
       LPAD(pc.id, 9, '0') as account_no,pc.meeting_day,pc.meeting_start_date, pc.meeting_end_date, pc.meeting_start_time, pc.meeting_end_time ,
       pc.legacy_center_number as legacy_number, pc.created_date as formation_date, mp.responsible_user_id, pc.createdby_id, pc.created_date,
       pc.lastmodifiedby_id, pc.lastmodified_date, pc.portfolio_id, pc.city_id, pc.state_province_id, pc.type_id, pc.distance_from_agency, pc.reference_point
from m_portfolio_center pc inner join m_portfolio mp on pc.portfolio_id = mp.id;

--migrate m_center_group to m_group
insert into m_group(display_name,parent_id,office_id,status_enum, level_id,submittedon_userid,activatedon_userid,hierarchy,submittedon_date, activation_date, account_no, meeting_day,meeting_start_date, meeting_end_date, meeting_start_time, meeting_end_time, group_location,latitude,longitude,
                    legacy_number,formation_date,responsible_user_id,createdby_id,created_date,lastmodifiedby_id,lastmodified_date,portfolio_id,city_id,state_province_id,type_id,distance_from_agency,reference_point)

select mcg.name as group_name, mpc.id as parent_id,mpc.office_id,300 as group_status,2 as group_level,
       mcg.createdby_id, mcg.createdby_id as activated_by, '.1.' as hierarchy,
       DATE_FORMAT(mcg.created_date, "%y-%m-%d") as created_at, DATE_FORMAT(mcg.created_date, "%y-%m-%d") as activation_date,
       LPAD(mcg.id, 9, '0') as account_no, mpc.meeting_day, mpc.meeting_start_date, mpc.meeting_end_date,
       mcg.meeting_start_time, mcg.meeting_end_time, mcg.location,mcg.latitude,mcg.longitude,mpc.legacy_number,mcg.formation_date,mcg.responsible_user_id,
       mcg.createdby_id,mcg.created_date,mcg.lastmodifiedby_id,mcg.lastmodified_date,mpc.portfolio_id,mpc.city_id,mpc.state_province_id,mpc.type_id,mpc.distance_from_agency,mpc.reference_point
from m_center_group mcg
         INNER JOIN m_group mpc on mpc.external_id = mcg.portfolio_center_id;

--update account numbers and hirearchy
update m_group set account_no = LPAD(id, 9, '0'), hierarchy = concat('.',id,'.')
