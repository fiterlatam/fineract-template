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
package org.apache.fineract.portfolio.client.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.springframework.stereotype.Component;

@Component
public final class ClientIdentifierDocumentValidator {

    // Expresión regular para numero de DPI
    /*
     * ^ inicio de la caracteres | [0-9]{4} siendo cuatro digitos con valores del 0 al 9 | \\s? indica que puede tener
     * un espacio en blanco | [0-9]{5} le siguen cinco digitos con valores del 0 al 9 | \\s? indica que puede tener un
     * espacio en blanco | [0-9]{4} luego tiene cuatro digitos del 0 al 9 | $ final de caracteres
     */
    private static final String PATTERN_CUI = "^[0-9]{4}\\s?[0-9]{5}\\s?[0-9]{4}$";
    // Array with the amount of municipalities per department
    private static final int[] MUNICIPALITY_PER_DEPARTMENT = { /* 01 - Guatemala tiene: */ 17 /* municipios. */,
            /* 02 - El Progreso tiene: */ 8 /* municipios. */, /* 03 - Sacatepéquez tiene: */ 16 /* municipios. */,
            /* 04 - Chimaltenango tiene: */ 16 /* municipios. */, /* 05 - Escuintla tiene: */ 13 /* municipios. */,
            /* 06 - Santa Rosa tiene: */ 14 /* municipios. */, /* 07 - Sololá tiene: */ 19 /* municipios. */,
            /* 08 - Totonicapán tiene: */ 8 /* municipios. */, /* 09 - Quetzaltenango tiene: */ 24 /* municipios. */,
            /* 10 - Suchitepéquez tiene: */ 21 /* municipios. */, /* 11 - Retalhuleu tiene: */ 9 /* municipios. */,
            /* 12 - San Marcos tiene: */ 30 /* municipios. */, /* 13 - Huehuetenango tiene: */ 32 /* municipios. */,
            /* 14 - Quiché tiene: */ 21 /* municipios. */, /* 15 - Baja Verapaz tiene: */ 8 /* municipios. */,
            /* 16 - Alta Verapaz tiene: */ 17 /* municipios. */, /* 17 - Petén tiene: */ 14 /* municipios. */,
            /* 18 - Izabal tiene: */ 5 /* municipios. */, /* 19 - Zacapa tiene: */ 11 /* municipios. */,
            /* 20 - Chiquimula tiene: */ 11 /* municipios. */, /* 21 - Jalapa tiene: */ 7 /* municipios. */,
            /* 22 - Jutiapa tiene: */ 17 /* municipios. */
    };

    public static final String STRING_DOCUMENTTYPE_DPI = "DPI";
    public static final String STRING_DOCUMENTTYPE_NIT = "NIT";
    public static final String STRING_REGEX_VALIDATE_DPI = "[0-9]{13}";
    public static final String STRING_DASH = "-";
    public static final String STRING_POINT = ".";
    public static final String STRING_REGEX_POINT = "\\.";
    public static final String STRING_SPACE = " ";
    public static final String STRING_NIT_TYPE_CF = "CF";
    public static final String STRING_NIT_TYPE_K = "K";
    public static final int INT_ELEVEN = 11;
    public static final int INT_TEN = 10;

    private static DataValidatorBuilder baseDataValidator = null;

    public static void isDPIValid(String dpi, String parameterName) {
        List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        baseDataValidator = new DataValidatorBuilder(dataValidationErrors);

        Pattern p = Pattern.compile(STRING_REGEX_VALIDATE_DPI);

        if (Objects.isNull(dpi) || dpi.isEmpty()) {
            baseDataValidator.reset().parameter(parameterName).value(dpi).notBlank();
        } else if (13 != dpi.length()) {
            baseDataValidator.reset().parameter(parameterName).value(dpi)
                    .failWithCodeNoParameterAddedToErrorCode("identifier.dpi.length.invalid");
        }

        if (dataValidationErrors.isEmpty()) {
            Matcher m = p.matcher(dpi);
            if (!m.matches()) {
                baseDataValidator.reset().parameter(parameterName).value(dpi)
                        .failWithCodeNoParameterAddedToErrorCode("identifier.dpi.not.in.format");
            }
        }

        throwErrorIfValidationIssuesWereFound(dataValidationErrors);

    }

