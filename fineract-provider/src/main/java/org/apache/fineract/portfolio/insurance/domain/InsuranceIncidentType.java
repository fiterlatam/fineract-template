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
package org.apache.fineract.portfolio.insurance.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public enum InsuranceIncidentType {

    INVALID(0, "labels.inputs.insurance.incident.invalid", "invalid"), //
    DEFINITIVE_CANCELLATION_DEFAULT(1, "labels.inputs.insurance.incident.definitive.default", "Cancelación definitiva por mora"), //
    DEFINITIVE_VOLUNTARY_CANCELLATION(2, "labels.inputs.insurance.incident.definitive.voluntary.cancellation",
            "Cancelación voluntaria definitiva"), //
    DEFINITIVE_FINAL_CANCELLATION(3, "labels.inputs.insurance.incident.final.advance.payment.cancellation",
            "Cancelación definitiva por cancelación del crédito"), //
    FINAL_GUARANTEE_CLAIM_CANCELLATION(4, "labels.inputs.insurance.incident.final.guarantee.claim.cancellation",
            "Cancelación definitiva por reclamación avaladora"), //
    NOVEDAD_REDIFERIDO(5, "labels.inputs.insurance.incident.novedad.rediferido", "Novedad rediferido"), // Renamed
                                                                                                        // notification
                                                                                                        // type from
                                                                                                        // FINAL_REFINANCED_CANCELLATION
                                                                                                        // to
                                                                                                        // NOVEDAD_REDIFERIDO
    BAD_SALE_CANCELLATION(6, "labels.inputs.insurance.incident.bad.sale.cancellation", "Cancelación por mala venta"), //
    PORTFOLIO_WRITE_OFF_CANCELLATION(7, "labels.inputs.insurance.incident.portfolio.write.off.cancellation",
            "Cancelación por castigo de cartera"), //
    TEMPORARY_SUSPENSION_DUE_TO_DEFAULT(8, "labels.inputs.insurance.incident.temporary.suspension.default", "Suspensión temporal por mora"), //
    PERMANENT_CANCELLATION_DUE_TO_MAX_AGE(9, "labels.inputs.insurance.incident.permanent.cancellation.max.age",
            "Cancelación definitiva por edad máxima de permanencia"), //
    DEATH_CANCELLATION(10, "labels.inputs.insurance.incident.death.cancellation", "Cancelación definitiva por fallecimiento"), //
    SUSPENSION_REMOVED(11, "labels.inputs.insurance.incident.removed.suspension", "Salida de suspensión"), //
    DEFINITIVE_FINAL_INVALIDATION(12, "labels.inputs.insurance.incident.final.annulment",
            "Anulación definitiva por invalidez del contrato"), //
    DEFINITIVE_RESTRUCTURING_CANCELLATION(13, "labels.inputs.insurance.incident.definitive.restructuring.cancellation",
            "Cancelación definitiva por reestructuración"); //

    private final Integer value;
    private final String code;
    private final String readableName;

    InsuranceIncidentType(final Integer value, final String code, final String readableName) {
        this.value = value;
        this.code = code;
        this.readableName = readableName;
    }

    public static List<EnumOptionData> getValuesAsEnumOptionDataList() {
        List<EnumOptionData> list = new ArrayList<>(
                Arrays.stream(values()).map(v -> new EnumOptionData((long) (v.getValue()), v.name(), v.getCode())).toList());
        // Remove FEE enum from the list as it is split into FEES, AVAL, MANDATORY_INSURANCE and VOLUNTARY_INSURANCE.
        list.removeIf(x -> x.getCode().equals(INVALID.name()));
        return list;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static InsuranceIncidentType fromInt(final Integer value) {
        switch (value) {
            case 1:
                return InsuranceIncidentType.DEFINITIVE_CANCELLATION_DEFAULT;
            case 2:
                return InsuranceIncidentType.DEFINITIVE_VOLUNTARY_CANCELLATION;
            case 3:
                return InsuranceIncidentType.DEFINITIVE_FINAL_CANCELLATION;
            case 4:
                return InsuranceIncidentType.FINAL_GUARANTEE_CLAIM_CANCELLATION;
            case 5:
                return InsuranceIncidentType.NOVEDAD_REDIFERIDO;
            case 6:
                return InsuranceIncidentType.BAD_SALE_CANCELLATION;
            case 7:
                return InsuranceIncidentType.PORTFOLIO_WRITE_OFF_CANCELLATION;
            case 8:
                return InsuranceIncidentType.TEMPORARY_SUSPENSION_DUE_TO_DEFAULT;
            case 9:
                return InsuranceIncidentType.PERMANENT_CANCELLATION_DUE_TO_MAX_AGE;
            case 10:
                return InsuranceIncidentType.DEATH_CANCELLATION;
            case 11:
                return InsuranceIncidentType.SUSPENSION_REMOVED;
            case 12:
                return InsuranceIncidentType.DEFINITIVE_FINAL_INVALIDATION;
            case 13:
                return InsuranceIncidentType.DEFINITIVE_RESTRUCTURING_CANCELLATION;
            default:
                return InsuranceIncidentType.INVALID;
        }
    }
}
