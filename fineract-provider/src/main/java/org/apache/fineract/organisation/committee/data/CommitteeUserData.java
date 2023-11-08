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

/**
 * Immutable data object for committee users data.
 */
public class CommitteeUserData {

    private final Long userId;

    private final String firstname;

    private final String lastname;

    public static CommitteeUserData instance(Long userId, String firstname, String lastname) {
        return new CommitteeUserData(userId, firstname, lastname);
    }

    public CommitteeUserData(Long userId, String firstname, String lastname) {
        this.userId = userId;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    // TODO: complete
    public static CommitteeUserData template() {
        return new CommitteeUserData(null, null, null);
    }
}
