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
package org.apache.fineract.organisation.committee.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import org.apache.fineract.organisation.committee.data.CommitteeUserData;

final class CommitteeApiResourceSwagger {

    private CommitteeApiResourceSwagger() {

    }

    @Schema(description = "GetUsersResponse")
    public static final class GetCommitteeResponse {

        private GetCommitteeResponse() {

        }

        @Schema(example = "1")
        public Long id;
        @Schema(example = "mifos")
        public String name;
        public Collection<CommitteeUserData> selectedUsers;

    }

    @Schema(description = "PostCommitteeRequest")
    public static final class PostCommitteeRequest {

        private PostCommitteeRequest() {

        }

        @Schema(example = "id")
        public String id;
        @Schema(example = "users")
        public Collection<String> users;

    }

    @Schema(description = "PostCommitteeResponse")
    public static final class PostCommitteeResponse {

        private PostCommitteeResponse() {

        }

        @Schema(example = "1")
        public Long id;
        @Schema(example = "mifos")
        public String name;
        public Collection<CommitteeUserData> selectedUsers;
    }

    @Schema(description = "PutCommitteesCommitteeIdRequest")
    public static final class PutCommitteesCommitteeIdRequest {

        private PutCommitteesCommitteeIdRequest() {

        }

        @Schema(example = "id")
        public String id;
        @Schema(example = "users")
        public Collection<String> users;
    }

    @Schema(description = "PutCommitteesCommitteeIdResponse")
    public static final class PutCommitteesCommitteeIdResponse {

        private PutCommitteesCommitteeIdResponse() {

        }

        @Schema(example = "1")
        public Long id;
        @Schema(example = "mifos")
        public String name;
        public Collection<CommitteeUserData> selectedUsers;
    }

    @Schema(description = "DeleteCommitteesCommitteeIdResponse")
    public static final class DeleteCommitteesCommitteeIdResponse {

        private DeleteCommitteesCommitteeIdResponse() {

        }

        @Schema(example = "1")
        public Long id;
    }
}
