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
package org.apache.fineract.infrastructure.dataqueries.domain;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Getter
@Table(name = "c_promissory_note_template")
public class PromissoryNoteTemplate extends AbstractAuditableCustom {

    @Column(name = "promissory_number")
    private Long promissoryNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "title")
    private String title;

    @Column(name = "block_one")
    private String blockOne;

    @Column(name = "block_two")
    private String blockTwo;

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(8);

        String paramName = "blockOne";
        if (command.isChangeInStringParameterNamed(paramName, this.blockOne)) {
            final String newValue = command.stringValueOfParameterNamed(paramName);
            actualChanges.put(paramName, newValue);
            this.blockOne = StringUtils.defaultIfEmpty(newValue, null);
        }
        paramName = "blockTwo";
        if (command.isChangeInStringParameterNamed(paramName, this.blockTwo)) {
            final String newValue = command.stringValueOfParameterNamed(paramName);
            actualChanges.put(paramName, newValue);
            this.blockTwo = StringUtils.defaultIfEmpty(newValue, null);
        }
        paramName = "name";
        if (command.isChangeInStringParameterNamed(paramName, this.name)) {
            final String newValue = command.stringValueOfParameterNamed(paramName);
            actualChanges.put(paramName, newValue);
            this.name = StringUtils.defaultIfEmpty(newValue, null);
        }
        paramName = "title";
        if (command.isChangeInStringParameterNamed(paramName, this.title)) {
            final String newValue = command.stringValueOfParameterNamed(paramName);
            actualChanges.put(paramName, newValue);
            this.title = StringUtils.defaultIfEmpty(newValue, null);
        }

        return actualChanges;
    }
}
