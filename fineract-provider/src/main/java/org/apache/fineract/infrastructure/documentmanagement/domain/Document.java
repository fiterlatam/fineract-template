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
package org.apache.fineract.infrastructure.documentmanagement.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.documentmanagement.command.DocumentCommand;

import java.time.LocalDateTime;

@Entity
@Table(name = "m_document")
public class Document extends AbstractPersistableCustom {

    @Column(name = "parent_entity_type", length = 50)
    private String parentEntityType;

    @Column(name = "parent_entity_id", length = 1000)
    private Long parentEntityId;

    @Column(name = "name", length = 250)
    private String name;

    @Column(name = "file_name", length = 250)
    private String fileName;

    @Column(name = "size")
    private Long size;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "storage_type_enum")
    private Integer storageType;

    @Column(name = "document_type")
    private Long documentType;

    @Column(name = "document_purpose")
    private Long documentPurpose;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    public Document() {}

    public static Document createNew(final String parentEntityType, final Long parentEntityId, final String name, final String fileName,
                                     final Long size, final String type, final String description, final String location, final StorageType storageType, String documentType, String documentPurpose, LocalDateTime dateCreated) {
        return new Document(parentEntityType, parentEntityId, name, fileName, size, type, description, location, storageType, documentType, documentPurpose, dateCreated);
    }

    private Document(final String parentEntityType, final Long parentEntityId, final String name, final String fileName, final Long size,
                     final String type, final String description, final String location, final StorageType storageType, String documentType, String documentPurpose, LocalDateTime dateCreated) {
        this.parentEntityType = StringUtils.defaultIfEmpty(parentEntityType, null);
        this.parentEntityId = parentEntityId;
        this.name = StringUtils.defaultIfEmpty(name, null);
        this.fileName = StringUtils.defaultIfEmpty(fileName, null);
        this.size = size;
        this.type = StringUtils.defaultIfEmpty(type, null);
        this.description = StringUtils.defaultIfEmpty(description, null);
        this.location = StringUtils.defaultIfEmpty(location, null);
        this.storageType = storageType.getValue();
        this.documentType = documentType != null ? Long.valueOf(documentType) : null;
        this.documentPurpose = documentPurpose != null ? Long.valueOf(documentPurpose) : null;
        this.dateCreated = dateCreated;
    }

    public void update(final DocumentCommand command) {
        if (command.isDescriptionChanged()) {
            this.description = command.getDescription();
        }
        if (command.isFileNameChanged()) {
            this.fileName = command.getFileName();
        }
        if (command.isFileTypeChanged()) {
            this.type = command.getType();
        }
        if (command.isLocationChanged()) {
            this.location = command.getLocation();
        }
        if (command.isNameChanged()) {
            this.name = command.getName();
        }
        if (command.isSizeChanged()) {
            this.size = command.getSize();
        }
    }

    public String getParentEntityType() {
        return this.parentEntityType;
    }

    public void setParentEntityType(final String parentEntityType) {
        this.parentEntityType = parentEntityType;
    }

    public Long getParentEntityId() {
        return this.parentEntityId;
    }

    public void setParentEntityId(final Long parentEntityId) {
        this.parentEntityId = parentEntityId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public Long getSize() {
        return this.size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    public String getType() {
        return this.type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public StorageType storageType() {
        return StorageType.fromInt(this.storageType);
    }

    public Long getDocumentType() {
        return documentType;
    }

    public void setDocumentType(Long documentType) {
        this.documentType = documentType;
    }

    public Long getDocumentPurpose() {
        return documentPurpose;
    }

    public void setDocumentPurpose(Long documentPurpose) {
        this.documentPurpose = documentPurpose;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
}
