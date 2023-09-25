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
package org.apache.fineract.organisation.bankcheque.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Collection;

public class BankChequeApiSwagger {

    private BankChequeApiSwagger() {}

    @Schema(description = "PostChequeBatchRequest")
    public static final class PostChequeBatchRequest {

        private PostChequeBatchRequest() {}

        @Schema(example = "10111")
        public Long bankAccNo;
        @Schema(example = "10300")
        public Long agencyId;
        @Schema(example = "500")
        public Long from;
        @Schema(example = "600")
        public Long to;
    }

    @Schema(description = "PostChequeBatchResponse")
    public static final class PostChequeBatchResponse {

        private PostChequeBatchResponse() {}

        @Schema(example = "2")
        public Long batchId;
        @Schema(example = "2")
        public Integer resourceId;
    }

    @Schema(description = "GetChequeBatchResponse")
    public static final class GetChequeBatchResponse {

        private GetChequeBatchResponse() {}

        static final class GetBankAccount {

            private GetBankAccount() {}

            @Schema(example = "AccountNumber")
            public Long accountNumber;
            @Schema(example = "AgencyID")
            public Long agencyId;
            @Schema(example = "BankId")
            public Long bankId;
            @Schema(example = "GLAccountId")
            public Long glAccountId;
            @Schema(example = "Description")
            public String description;
        }

        static final class GetBankCheque {

            @Schema(example = "77")
            public Long id;
            @Schema(example = "91009")
            public Long batchId;
            @Schema(example = "200")
            public Long chequeNo;
            @Schema(example = "Description")
            public String description;
        }

        @Schema(example = "2")
        public Long id;
        @Schema(example = "2")
        public Long batchNo;
        public GetBankAccount bankAccount;
        @Schema(example = "2")
        public Long from;
        @Schema(example = "500")
        public Long to;
        @Schema(example = "[2013, 1, 1]")
        public LocalDate createdDate;
        public Collection<GetBankCheque> cheques;
    }
}
