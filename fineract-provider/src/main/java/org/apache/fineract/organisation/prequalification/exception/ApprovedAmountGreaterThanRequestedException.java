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
package org.apache.fineract.organisation.prequalification.exception;

import java.math.BigDecimal;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class ApprovedAmountGreaterThanRequestedException extends AbstractPlatformDomainRuleException {

    public ApprovedAmountGreaterThanRequestedException(final String dpi, final BigDecimal approvedAmount,
            final BigDecimal requestedAmount) {
        super("error.msg.approved.amount.greater.than.requested", "Approved Amount " + approvedAmount
                + " cannot be greater than the requested amount " + requestedAmount + " for member with dpi " + dpi, approvedAmount,
                requestedAmount, dpi);
    }

}
