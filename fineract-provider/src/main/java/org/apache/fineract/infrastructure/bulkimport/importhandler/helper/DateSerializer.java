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
package org.apache.fineract.infrastructure.bulkimport.importhandler.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;

public class DateSerializer implements JsonSerializer<LocalDate> {

    private final String dateFormat;
    private final String localeAsString;

    public DateSerializer(String dateFormat) {
        this.dateFormat = dateFormat;
        this.localeAsString = null;
    }

    public DateSerializer(final String dateFormat, final String localeAsString) {
        this.dateFormat = dateFormat;
        this.localeAsString = localeAsString;
    }

    @Override
    public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
        if (this.localeAsString != null && !this.localeAsString.isEmpty()) {
            final Locale locale = JsonParserHelper.localeFromString(this.localeAsString);
            return context.serialize(src.format(DateTimeFormatter.ofPattern(dateFormat, locale)));
        }
        return new JsonPrimitive(src.format(DateTimeFormatter.ofPattern(dateFormat)));
    }
}
