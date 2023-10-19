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

public final class PreQualificationsEnumerations {

    private PreQualificationsEnumerations() {

    }

    public static EnumOptionData status(final Integer statusId) {
        return status(PrequalificationStatus.fromInt(statusId));
    }

    public static EnumOptionData status(final PrequalificationStatus status) {
        new EnumOptionData(PrequalificationStatus.INVALID.getValue().longValue(), PrequalificationStatus.INVALID.getCode(), "INVALID");

        return switch (status) {
            case PENDING -> new EnumOptionData(PrequalificationStatus.PENDING.getValue().longValue(),
                    PrequalificationStatus.PENDING.getCode(), "PENDING");
            case APPROVED -> new EnumOptionData(PrequalificationStatus.APPROVED.getValue().longValue(),
                    PrequalificationStatus.APPROVED.getCode(), "APPROVED");
            case REJECTED -> new EnumOptionData(PrequalificationStatus.REJECTED.getValue().longValue(),
                    PrequalificationStatus.REJECTED.getCode(), "REJECTED");
            case BLACKLIST_CHECKED -> new EnumOptionData(PrequalificationStatus.BLACKLIST_CHECKED.getValue().longValue(),
                    PrequalificationStatus.BLACKLIST_CHECKED.getCode(), "BLACKLIST_CHECKED");
            case BLACKLIST_REJECTED -> new EnumOptionData(PrequalificationStatus.BLACKLIST_REJECTED.getValue().longValue(),
                    PrequalificationStatus.BLACKLIST_REJECTED.getCode(), "BLACKLIST_REJECTED");
            case HARD_POLICY_CHECKED -> new EnumOptionData(PrequalificationStatus.HARD_POLICY_CHECKED.getValue().longValue(),
                    PrequalificationStatus.HARD_POLICY_CHECKED.getCode(), "HARD_POLICY_CHECKED");
            case BURO_CHECKED -> new EnumOptionData(PrequalificationStatus.BURO_CHECKED.getValue().longValue(),
                    PrequalificationStatus.BURO_CHECKED.getCode(), "BURO_CHECKED");
            case TIME_EXPIRED -> new EnumOptionData(PrequalificationStatus.TIME_EXPIRED.getValue().longValue(),
                    PrequalificationStatus.TIME_EXPIRED.getCode(), "TIME_EXPIRED");
            case COMPLETED -> new EnumOptionData(PrequalificationStatus.COMPLETED.getValue().longValue(),
                    PrequalificationStatus.COMPLETED.getCode(), "COMPLETED");
            case CONSENT_ADDED -> new EnumOptionData(PrequalificationStatus.CONSENT_ADDED.getValue().longValue(),
                    PrequalificationStatus.CONSENT_ADDED.getCode(), "CONSENT_ADDED");
            case AGENCY_LEAD_PENDING_APPROVAL -> new EnumOptionData(
                    PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL.getValue().longValue(),
                    PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL.getCode(), "AGENCY_LEAD_PENDING_APPROVAL");
            case PREQUALIFICATION_UPDATE_REQUESTED -> new EnumOptionData(
                    PrequalificationStatus.PREQUALIFICATION_UPDATE_REQUESTED.getValue().longValue(),
                    PrequalificationStatus.PREQUALIFICATION_UPDATE_REQUESTED.getCode(), "PREQUALIFICATION_UPDATE_REQUESTED");
            case AGENCY_LEAD_PENDING_APPROVAL_WITH_EXCEPTIONS -> new EnumOptionData(
                    PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL_WITH_EXCEPTIONS.getValue().longValue(),
                    PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL_WITH_EXCEPTIONS.getCode(),
                    "AGENCY_LEAD_PENDING_APPROVAL_WITH_EXCEPTIONS");
            case ANALYSIS_UNIT_PENDING_APPROVAL -> new EnumOptionData(
                    PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL.getValue().longValue(),
                    PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL.getCode(), "ANALYSIS_UNIT_PENDING_APPROVAL");
            case ANALYSIS_UNIT_PENDING_APPROVAL_WITH_EXCEPTIONS -> new EnumOptionData(
                    PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL_WITH_EXCEPTIONS.getValue().longValue(),
                    PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL_WITH_EXCEPTIONS.getCode(),
                    "ANALYSIS_UNIT_PENDING_APPROVAL_WITH_EXCEPTIONS");
            default -> new EnumOptionData(PrequalificationStatus.INVALID.getValue().longValue(), PrequalificationStatus.INVALID.getCode(),
                    "INVALID");
        };
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

    public static EnumOptionData validationColor(final CheckValidationColor checkValidationColor) {
        return new EnumOptionData(checkValidationColor.getValue().longValue(), checkValidationColor.getCode(),
                checkValidationColor.name().toLowerCase());
    }

    public static List<EnumOptionData> clientTransactionType(final ClientTransactionType[] clientTransactionTypes) {
        final List<EnumOptionData> optionDatas = new ArrayList<>();
        for (final ClientTransactionType clientTransaction : clientTransactionTypes) {
            optionDatas.add(clientTransactionType(clientTransaction));
        }
        return optionDatas;
    }

    public static EnumOptionData prequalificationType(final Integer prequalificationTypeId) {
        return prequalificationType(PrequalificationType.fromInt(prequalificationTypeId));
    }

    public static EnumOptionData prequalificationType(final PrequalificationType prequalificationType) {
        final EnumOptionData optionData = new EnumOptionData(prequalificationType.getValue().longValue(), prequalificationType.getCode(),
                prequalificationType.toString());
        return optionData;
    }

}
