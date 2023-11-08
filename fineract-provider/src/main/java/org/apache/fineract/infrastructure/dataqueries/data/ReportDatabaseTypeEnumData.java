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

/**
 * Immutable data object represent loan status enumerations.
 */
public class ReportDatabaseTypeEnumData {

    private final Long id;
    private final String code;
    private final String value;

    private final boolean mysql;
    private final boolean postgres;
    private final boolean redshift;

    public ReportDatabaseTypeEnumData(final Long id, final String code, final String value) {
        this.id = id;
        this.code = code;
        this.value = value;

        this.mysql = Long.valueOf(1).equals(this.id);
        this.postgres = Long.valueOf(2).equals(this.id);
        this.redshift = Long.valueOf(3).equals(this.id);
    }

    public Long id() {
        return this.id;
    }

    public String getCode() {
        return this.code;
    }

    public String getValue() {
        return this.value;
    }

    public boolean isMysql() {
        return this.mysql;
    }

    public boolean isRedshift() {
        return this.redshift;
    }

    public boolean isPostgres() {
        return this.postgres;
    }

    public static ReportDatabaseTypeEnumData generateFromId(Long i) {

        if (Long.valueOf(1).equals(i)) {
            return new ReportDatabaseTypeEnumData(i, "mysql", "Mysql");
        } else if (Long.valueOf(2).equals(i)) {
            return new ReportDatabaseTypeEnumData(i, "postgres", "Postgres");
        } else if (Long.valueOf(3).equals(i)) {
            return new ReportDatabaseTypeEnumData(i, "redshift", "Redshift");
        }
        return null;
    }

}
