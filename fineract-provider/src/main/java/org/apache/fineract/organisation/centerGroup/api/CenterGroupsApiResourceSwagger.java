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
package org.apache.fineract.organisation.centerGroup.api;

import io.swagger.v3.oas.annotations.media.Schema;

final class CenterGroupsApiResourceSwagger {

    private CenterGroupsApiResourceSwagger() {

    }

    @Schema(description = "PostCenterGroupsRequest")
    public static final class PostCenterGroupsRequest {

        private PostCenterGroupsRequest() {

        }

        @Schema(example = "Good Friday")
        public String name;
        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;
        @Schema(example = "en")
        public String locale;
        @Schema(example = "2")
        public Long parentId;

    }

    @Schema(description = "PostCenterGroupsResponse")
    public static final class PostCenterGroupsResponse {

        private PostCenterGroupsResponse() {

        }

        @Schema(example = "3")
        public Long agencyId;
        @Schema(example = "3")
        public Long parentId;
    }

    @Schema(description = "PutCenterGroupsCenterGroupIdRequest")
    public static final class PutCenterGroupsCenterGroupIdRequest {}

    @Schema(description = "PutCenterGroupsCenterGroupIdResponse")
    public static final class PutCenterGroupsCenterGroupIdResponse {}

    @Schema(description = "PutCenterGroupsCenterGroupIdResponse")
    public static final class DeleteCenterGroupsCenterGroupIdResponse {}
}
