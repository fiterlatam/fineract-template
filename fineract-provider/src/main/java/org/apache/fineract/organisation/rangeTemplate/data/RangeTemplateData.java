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
package org.apache.fineract.organisation.rangeTemplate.data;

/**
 * Immutable data object for range template data.
 */
public class RangeTemplateData {

    private final Long id;

    private final String name;

    private final String code;

    private final Integer startDay;

    private final Integer endDay;

    public RangeTemplateData(Long id, String name, String code, Integer startDay, Integer endDay) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.startDay = startDay;
        this.endDay = endDay;
    }

    public static RangeTemplateData instance(Long id, String name, String code, int startDay, int endDay) {
        return new RangeTemplateData(id, name, code, startDay, endDay);
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Integer getStartDay() {
        return startDay;
    }

    public Integer getEndDay() {
        return endDay;
    }
}
