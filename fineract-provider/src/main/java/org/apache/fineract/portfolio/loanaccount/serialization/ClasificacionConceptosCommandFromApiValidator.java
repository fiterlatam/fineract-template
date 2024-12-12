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

package org.apache.fineract.portfolio.loanaccount.serialization;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ClasificacionConceptosCommandFromApiValidator {

    public static final String CONCEPTO = "concepto";
    public static final String MANDATO = "mandato";
    public static final String EXCLUIDO = "excluido";
    public static final String EXENTO = "exento";
    public static final String GRAVADO = "gravado";
    public static final String NORMA = "norma";
    public static final String TARIFA = "tarifa";
    public static final String ID = "id";
    public static final String LOCALE = "locale";
    public static final String DATE_FORMAT = "dateFormat";
    private static final String[] SUPPORTED_PARAMETERS = new String[] { DATE_FORMAT, ID, LOCALE, CONCEPTO, MANDATO, EXCLUIDO, EXENTO,
            GRAVADO, NORMA, TARIFA };
    private final FromJsonHelper fromApiJsonHelper;

    public void validate(String json) {

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();

        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, Arrays.stream(SUPPORTED_PARAMETERS).toList());
    }
}