    /**
     * Checks the dpi number base on the guideline of the government, using regex and the data of the departments and
     * municipalities.
     *
     * @param dpiNumber
     */
    public static void checkDPI(String dpiNumber, String parameterName) {
        List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("identifier");

        if (Objects.isNull(dpiNumber) || dpiNumber.isEmpty()) {
            baseDataValidator.reset().parameter(parameterName).value(dpiNumber).notBlank();

        } else {
            Pattern pattern = Pattern.compile(PATTERN_CUI);
            Matcher matcher = pattern.matcher(dpiNumber);
            if (matcher.matches()) {
                dpiNumber = dpiNumber.replace(" ", "");
                int dpi = Integer.parseInt(dpiNumber.substring(0, 9));
                int departmentNumber = Integer.parseInt(dpiNumber.substring(9, 11));
                int municipalityNumber = Integer.parseInt(dpiNumber.substring(11, 13));

                // The number 0 is not a valid number for the municipality or department
                if (dpi == 0 || municipalityNumber == 0 || departmentNumber == 0) {
                    baseDataValidator.reset().parameter(parameterName).value(dpiNumber)
                            .failWithCodeNoParameterAddedToErrorCode("dpi.not.valid");
                } else {
                    if (departmentNumber > MUNICIPALITY_PER_DEPARTMENT.length) {
                        baseDataValidator.reset().parameter(parameterName).value(dpiNumber)
                                .failWithCodeNoParameterAddedToErrorCode("dpi.not.valid.only.22");
                    } else if (municipalityNumber > MUNICIPALITY_PER_DEPARTMENT[departmentNumber - 1]) {
                        baseDataValidator.reset().parameter(parameterName).value(dpiNumber)
                                .failWithCodeNoParameterAddedToErrorCode("dpi.municipality.not.valid");
                    } else {
                        int modulo = parseModuleNumber(dpiNumber.substring(0, 8));
                        int validatorNumber = Integer.parseInt(dpiNumber.substring(8, 9));
                        if (modulo != validatorNumber) {
                            baseDataValidator.reset().parameter(parameterName).value(dpiNumber)
                                    .failWithCodeNoParameterAddedToErrorCode("dpi.digit.not.valid");
                        }
                    }
                }
            } else {
                baseDataValidator.reset().parameter(parameterName).value(dpiNumber)
                        .failWithCodeNoParameterAddedToErrorCode("dpi.chars.not.valid");
            }
        }

        throwErrorIfValidationIssuesWereFound(dataValidationErrors);

    }

    /**
     * It calculates the number that will be used to verify the dpi, this number is in module 11
     *
     * @param cleanDPINumber
     * @return
     */
    private static Integer parseModuleNumber(String cleanDPINumber) {
        int total = 0;

        for (int i = 0; i < cleanDPINumber.length(); i++) {
            total += Integer.parseInt(cleanDPINumber.substring(i, i + 1)) * (i + 2);
        }

        int ret = total % 11;
        return (ret >= 10 ? 0 : ret);
    }

    public static boolean isNITValid(String nit, String parameterName) {
        List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        baseDataValidator = new DataValidatorBuilder(dataValidationErrors);
        ;

        if (Objects.nonNull(nit)) {

            // Remove dash
            if (nit.contains(STRING_DASH)) {
                nit = nit.replaceAll(STRING_DASH, StringUtils.EMPTY);
            } else {
                // Didin´t found the dash to know which is the digit
                baseDataValidator.reset().parameter(parameterName).value(nit)
                        .failWithCodeNoParameterAddedToErrorCode("identifier.nit.dash.not.found");
            }

            // Remove point
            if (nit.contains(STRING_POINT)) {
                nit = nit.replaceAll(STRING_REGEX_POINT, StringUtils.EMPTY);
            }

            if (nit.length() < 2) {
                baseDataValidator.reset().parameter(parameterName).value(nit)
                        .failWithCodeNoParameterAddedToErrorCode("identifier.nit.minchar.2");
            }

            // Remove spaces
            nit = nit.trim().replaceAll(STRING_SPACE, StringUtils.EMPTY);

            // Leading zeros
            nit = StringUtils.leftPad(nit, INT_ELEVEN, "0");

            // Upper case
            nit = nit.toUpperCase();

            if (nit.contains(STRING_NIT_TYPE_CF)) {
                return true;
            }

            // Is numeric
            String cleanedNIT = nit.replaceAll(STRING_NIT_TYPE_CF, StringUtils.EMPTY).replaceAll(STRING_NIT_TYPE_K, StringUtils.EMPTY);
            try {
                Long.parseLong(cleanedNIT);
            } catch (NumberFormatException e) {
                baseDataValidator.reset().parameter(parameterName).value(nit)
                        .failWithCodeNoParameterAddedToErrorCode("identifier.nit.invalid");
            }

            if (dataValidationErrors.isEmpty()) {
                // Start validation
                int finalValue = (nit.contains(STRING_NIT_TYPE_K) ? INT_TEN : Integer.parseInt(nit.substring(nit.length() - 1)));

                int totalValue = 0;
                int digitWeight = nit.length();

                // Calculates digit
                for (char currChar : nit.toCharArray()) {
                    // Do not compute the digit
                    if (digitWeight == 1) {
                        break;
                    }

                    totalValue += Integer.parseInt(currChar + StringUtils.EMPTY) * digitWeight;
                    digitWeight--;
                }

                // Compare calculated digit against provided ont
                if (((INT_ELEVEN - (totalValue % INT_ELEVEN)) % INT_ELEVEN) != finalValue) {
                    baseDataValidator.reset().parameter(parameterName).value(nit)
                            .failWithCodeNoParameterAddedToErrorCode("identifier.nit.digit.invalid");
                }
            }

            throwErrorIfValidationIssuesWereFound(dataValidationErrors);

            return true;
        }

        return false;
    }

    private static void throwErrorIfValidationIssuesWereFound(List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
