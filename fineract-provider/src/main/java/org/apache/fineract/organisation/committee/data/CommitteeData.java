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
package org.apache.fineract.organisation.committee.data;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;

/**
 * Immutable data object for committee data.
 */
public class CommitteeData {

    private final Long id;

    private final String name;

    private Collection<CommitteeUserData> selectedUsers;

    // template
    private final Collection<CodeValueData> committees;
    private Collection<CommitteeUserData> availableUsers;

    public static CommitteeData instance(Long id, String name, Collection<CommitteeUserData> selectedUsers) {
        return new CommitteeData(id, name, null, selectedUsers, null);
    }

    public CommitteeData(Long id, String name, Collection<CodeValueData> committees, Collection<CommitteeUserData> selectedUsers,
            Collection<CommitteeUserData> availableUsers) {
        this.id = id;
        this.name = name;
        this.selectedUsers = selectedUsers;
        this.committees = committees;
        this.availableUsers = availableUsers;
    }

    public static CommitteeData template(Collection<CodeValueData> committees, Collection<CommitteeUserData> availableUsers) {
        return new CommitteeData(null, null, committees, new ArrayList<>(), availableUsers);
    }

    public Long getId() {
        return id;
    }

    public void setSelectedUsers(Collection<CommitteeUserData> users) {
        this.selectedUsers = users;
    }

    public void setAvailableUsers(Collection<CommitteeUserData> availableUsers) {
        this.availableUsers = availableUsers;
    }

}
