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
package org.apache.fineract.organisation.prequalification.domain;

import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.domain.ClientTransactionType;
import org.apache.fineract.portfolio.client.domain.LegalForm;

public final class PreQualificationsMemberEnumerations {

    private PreQualificationsMemberEnumerations() {

    }

    public static EnumOptionData status(final Integer statusId) {
        return status(PrequalificationMemberIndication.fromInt(statusId));
    }

    public static EnumOptionData status(final PrequalificationMemberIndication status) {
        EnumOptionData optionData = new EnumOptionData(PrequalificationMemberIndication.INVALID.getValue().longValue(),
                PrequalificationMemberIndication.INVALID.getCode(), "INVALID");
        switch (status) {
            case INVALID:
                optionData = new EnumOptionData(PrequalificationMemberIndication.INVALID.getValue().longValue(),
                        PrequalificationMemberIndication.INVALID.getCode(), "INVALID");
<<<<<<< HEAD
                break;
            case ACTIVE:
                optionData = new EnumOptionData(PrequalificationMemberIndication.ACTIVE.getValue().longValue(),
                        PrequalificationMemberIndication.ACTIVE.getCode(), "ACTIVE");
                break;
            case INACTIVE:
                optionData = new EnumOptionData(PrequalificationMemberIndication.INACTIVE.getValue().longValue(),
                        PrequalificationMemberIndication.INACTIVE.getCode(), "INACTIVE");
            case NONE:
                optionData = new EnumOptionData(PrequalificationMemberIndication.NONE.getValue().longValue(),
                        PrequalificationMemberIndication.NONE.getCode(), "NONE");
                break;
=======
            break;
            case ACTIVE:
                optionData = new EnumOptionData(PrequalificationMemberIndication.ACTIVE.getValue().longValue(),
                        PrequalificationMemberIndication.ACTIVE.getCode(), "ACTIVE");
            break;
            case INACTIVE:
                optionData = new EnumOptionData(PrequalificationMemberIndication.INACTIVE.getValue().longValue(),
                        PrequalificationMemberIndication.INACTIVE.getCode(), "INACTIVE");
            break;
            case NONE:
                optionData = new EnumOptionData(PrequalificationMemberIndication.NONE.getValue().longValue(),
                        PrequalificationMemberIndication.NONE.getCode(), "NONE");
            break;
>>>>>>> fiter/fb/dev
        }

        return optionData;
    }

    public static EnumOptionData legalForm(final Integer statusId) {
        return legalForm(LegalForm.fromInt(statusId));
    }

    public static EnumOptionData legalForm(final LegalForm legalForm) {
        final EnumOptionData optionData = new EnumOptionData(legalForm.getValue().longValue(), legalForm.getCode(), legalForm.toString());
        return optionData;
    }

    public static List<EnumOptionData> legalForm(final LegalForm[] legalForms) {
        final List<EnumOptionData> optionDatas = new ArrayList<>();
        for (final LegalForm legalForm : legalForms) {
            optionDatas.add(legalForm(legalForm));
        }
        return optionDatas;
    }

    public static EnumOptionData clientTransactionType(final int id) {
        return clientTransactionType(ClientTransactionType.fromInt(id));
    }

    public static EnumOptionData clientTransactionType(final ClientTransactionType clientTransactionType) {
        final EnumOptionData optionData = new EnumOptionData(clientTransactionType.getValue().longValue(), clientTransactionType.getCode(),
                clientTransactionType.toString());
        return optionData;
    }

    public static List<EnumOptionData> clientTransactionType(final ClientTransactionType[] clientTransactionTypes) {
        final List<EnumOptionData> optionDatas = new ArrayList<>();
        for (final ClientTransactionType clientTransaction : clientTransactionTypes) {
            optionDatas.add(clientTransactionType(clientTransaction));
        }
        return optionDatas;
    }

}
