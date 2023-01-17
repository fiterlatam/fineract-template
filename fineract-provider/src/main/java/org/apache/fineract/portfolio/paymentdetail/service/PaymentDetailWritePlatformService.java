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
package org.apache.fineract.portfolio.paymentdetail.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;

public interface PaymentDetailWritePlatformService {

    PaymentDetail createAndPersistPaymentDetail(JsonCommand command, Map<String, Object> changes);

    PaymentDetail createPaymentDetail(JsonCommand command, Map<String, Object> changes);

    PaymentDetail persistPaymentDetail(PaymentDetail paymentDetail);

    PaymentDetail createAndPersistPaymentDetailForVaultTribe(JsonCommand command, Map<String, Object> changes,
            SavingsAccountTransactionType savingsAccountTransactionType, Integer parentSavingsAccountTransactionId,
            LocalDate transactionDate, Integer parentTransactionPaymentDetailsId);
    Optional<PaymentDetail> getPaymentDetail(Long id);
}
