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
package org.apache.fineract.portfolio.loanaccount.exception;

import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

/**
 * A {@link RuntimeException} thrown when loan resources are not found.
 */
public class LoanNotFoundException extends AbstractPlatformResourceNotFoundException {

    public LoanNotFoundException(final Long id) {
        super("error.msg.loan.id.invalid",
                new String(("El préstamo " + id + " no existe").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8), id);
    }

    public LoanNotFoundException(Long id, Exception e) {
        super("error.msg.loan.id.invalid",
                new String(("El préstamo " + id + " no existe").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8), id, e);
    }

    public LoanNotFoundException(String accountId) {
        super("error.msg.loan.account.id.invalid", "Loan with account ID " + accountId + " does not exist", accountId);
    }

    public LoanNotFoundException(ExternalId externalId) {
        super("error.msg.loan.external.id.invalid",
                "Loan with external identifier " + ObjectUtils.defaultIfNull(externalId, ExternalId.empty()).getValue() + " does not exist",
                externalId);
    }

    public LoanNotFoundException(ExternalId externalId, Exception e) {
        super("error.msg.loan.external.id.invalid",
                "Loan with external identifier " + ObjectUtils.defaultIfNull(externalId, ExternalId.empty()).getValue() + " does not exist",
                externalId, e);
    }
}
