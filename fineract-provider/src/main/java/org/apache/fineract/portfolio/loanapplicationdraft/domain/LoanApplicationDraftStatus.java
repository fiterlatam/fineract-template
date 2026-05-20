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
package org.apache.fineract.portfolio.loanapplicationdraft.domain;

/**
 * Enum representation of loan application draft status.
 */
public enum LoanApplicationDraftStatus {

    INVALID(0, "loanStatusType.in.invalid"), IN_PROGRESS(100, "loanStatusType.in.progress"), //
    SUBMITTED(200, "loanStatusType.submitted"), //
    EXPIRED(300, "loanStatusType.expired"), //
    DELETED(400, "loanStatusType.deleted");

    private final Integer value;
    private final String code;

    public static LoanApplicationDraftStatus fromInt(final Integer statusValue) {

        LoanApplicationDraftStatus enumeration = LoanApplicationDraftStatus.INVALID;
        switch (statusValue) {
            case 100:
                enumeration = LoanApplicationDraftStatus.IN_PROGRESS;
            break;
            case 200:
                enumeration = LoanApplicationDraftStatus.SUBMITTED;
            break;
            case 300:
                enumeration = LoanApplicationDraftStatus.EXPIRED;
            break;
            case 400:
                enumeration = LoanApplicationDraftStatus.DELETED;
            break;
        }
        return enumeration;
    }

    LoanApplicationDraftStatus(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public boolean isInProgress() {
        return this.value.equals(LoanApplicationDraftStatus.IN_PROGRESS.getValue());
    }

    public boolean isInSubmitted() {
        return this.value.equals(LoanApplicationDraftStatus.SUBMITTED.getValue());
    }

}
