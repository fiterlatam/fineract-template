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
package org.apache.fineract.organisation.prequalification.domain;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Getter
@Table(name = "m_group_prequalification_relationship")
public class GroupPrequalificationRelationship extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "prequalification_id", nullable = false)
    private PrequalificationGroup prequalificationGroup;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser addedBy;

    public GroupPrequalificationRelationship(AppUser appUser, Group group, PrequalificationGroup prequalificationGroup) {
        this.addedBy = appUser;
        this.group = group;
        this.prequalificationGroup = prequalificationGroup;
    }

    public static GroupPrequalificationRelationship addRelationship(final AppUser appUser, final Group group,
            final PrequalificationGroup prequalificationGroup) {
        return new GroupPrequalificationRelationship(appUser, group, prequalificationGroup);
    }

    protected GroupPrequalificationRelationship() {
        //
    }
}
