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
package org.apache.fineract.organisation.bankcheque.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.bankcheque.event.BatchChequePrintEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchChequePrintEventListener implements ApplicationListener<BatchChequePrintEvent> {

    private final ChequeWritePlatformService chequeWritePlatformService;

    @Override
    public void onApplicationEvent(final BatchChequePrintEvent event) {
        ThreadLocalContextUtil.init(event.getContext());
        try {
            log.info("Starting async processing for batch cheque request {}", event.getBatchChequeRequestId());
            this.chequeWritePlatformService.processBatchChequeRequestById(event.getBatchChequeRequestId());
        } catch (final Exception e) {
            log.error("Async processing failed for batch cheque request {}", event.getBatchChequeRequestId(), e);
        }
    }
}
