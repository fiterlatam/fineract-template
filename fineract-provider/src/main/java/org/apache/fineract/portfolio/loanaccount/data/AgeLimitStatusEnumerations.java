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
package org.apache.fineract.portfolio.loanaccount.data;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public final class AgeLimitStatusEnumerations {

    private AgeLimitStatusEnumerations() {

    }

    public static EnumOptionData status(final Integer statusId) {
        return status(AgeLimitStatus.fromInt(statusId));
    }

    public static EnumOptionData status(final AgeLimitStatus status) {
        EnumOptionData optionData = new EnumOptionData(AgeLimitStatus.INVALID.getValue().longValue(), AgeLimitStatus.INVALID.getCode(),
                "INVALID");
        switch (status) {
            case INVALID:
                optionData = new EnumOptionData(AgeLimitStatus.INVALID.getValue().longValue(), AgeLimitStatus.INVALID.getCode(), "INVALID");
            break;
            case WARNING:
                optionData = new EnumOptionData(AgeLimitStatus.WARNING.getValue().longValue(), AgeLimitStatus.WARNING.getCode(), "WARNING");
            break;
            case BLOCK:
                optionData = new EnumOptionData(AgeLimitStatus.BLOCK.getValue().longValue(), AgeLimitStatus.BLOCK.getCode(), "BLOCK");
            case CONTINUE:
                optionData = new EnumOptionData(AgeLimitStatus.CONTINUE.getValue().longValue(), AgeLimitStatus.CONTINUE.getCode(),
                        "CONTINUE");
            break;
        }

        return optionData;
    }
}
