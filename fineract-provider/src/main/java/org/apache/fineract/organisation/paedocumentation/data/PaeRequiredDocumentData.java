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
package org.apache.fineract.organisation.paedocumentation.data;

public class PaeRequiredDocumentData {

    private final Long id;
    private final Long categoryId;
    private final String documentName;
    private final String description;
    private final String acceptedFormat;
    private final Boolean required;

    public PaeRequiredDocumentData(Long id, Long categoryId, String documentName, String description, String acceptedFormat,
            Boolean required) {
        this.id = id;
        this.categoryId = categoryId;
        this.documentName = documentName;
        this.description = description;
        this.acceptedFormat = acceptedFormat;
        this.required = required;
    }

    public Long getId() {
        return id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getDescription() {
        return description;
    }

    public String getAcceptedFormat() {
        return acceptedFormat;
    }

    public Boolean getRequired() {
        return required;
    }
}
