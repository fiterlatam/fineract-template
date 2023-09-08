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
package org.apache.fineract.portfolio.client.data;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;

/**
 * Immutable data object representing client data.
 */
@SuppressWarnings("unused")
@Data
public final class ClientContactInformationData implements Serializable {

    private final String area;
    private final String housingType;
    private final String yearsOfResidence;
    private final String department;
    private final String municipality;
    private final String village;
    private final String referenceHousingData;
    private final String street;
    private final String avenue;
    private final String houseNumber;
    private final String colony;
    private final String sector;
    private final String batch;
    private final String square;
    private final String zone;
    private final String lightMeterNumber;
    private final String homePhone;
    private final Integer communityYears;
    private final String streetNumber;
    private List<CodeValueData> publicServiceTypes;

    public ClientContactInformationData(String area, String housingType, String yearsOfResidence, String department, String municipality,
            String village, String referenceHousingData, String street, String avenue, String houseNumber, String colony, String sector,
            String batch, String square, String zone, String lightMeterNumber, String homePhone, final Integer communityYears,
            String streetNumber) {
        this.area = area;
        this.housingType = housingType;
        this.yearsOfResidence = yearsOfResidence;
        this.department = department;
        this.municipality = municipality;
        this.village = village;
        this.referenceHousingData = referenceHousingData;
        this.street = street;
        this.avenue = avenue;
        this.houseNumber = houseNumber;
        this.colony = colony;
        this.sector = sector;
        this.batch = batch;
        this.square = square;
        this.zone = zone;
        this.lightMeterNumber = lightMeterNumber;
        this.homePhone = homePhone;
        this.communityYears = communityYears;
        this.streetNumber = streetNumber;
    }

    public static ClientContactInformationData instance(String area, String housingType, String yearsOfResidence, String department,
            String municipality, String village, String referenceHousingData, String street, String avenue, String houseNumber,
            String colony, String sector, String batch, String square, String zone, String lightMeterNumber, String homePhone,
            final Integer communityYears, String streetNumber) {
        return new ClientContactInformationData(area, housingType, yearsOfResidence, department, municipality, village,
                referenceHousingData, street, avenue, houseNumber, colony, sector, batch, square, zone, lightMeterNumber, homePhone,
                communityYears, streetNumber);
    }
}
