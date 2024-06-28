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
package org.apache.fineract.infrastructure.documentmanagement.command;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Immutable command for creating or updating details of a client identifier.
 */
public class DocumentCommand {

    private final Long id;
    private final String parentEntityType;
    private final Long parentEntityId;
    private final String name;
    private final String description;

    private String fileName;
    private Long size;
    private String type;
    private String location;
    private Integer storageType;
    private String locale;
    private String documentType;
    private String documentPurpose;
    private LocalDateTime dateCreated;

    private final Set<String> modifiedParameters;

    public DocumentCommand(final Set<String> modifiedParameters, final Long id, final String parentEntityType, final Long parentEntityId,
            final String name, final String fileName, final Long size, final String type, final String description, final String location) {
        this.modifiedParameters = modifiedParameters;
        this.id = id;
        this.parentEntityType = parentEntityType;
        this.parentEntityId = parentEntityId;
        this.name = name;
        this.fileName = fileName;
        this.size = size;
        this.type = type;
        this.description = description;
        this.location = location;
    }

    public DocumentCommand(final Set<String> modifiedParameters, final Long id, final String parentEntityType, final Long parentEntityId,
            final String name, final String fileName, final Long size, final String type, final String description, final String location,
            final String locale, final String documentType, final String documentPurpose, final LocalDateTime dateCreated) {
        this.modifiedParameters = modifiedParameters;
        this.id = id;
        this.parentEntityType = parentEntityType;
        this.parentEntityId = parentEntityId;
        this.name = name;
        this.fileName = fileName;
        this.size = size;
        this.type = type;
        this.description = description;
        this.location = location;
        this.locale = locale;
        this.documentType = documentType;
        this.documentPurpose = documentPurpose;
        this.dateCreated = dateCreated;
    }

    public Long getId() {
        return this.id;
    }

    public String getParentEntityType() {
        return this.parentEntityType;
    }

    public Long getParentEntityId() {
        return this.parentEntityId;
    }

    public String getName() {
        return this.name;
    }

    public String getFileName() {
        return this.fileName;
    }

    public Long getSize() {
        return this.size;
    }

    public String getType() {
        return this.type;
    }

    public String getDescription() {
        return this.description;
    }

    public String getLocation() {
        return this.location;
    }

    public Set<String> getModifiedParameters() {
        return this.modifiedParameters;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public Integer getStorageType() {
        return this.storageType;
    }

    public void setStorageType(final Integer storageType) {
        this.storageType = storageType;
    }

    public boolean isNameChanged() {
        return this.modifiedParameters.contains("name");
    }

    public boolean isFileNameChanged() {
        return this.modifiedParameters.contains("fileName");
    }

    public boolean isSizeChanged() {
        return this.modifiedParameters.contains("size");
    }

    public boolean isFileTypeChanged() {
        return this.modifiedParameters.contains("type");
    }

    public boolean isDescriptionChanged() {
        return this.modifiedParameters.contains("description");
    }

    public boolean isLocationChanged() {
        return this.modifiedParameters.contains("location");
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentPurpose() {
        return documentPurpose;
    }

    public void setDocumentPurpose(String documentPurpose) {
        this.documentPurpose = documentPurpose;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
}
