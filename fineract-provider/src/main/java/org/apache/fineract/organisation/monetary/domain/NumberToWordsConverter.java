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
package org.apache.fineract.organisation.monetary.domain;

import java.text.DecimalFormat;

public class NumberToWordsConverter {
    public enum Language {
        ENGLISH, SPANISH
    }

    public static String convertToWords(int number, Language language) {
        switch (language) {
            case SPANISH:
                return SpanishConverter.convertToWords(number);
            default:
                return EnglishConverter.convertToWords(number);
        }
    }

    public static class EnglishConverter {
        private static final String[] tensNames = {"", " ten", " twenty", " thirty", " forty", " fifty", " sixty", " seventy", " eighty", " ninety"};

        private static final String[] numNames = {"", " one", " two", " three", " four", " five", " six", " seven", " eight", " nine", " ten",
                " eleven", " twelve", " thirteen", " fourteen", " fifteen", " sixteen", " seventeen", " eighteen", " nineteen"
        };
        private static String convertLessThanOneThousand(int number) {
            String soFar;

            if (number % 100 < 20){
                soFar = numNames[number % 100];
                number /= 100;
            }
            else {
                soFar = numNames[number % 10];
                number /= 10;

                soFar = tensNames[number % 10] + soFar;
                number /= 10;
            }
            if (number == 0) return soFar;
            return numNames[number] + " hundred" + soFar;
        }


        public static String convertToWords(long number) {
            // 0 to 999 999 999 999
            if (number == 0) { return "zero"; }

            String snumber = Long.toString(number);

            // pad with "0"
            String mask = "000000000000";
            DecimalFormat df = new DecimalFormat(mask);
            snumber = df.format(number);
            int billions = Integer.parseInt(snumber.substring(0,3));
            int millions  = Integer.parseInt(snumber.substring(3,6));
            int hundredThousands = Integer.parseInt(snumber.substring(6,9));
            int thousands = Integer.parseInt(snumber.substring(9,12));

            String tradBillions;
            switch (billions) {
                case 0:
                    tradBillions = "";
                    break;
                case 1 :
                    tradBillions = convertLessThanOneThousand(billions)
                            + " billion ";
                    break;
                default :
                    tradBillions = convertLessThanOneThousand(billions)
                            + " billion ";
            }
            String result =  tradBillions;

            String tradMillions;
            switch (millions) {
                case 0:
                    tradMillions = "";
                    break;
                case 1 :
                    tradMillions = convertLessThanOneThousand(millions)
                            + " million ";
                    break;
                default :
                    tradMillions = convertLessThanOneThousand(millions)
                            + " million ";
            }
            result =  result + tradMillions;

            String tradHundredThousands;
            switch (hundredThousands) {
                case 0:
                    tradHundredThousands = "";
                    break;
                case 1 :
                    tradHundredThousands = "one thousand ";
                    break;
                default :
                    tradHundredThousands = convertLessThanOneThousand(hundredThousands)
                            + " thousand ";
            }
            result =  result + tradHundredThousands;

            String tradThousand;
            tradThousand = convertLessThanOneThousand(thousands);
            result =  result + tradThousand;

            // remove extra spaces!
            return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
        }
    }

