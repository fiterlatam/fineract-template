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
package org.apache.fineract.organisation.bank.data;

/**
 * Immutable data object for bank data.
 */
public class BankData {

    private final Long id;

    private final String code;

    private final String name;

    // template

    public static BankData instance(Long id, String code, String name) {
        return new BankData(id, code, name);
    }

    public BankData(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }
    //TODO: complete
    public static BankData template() {
        return new BankData(null, null, null);
    }
}
