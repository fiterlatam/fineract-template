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
package org.apache.fineract.infrastructure.dataqueries.data;

import java.io.Serializable;

/**
 * Immutable data object representing a possible value for a given resultset column.
 */
public class ResultsetColumnValueData implements Serializable {

    private final int id;
    private final String value;
    @SuppressWarnings("unused")
    private final Integer score;

    private final Integer parentId;

    public ResultsetColumnValueData(final Integer id, final String value, final Integer score, final Integer parentId) {
        this.id = id;
        this.value = value;
        this.score = score;
        this.parentId = parentId;
    }

    public boolean matches(final String match) {
        return match.equalsIgnoreCase(this.value);
    }

    public boolean codeMatches(final Integer match) {
        return match.intValue() == this.id;
    }
}