    public static class SpanishConverter {
        private static final String FORMAT_MASK = "000000000000000000000";
        private static final String[] NUM_NAMES = {"", "un", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once", "doce", "trece", "catorce", "quince",
                "diecis\u00E9is", "diecisiete", "dieciocho", "diecinueve", "veinte", "veintiun", "veintid\u00F3s", "veintitr\u00E9s",
                "veinticuatro", "veinticinco", "veintis\u00E9is", "veintisiete", "veintiocho", "veintinueve"};
        private static final String[] TENS_NAMES = {"", "diez", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa", "ciento"};
        private static final String[] HUNDREDS_NAMES = {"", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"};
        private static final String[] POWER_NAMES = {"mil", "mill\u00F3nes", "mil mill\u00F3nes", "bill\u00F3nes", "mil bill\u00F3nes", "trill\u00F3nes"};
        private static final String[] SINGLE_POWER_NAMES = {"un mil", "un mill\u00F3n", "mil mill\u00F3nes", "un bill\u00F3n", "mil bill\u00F3nes", "un trill\u00F3n"};

        protected static String convertLessThanOneThousand(final int _number)
        {
            final StringBuilder ret = new StringBuilder();
            if (_number == 100)  {
                ret.append("cien");
            } else  {
                ret.append(HUNDREDS_NAMES[_number / 100]).append(' ').append(convertLessThanOneHundred(_number % 100));
            }
            return ret.toString();
        }


        protected static String convertLessThanOneHundred(final int _number)
        {
            final StringBuilder ret = new StringBuilder();

            if (_number < 30) {
                ret.append(" ").append(NUM_NAMES[_number]);
            } else  {
                ret.append(" ").append(TENS_NAMES[_number / 10]);
                final String num = NUM_NAMES[_number % 10];
                if (!"".equals(num))  {
                    ret.append(" y ").append(NUM_NAMES[_number % 10]);
                }
            }
            return ret.toString();
        }

        protected static String convertPower(final int _number,
                                             final int _power)
        {
            if (_number == 1) {
                return SINGLE_POWER_NAMES[_power];
            }else {
                final StringBuilder ret = new StringBuilder();
                if (_number != 0)  {
                    ret.append(convertLessThanOneThousand(_number))
                            .append(' ')
                            .append(getPowerNames()[_power]);
                }
                return ret.toString();
            }
        }

        protected String[] getNumNames()
        {
            return NUM_NAMES;
        }

        protected String[] getTensNames()
        {
            return TENS_NAMES;
        }

        protected static String[] getPowerNames()
        {
            return POWER_NAMES;
        }

        protected static String getZero()
        {
            return "cero";
        }

        protected static String getMinus()
        {
            return "menos";
        }

        public static String convertToWords(final long _number)
        {
            final StringBuilder ret = new StringBuilder();
            if (_number == 0)  {
                ret.append(getZero());
            } else {
                final long number = _number < 0 ? _number * -1 : _number;

                // pad with "0"
                final DecimalFormat df = new DecimalFormat(FORMAT_MASK);
                final String snumber = df.format(number);

                final int quintillions = Integer.parseInt(snumber.substring(0, 3));
                final int quadrillions = Integer.parseInt(snumber.substring(3, 6));
                final int trillions = Integer.parseInt(snumber.substring(6, 9));
                final int billions = Integer.parseInt(snumber.substring(9, 12));
                final int millions = Integer.parseInt(snumber.substring(12, 15));
                final int thousands = Integer.parseInt(snumber.substring(15, 18));
                final int hundreds = Integer.parseInt(snumber.substring(18, 21));

                final String result = new StringBuilder()
                        .append(convertPower(quintillions, 5)).append(' ')
                        .append(convertPower(quadrillions, 4)).append(' ')
                        .append(convertPower(trillions, 3)).append(' ')
                        .append(convertPower(billions, 2)).append(' ')
                        .append(convertPower(millions, 1)).append(' ')
                        .append(convertPower(thousands, 0)).append(' ')
                        .append(convertLessThanOneThousand(hundreds))
                        .toString();

                // negative number?
                if (_number < 0)  {
                    ret.append(getMinus()).append(' ');
                }

                // remove extra spaces!
                ret.append(result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ").trim());
            }
            ret.toString();
            if (Long.valueOf(_number).toString().endsWith("1") && _number!=11 && _number!=-11) {
                ret.append("o");
            }
            return ret.toString();
        }
    }

    public static void main(String[] args) {
        int number = 1000;

        String wordsInEnglish = convertToWords(number, Language.ENGLISH);
        System.out.println(number + " in English: " + wordsInEnglish);

        String wordsInSpanish = convertToWords(number, Language.SPANISH);
        System.out.println(number + " in Spanish: " + wordsInSpanish);
    }

}
