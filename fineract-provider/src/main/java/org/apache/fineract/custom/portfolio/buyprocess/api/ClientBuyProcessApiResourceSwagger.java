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
package org.apache.fineract.custom.portfolio.buyprocess.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public class ClientBuyProcessApiResourceSwagger {

    public ClientBuyProcessApiResourceSwagger() {}

    @Schema(description = "PostClientBuyProcessRequest")
    public static final class PostClientBuyProcessRequest {

        public PostClientBuyProcessRequest() {}

        @Schema(example = "001")
        public String clientDocumentId;
        @Schema(example = "001")
        public String pointOfSalesCode;
        @Schema(example = "1")
        public Long productId;
        @Schema(example = "0")
        public Integer interestRatePoints;
        @Schema(example = "1")
        public Long creditId;
        @Schema(example = "25/06/2024")
        public String requestedDate;
        @Schema(example = "1000")
        public BigDecimal amount;
        @Schema(example = "3")
        public Integer term;
        @Schema(example = "1ae8d4db830eed577c6023998337d0hags546f1a3ba08e5df1ef0d1673431a3")
        public String channelHash;
        @Schema(example = "dd/MM/yyyy")
        public String dateFormat;
        @Schema(example = "es")
        public String locale;
        @Schema(example = "0")
        public Long codigoSeguro;
        @Schema(example = "0")
        public Long cedulaSeguroVoluntario;

    }

}
