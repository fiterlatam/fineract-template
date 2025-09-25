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
package org.apache.fineract.custom.portfolio.gac.api;

import io.swagger.v3.oas.annotations.media.Schema;

public class GacApiResourceSwagger {

    private GacApiResourceSwagger() {}

    @Schema(description = "PostAddGacRequest")
    static final class PostAddGacRequest {

        private PostAddGacRequest() {}

        @Schema(example = "1")
        public Long id;
        @Schema(example = "Gac 1")
        public String classification;
        @Schema(example = "1")
        public Integer minimumAgeDays;
        @Schema(example = "3")
        public Integer maximumAgeDays;
        @Schema(example = "1")
        public Integer blockId;
    }

    @Schema(description = "PostAddGacResponse")
    static final class PostAddGacResponse {

        private PostAddGacResponse() {}

        @Schema(example = "1")
        public Long resourceId;
    }

    @Schema(description = "GetGacsGacIdResponse")
    static final class GetGacsGacIdResponse {

        private GetGacsGacIdResponse() {}

        @Schema(example = "1")
        public Long id;
        @Schema(example = "Gac 1")
        public String classification;
        @Schema(example = "1")
        public Integer minimumAgeDays;
        @Schema(example = "3")
        public Integer maximumAgeDays;
        @Schema(example = "1")
        public Integer blockId;
    }
}
