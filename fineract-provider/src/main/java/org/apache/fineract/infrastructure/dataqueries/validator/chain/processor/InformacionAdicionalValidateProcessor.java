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

package org.apache.fineract.infrastructure.dataqueries.validator.chain.processor;

import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.dataqueries.validator.chain.CustomFieldValidationProcessor;
import org.apache.fineract.infrastructure.dataqueries.validator.data.DataTableMetaData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.tika.utils.StringUtils;

@Slf4j
public class InformacionAdicionalValidateProcessor extends CustomFieldValidationProcessor {

    public static final String STRING_DATATABLE_INFORMACION_ADICIONAL = "Informacion Adicional";
    public static final String STRING_PARAM_VALIDACION_MANUAL = "validacion_manual";
    public static final String STRING_PARAM_NOTIFICACION_BIENVENIDA = "notificacion_bienvenida";

    public InformacionAdicionalValidateProcessor(CustomFieldValidationProcessor nextCustomFieldValidationProcessor) {
        super(nextCustomFieldValidationProcessor);
    }

    @Override
    protected String whoAmI() {
        return STRING_DATATABLE_INFORMACION_ADICIONAL;
    }

  @Override
  public void process(
      Object parentObject,
      DataTableMetaData metData,
      Map<String, String> dataParams,
      Map<String, Object> auxliaryObjects) {

        if (metData.getDataTableName().equalsIgnoreCase(whoAmI()) 
                && parentObject instanceof Loan loanObj
                && loanObj.isApproved() 
                && Boolean.FALSE.equals(loanObj.isDisbursed())
                && dataParams.containsKey(STRING_PARAM_VALIDACION_MANUAL)
                && dataParams.get(STRING_PARAM_VALIDACION_MANUAL).equalsIgnoreCase(Boolean.TRUE.toString())
                && dataParams.containsKey(STRING_PARAM_NOTIFICACION_BIENVENIDA)
                && (dataParams.get(STRING_PARAM_NOTIFICACION_BIENVENIDA).equalsIgnoreCase(Boolean.FALSE.toString())
                        || dataParams.get(STRING_PARAM_NOTIFICACION_BIENVENIDA).equalsIgnoreCase(StringUtils.EMPTY))) {

            log.warn("Informacion Adicional - DataTableMetaData: " + metData.getDataTableName());
        }

        super.process(parentObject, metData, dataParams, auxliaryObjects);
    }
}
