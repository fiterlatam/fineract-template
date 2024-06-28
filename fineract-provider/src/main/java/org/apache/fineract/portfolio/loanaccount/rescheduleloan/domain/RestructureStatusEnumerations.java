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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public final class RestructureStatusEnumerations {

    private RestructureStatusEnumerations() {

    }

    public static EnumOptionData status(final Integer statusId) {
        return status(RestructureCreditStatus.fromInt(statusId));
    }

    public static EnumOptionData status(final RestructureCreditStatus status) {
        new EnumOptionData(RestructureCreditStatus.INVALID.getValue().longValue(), RestructureCreditStatus.INVALID.getCode(), "INVALID");

        return switch (status) {
            case PENDING -> new EnumOptionData(RestructureCreditStatus.PENDING.getValue().longValue(),
                    RestructureCreditStatus.PENDING.getCode(), "PENDING");
            case APPROVED -> new EnumOptionData(RestructureCreditStatus.APPROVED.getValue().longValue(),
                    RestructureCreditStatus.APPROVED.getCode(), "APPROVED");
            case REJECTED -> new EnumOptionData(RestructureCreditStatus.REJECTED.getValue().longValue(),
                    RestructureCreditStatus.REJECTED.getCode(), "REJECTED");
            default -> new EnumOptionData(RestructureCreditStatus.INVALID.getValue().longValue(), RestructureCreditStatus.INVALID.getCode(),
                    "INVALID");
        };
    }
}
