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
package org.apache.fineract.organisation.paedocumentation.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.apache.fineract.organisation.paedocumentation.data.PaeRequiredDocumentData;

final class PaeRequiredDocumentationApiResourceSwagger {

    private PaeRequiredDocumentationApiResourceSwagger() {

    }

    @Schema(description = "PaeRequiredDocumentsResponse")
    static final class GetPaeRequiredDocumentsResponse {

        private GetPaeRequiredDocumentsResponse() {}

        @Schema(example = "1")
        public Long id;

        @Schema(example = "1")
        public Long categoryId;

        @Schema(example = "ID Document")
        public String documentName;

        @Schema(example = "Copy of national ID")
        public String description;

        @Schema(example = "PDF,JPEG")
        public String acceptedFormat;
    }

    @Schema(description = "PostPaeRequiredDocumentRequest")
    static final class PostPaeRequiredDocumentRequest {

        private PostPaeRequiredDocumentRequest() {}

        @Schema(example = "1")
        public Long categoryId;

        @Schema(example = "ID Document")
        public String documentName;

        @Schema(example = "Copy of national ID")
        public String description;

        @Schema(example = "PDF,JPEG")
        public String acceptedFormat;
    }

    @Schema(description = "PostPaeRequiredDocumentResponse")
    static final class PostPaeRequiredDocumentResponse {

        private PostPaeRequiredDocumentResponse() {}

        @Schema(example = "1")
        public Long resourceId;
    }

    @Schema(description = "GetPaeRequiredDocumentsListResponse")
    static final class GetPaeRequiredDocumentsListResponse {

        private GetPaeRequiredDocumentsListResponse() {}

        public List<PaeRequiredDocumentData> documents;
    }

    @Schema(description = "DeletePaeRequiredDocumentResponse")
    static final class DeletePaeRequiredDocumentResponse {

        private DeletePaeRequiredDocumentResponse() {}

        @Schema(example = "1")
        public Long resourceId;
    }
}
