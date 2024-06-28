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
package org.apache.fineract.organisation.bankcheque.domain;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.organisation.bankcheque.exception.BatchNotFoundException;
import org.apache.fineract.organisation.bankcheque.exception.ChequeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChequeBatchRepositoryWrapper {

    private final BatchJpaRepository batchJpaRepository;
    private final ChequeJpaRepository chequeJpaRepository;

    @Transactional(readOnly = true)
    public Batch findOneBatchWithNotFoundDetection(final Long batchId) {
        return this.batchJpaRepository.findById(batchId).orElseThrow(() -> new BatchNotFoundException(batchId));
    }

    @Transactional(readOnly = true)
    public Cheque findOneChequeWithNotFoundDetection(final Long chequeId) {
        return this.chequeJpaRepository.findById(chequeId).orElseThrow(() -> new ChequeNotFoundException(chequeId));
    }

    public Batch createBatch(final Batch batch) {
        return this.batchJpaRepository.saveAndFlush(batch);
    }

    public Batch updateBatch(final Batch batch) {
        return this.batchJpaRepository.saveAndFlush(batch);
    }

    public void deleteBatch(final Batch batch) {
        this.batchJpaRepository.delete(batch);
    }

    public Cheque createCheque(final Cheque cheque) {
        return this.chequeJpaRepository.saveAndFlush(cheque);
    }

    public Cheque updateCheque(final Cheque cheque) {
        return this.chequeJpaRepository.saveAndFlush(cheque);
    }

    public void deleteCheque(final Cheque cheque) {
        this.chequeJpaRepository.delete(cheque);
    }

}
