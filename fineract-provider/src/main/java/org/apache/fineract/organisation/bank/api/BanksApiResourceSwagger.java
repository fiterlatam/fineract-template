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
package org.apache.fineract.organisation.bank.api;

import io.swagger.v3.oas.annotations.media.Schema;

final class BanksApiResourceSwagger {

    private BanksApiResourceSwagger() {

    }

    @Schema(description = "PostBanksRequest")
    public static final class PostBanksRequest {

        private PostBanksRequest() {

        }

        @Schema(example = "BankCode")
        public String code;
        @Schema(example = "BankName")
        public String name;

    }

    @Schema(description = "PostBanksResponse")
    public static final class PostBanksResponse {

        private PostBanksResponse() {

        }

        @Schema(example = "3")
        public Long bankId;
        @Schema(example = "BankCode")
        public String code;
        @Schema(example = "BankName")
        public String name;
    }

    @Schema(description = "PutBanksBankIdRequest")
    public static final class PutBanksBankIdRequest {}

    @Schema(description = "PutBanksBankIdResponse")
    public static final class PutBanksBankIdResponse {}

    @Schema(description = "DeleteBanksBankIdResponse")
    public static final class DeleteBanksBankIdResponse {}

}
