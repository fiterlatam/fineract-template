/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.client.domain;

/**
 * @author : mart
 * @project : fineract
 * @created : 9/6/23, Wednesday
 **/
public interface ClientMinimumInfoRelatedDetails {
    Integer getLoanCycle();
    String getGroupNumber();
    String getMaidenName();
    String getOthernames();
    String getGroupMember();
    String getStatusInGroup();
    String getRetirementReason();
    String getCivilStatus();
    String getEducationLevel();
    String getEthinicity();
    String getNationality();
    String getLanguages();
    String getEconomicSector();
    String getEconomicActivity();
    String getFamilyReference();
}
