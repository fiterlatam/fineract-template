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
package org.apache.fineract.organisation.agency.service;

import org.apache.fineract.organisation.agency.data.AgencyEntityCodeEnumData;
import org.apache.fineract.organisation.agency.data.AgencyTypeEnumData;
import org.apache.fineract.organisation.agency.domain.AgencyEntityCode;
import org.apache.fineract.organisation.agency.domain.AgencyType;

public final class AgencyEnumerations {

    private AgencyEnumerations() {

    }

    public static AgencyTypeEnumData type(final Integer typeId) {
        return type(AgencyType.fromInt(typeId));
    }

    public static AgencyTypeEnumData type(final AgencyType typeId) {
        AgencyTypeEnumData optionData = new AgencyTypeEnumData(AgencyType.INVALID.getValue().longValue(), AgencyType.INVALID.getCode(),
                "Invalid");
        switch (typeId) {
            case INVALID:
                optionData = new AgencyTypeEnumData(AgencyType.INVALID.getValue().longValue(), AgencyType.INVALID.getCode(), "Invalid");
            break;
            case BRANCH:
                optionData = new AgencyTypeEnumData(AgencyType.BRANCH.getValue().longValue(), AgencyType.BRANCH.getCode(), "Branch");
            break;
        }

        return optionData;
    }

    public static AgencyEntityCodeEnumData entityCode(final Integer entityCodeId) {
        return entityCode(AgencyEntityCode.fromInt(entityCodeId));
    }

    public static AgencyEntityCodeEnumData entityCode(final AgencyEntityCode entityCodeId) {
        AgencyEntityCodeEnumData optionData = new AgencyEntityCodeEnumData(AgencyEntityCode.INVALID.getValue().longValue(),
                AgencyEntityCode.INVALID.getCode(), "Invalid");
        switch (entityCodeId) {
            case INVALID:
                optionData = new AgencyEntityCodeEnumData(AgencyEntityCode.INVALID.getValue().longValue(),
                        AgencyEntityCode.INVALID.getCode(), "Invalid");
            break;
            case FRIENDSHIPBRIDGE:
                optionData = new AgencyEntityCodeEnumData(AgencyEntityCode.FRIENDSHIPBRIDGE.getValue().longValue(),
                        AgencyType.BRANCH.getCode(), "FriendshipBridge");
            break;
        }

        return optionData;
    }

}
