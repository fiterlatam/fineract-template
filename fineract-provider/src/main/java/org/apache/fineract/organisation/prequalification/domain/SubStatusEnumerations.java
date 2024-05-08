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

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public final class SubStatusEnumerations {

    private SubStatusEnumerations() {

    }

    public static EnumOptionData status(final Integer statusId) {
        return status(PrequalificationSubStatus.fromInt(statusId));
    }

    public static EnumOptionData status(final PrequalificationSubStatus status) {
        new EnumOptionData(PrequalificationSubStatus.INVALID.getValue().longValue(), PrequalificationSubStatus.INVALID.getCode(), "INVALID");

        return switch (status) {
            case PENDING -> new EnumOptionData(PrequalificationSubStatus.PENDING.getValue().longValue(),
                    PrequalificationSubStatus.PENDING.getCode(), "PENDING");
            case COMPLETED -> new EnumOptionData(PrequalificationSubStatus.COMPLETED.getValue().longValue(),
                    PrequalificationSubStatus.COMPLETED.getCode(), "COMPLETED");
            case IN_PROGRESS -> new EnumOptionData(PrequalificationSubStatus.IN_PROGRESS.getValue().longValue(),
                    PrequalificationSubStatus.IN_PROGRESS.getCode(), "IN_PROGRESS");
            case BURO_EVIDENCE -> new EnumOptionData(PrequalificationSubStatus.BURO_EVIDENCE.getValue().longValue(),
                    PrequalificationSubStatus.BURO_EVIDENCE.getCode(), "BURO_EVIDENCE");
            case RE_VALIDATE -> new EnumOptionData(PrequalificationSubStatus.RE_VALIDATE.getValue().longValue(),
                    PrequalificationSubStatus.RE_VALIDATE.getCode(), "RE_VALIDATE");
            default -> new EnumOptionData(PrequalificationSubStatus.INVALID.getValue().longValue(), PrequalificationSubStatus.INVALID.getCode(),
                    "INVALID");
        };
    }
}
