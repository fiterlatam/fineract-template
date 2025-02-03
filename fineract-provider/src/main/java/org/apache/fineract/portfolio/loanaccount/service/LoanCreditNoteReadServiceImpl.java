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
package org.apache.fineract.portfolio.loanaccount.service;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.portfolio.loanaccount.data.LoanCreditNoteData;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNote;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCreditNoteRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanCreditNoteNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanCreditNoteReadServiceImpl implements LoanCreditNoteReadService {

    private final LoanCreditNoteRepository loanCreditNoteRepository;

    @Override
    public Collection<LoanCreditNoteData> retrieveAllCreditNotesForLoan(Long loanId) {

        List<LoanCreditNote> creditNotes = loanCreditNoteRepository.findByLoan_Id(loanId);

        return creditNotes.stream().map(LoanCreditNote::toData).toList();
    }

    @Override
    public LoanCreditNoteData retrieveCreditNoteForLoan(Long loanId, Long creditNoteId) {
        LoanCreditNote creditNote = loanCreditNoteRepository.findByLoan_IdAndId(loanId, creditNoteId);
        if (creditNote != null) {
            return creditNote.toData();
        }
        throw new LoanCreditNoteNotFoundException(creditNoteId, loanId);
    }
}
