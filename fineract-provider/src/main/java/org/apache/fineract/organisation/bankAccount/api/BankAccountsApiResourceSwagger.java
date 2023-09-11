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
package org.apache.fineract.organisation.bankAccount.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

final class BankAccountsApiResourceSwagger {

    private BankAccountsApiResourceSwagger() {

    }

    @Schema(description = "PostBanksRequest")
    public static final class PostBanksRequest {

        private PostBanksRequest() {

        }

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

    @Schema(description = "PostBanksResponse")
    public static final class PostBanksResponse {

        private PostBanksResponse() {

        }

        @Schema(example = "3")
        public Long bankAccountId;
    }

    @Schema(description = "PutBanksBankIdRequest")
    public static final class PutBanksBankIdRequest {}

    @Schema(description = "PutBanksBankIdResponse")
    public static final class PutBanksBankIdResponse {}

    @Schema(description = "DeleteBanksBankIdResponse")
    public static final class DeleteBanksBankIdResponse {}

    @Schema(description = "GetBankAccountsTemplateResponse")
    public static final class GetBankAccountsTemplateResponse {

        private GetBankAccountsTemplateResponse() {}

        static final class GetBankAccountBankOptions {

            private GetBankAccountBankOptions() {}

            @Schema(example = "1")
            public Integer id;
            @Schema(example = "Bank 1")
            public String name;
            @Schema(example = "B1")
            public String code;
        }

        static final class GetBankAccountAgencyOptions {

            private GetBankAccountAgencyOptions() {}

            @Schema(example = "1")
            public Integer id;
            @Schema(example = "xyz")
            public String name;
        }

        static final class GetBankAccountGLAccountOptions {

            private GetBankAccountGLAccountOptions() {}

            @Schema(example = "4")
            public Integer id;
            @Schema(example = "account overdraft account")
            public String name;
        }

        public Set<BankAccountsApiResourceSwagger.GetBankAccountsTemplateResponse.GetBankAccountBankOptions> bankOptions;
        public Set<BankAccountsApiResourceSwagger.GetBankAccountsTemplateResponse.GetBankAccountAgencyOptions> agencyOptions;
        public Set<BankAccountsApiResourceSwagger.GetBankAccountsTemplateResponse.GetBankAccountGLAccountOptions> glAccountOptions;
    }

}
