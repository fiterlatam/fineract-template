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
package org.apache.fineract.organisation.paedocumentation.domain;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "pae_required_documents")
public class PaeRequiredDocument extends AbstractPersistableCustom {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CodeValue category;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "accepted_format", length = 100)
    private String acceptedFormat;

    @Getter
    @Column(name = "required")
    private Boolean required;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    protected PaeRequiredDocument() {
        // for JPA
    }

    public PaeRequiredDocument(CodeValue category, String documentName, String description, String acceptedFormat, Boolean required,
            Long createdBy, LocalDateTime createdOn) {
        this.category = category;
        this.documentName = documentName;
        this.description = description;
        this.acceptedFormat = acceptedFormat;
        this.required = required;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
    }

    public CodeValue getCategory() {
        return category;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }
}
