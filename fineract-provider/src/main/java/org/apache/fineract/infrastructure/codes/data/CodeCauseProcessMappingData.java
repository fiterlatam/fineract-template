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
package org.apache.fineract.infrastructure.codes.data;

import java.io.Serializable;

public final class CodeCauseProcessMappingData implements Serializable {

    // pk
    private final String processId;
    private final Integer currencyIntCode;

    // other fields
    private final String operation;
    private final Integer causalCode;
    private final String triggerType;
    private final String logTransactionType;
    private final String destination;
    private final String description;

    public static CodeCauseProcessMappingData instance(final String processId, final Integer currencyIntCode, final String operation,
            final Integer causalCode, final String triggerType, final String logTransactionType, final String destination,
            final String description) {
        return new CodeCauseProcessMappingData(processId, currencyIntCode, operation, causalCode, triggerType, logTransactionType,
                destination, description);
    }

    private CodeCauseProcessMappingData(final String processId, final Integer currencyIntCode, final String operation,
            final Integer causalCode, final String triggerType, final String logTransactionType, final String destination,
            final String description) {
        this.processId = processId;
        this.currencyIntCode = currencyIntCode;
        this.operation = operation;
        this.causalCode = causalCode;
        this.triggerType = triggerType;
        this.logTransactionType = logTransactionType;
        this.destination = destination;
        this.description = description;
    }

    public Integer getCausalCode() {
        return causalCode;
    }

    public String getLogTransactionType() {
        return logTransactionType;
    }
}
