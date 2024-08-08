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
package org.apache.fineract.portfolio.charge.domain;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.portfolio.charge.enumerator.ChargeCalculationTypeBaseItemsEnum;

@AllArgsConstructor
@Getter
public enum ChargeCalculationType {

    INVALID(0, "00000000000", "invalid"), //
    FLAT_AMOUNT(1, "10000000000", "flat"), //
    PERCENT_OF_AMOUNT(2, "00100000000", "percent.of.amount"), //
    PERCENT_OF_AMOUNT_AND_INTEREST(3, "00110000000", "percent.of.amount.and.interest"), //
    PERCENT_OF_INTEREST(4, "00010000000", "percent.of.interest"), //
    PERCENT_OF_DISBURSEMENT_AMOUNT(5, "01000000000", "percent.of.disbursement.amount"), //
    PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT(6, "00001000000", "percent.of.outstanding.principal"), //
    PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT(7, "00000100000", "percent.of.outstanding.interest"), //

    PERCENT_OF_ANOTHER_CHARGE(8, "00000000010", "percent.of.another.charge"), //
    AMOUNT_FROM_EXTERNAL_CALCULATION(9, "9", "amount.from.external"), //

    IPRIN_IINT_OPRIN_SEGO_ACHG(11, "00111010010",
            "installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_SEGO(12, "00111010000", "installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio"), //
    OPRIN_OINT_SEGO_AVAL_HONO_ACHG(13, "00001111110",
            "outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_AVAL_HONO(14, "01000001100", "disbursedamount.aval.honorarios"), //
    DISB_IPRIN_OPRIN(15, "01101000000", "disbursedamount.installmentprincipal.outstandingprincipal"), //
    FLAT_DISB_OPRIN_SEGO_AVAL_ACHG(16, "11001011010",
            "flat.disbursedamount.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    IINT_SEGO_AVAL_HONO_ACHG(17, "00010011110", "installmentinterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IINT_SEGO_AVAL_HONO(18, "00010011100", "installmentinterest.seguroobrigatorio.aval.honorarios"), //
    DISB_AVAL_HONO_ACHG(19, "01000001110", "disbursedamount.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_HONO(20, "10111100100",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.honorarios"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL(21, "10111111000",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_HONO_ACHG(22, "10111100110",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OINT_AVAL(23, "00110101000", "installmentprincipal.installmentinterest.outstandinginterest.aval"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_ACHG(24, "10111100010",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    IPRIN_IINT_OINT_AVAL_ACHG(25, "0011010101", "installmentprincipal.installmentinterest.outstandinginterest.aval.percentofanothercharge"), //
    DISB_OINT_SEGO(26, "0100011000", "disbursedamount.outstandinginterest.seguroobrigatorio"), //
    DISB_OINT_SEGO_ACHG(27, "0100011001", "disbursedamount.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_OPRIN_SEGO_AVAL_HONO(28, "1100101110", "flat.disbursedamount.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_OPRIN_SEGO_AVAL_HONO_ACHG(29, "1100101111",
            "flat.disbursedamount.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OINT_AVAL_HONO_ACHG(30, "1001010111", "flat.installmentinterest.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OINT_AVAL_HONO(31, "1001010110", "flat.installmentinterest.outstandinginterest.aval.honorarios"), //
    IPRIN_IINT_OPRIN_SEGO_HONO(32, "0011101010",
            "installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios"), //
    IPRIN_IINT_OPRIN_SEGO_HONO_ACHG(33, "0011101011",
            "installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_OPRIN_ACHG(34, "1100100001", "flat.disbursedamount.outstandingprincipal.percentofanothercharge"), //
    OPRIN_OINT_HONO_ACHG(35, "0000110011", "outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    OPRIN_OINT_SEGO_AVAL(36, "0000111100", "outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_IINT_OINT_SEGO(37, "1111011000",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio"), //
    OPRIN_OINT_SEGO_AVAL_ACHG(38, "0000111101", "outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OINT_SEGO_ACHG(39, "1111011001",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_AVAL_ACHG(40, "0100000101", "disbursedamount.aval.percentofanothercharge"), //
    DISB_AVAL(41, "0100000100", "disbursedamount.aval"), //
    FLAT_IPRIN_IINT_OPRIN_OINT(42, "1011110000", "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_AVAL_HONO(43, "1110110110",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_AVAL_ACHG(44, "1110110101",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_AVAL(45, "1110110100",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.aval"), //
    OPRIN_OINT_SEGO_AVAL_HONO(46, "0000111110", "outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_OPRIN_SEGO_AVAL(47, "1100101100", "flat.disbursedamount.outstandingprincipal.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_IINT_OINT_SEGO_HONO(48, "1111011010",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_OPRIN_HONO_ACHG(49, "1100100011", "flat.disbursedamount.outstandingprincipal.honorarios.percentofanothercharge"), //
    FLAT_DISB_OPRIN_HONO(50, "1100100010", "flat.disbursedamount.outstandingprincipal.honorarios"), //
    OPRIN_OINT(51, "0000110000", "outstandingprincipal.outstandinginterest"), //
    FLAT_DISB_IPRIN_IINT_OINT_SEGO_HONO_ACHG(52, "1111011011",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    OPRIN_OINT_ACHG(53, "0000110001", "outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    OPRIN_OINT_HONO(54, "0000110010", "outstandingprincipal.outstandinginterest.honorarios"), //
    FLAT_DISB_OPRIN(55, "1100100000", "flat.disbursedamount.outstandingprincipal"), //
    FLAT_DISB_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(56, "1100111111",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OINT_AVAL(57, "1001010100", "flat.installmentinterest.outstandinginterest.aval"), //
    FLAT_IINT_OINT_AVAL_ACHG(58, "1001010101", "flat.installmentinterest.outstandinginterest.aval.percentofanothercharge"), //
    IPRIN_OPRIN_OINT_AVAL_ACHG(59, "0010110101",
            "installmentprincipal.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    DISB_OPRIN_SEGO_HONO(60, "0100101010", "disbursedamount.outstandingprincipal.seguroobrigatorio.honorarios"), //
    IPRIN_OPRIN_OINT_AVAL(61, "0010110100", "installmentprincipal.outstandingprincipal.outstandinginterest.aval"), //
    DISB_OPRIN_SEGO_HONO_ACHG(62, "0100101011", "disbursedamount.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    OPRIN_SEGO_AVAL_HONO(63, "0000101110", "outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    DISB_IPRIN_OPRIN_OINT_ACHG(64, "0110110001",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    OPRIN_SEGO_AVAL_HONO_ACHG(65, "0000101111", "outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_IINT_AVAL(66, "0011000100", "installmentprincipal.installmentinterest.aval"), //
    DISB_IPRIN_OPRIN_OINT(67, "0110110000", "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest"), //
    IPRIN_IINT_AVAL_ACHG(68, "0011000101", "installmentprincipal.installmentinterest.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT(69, "0111000000", "disbursedamount.installmentprincipal.installmentinterest"), //
    FLAT_DISB_OPRIN_OINT_SEGO_AVAL_HONO(70, "1100111110",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_IPRIN_IINT_ACHG(71, "0111000001", "disbursedamount.installmentprincipal.installmentinterest.percentofanothercharge"), //
    DISB_OINT_AVAL_HONO_ACHG(72, "0100010111", "disbursedamount.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    DISB_OINT_AVAL_HONO(73, "0100010110", "disbursedamount.outstandinginterest.aval.honorarios"), //
    FLAT_IPRIN_SEGO(74, "1010001000", "flat.installmentprincipal.seguroobrigatorio"), //
    FLAT_DISB_OPRIN_OINT_HONO_ACHG(75, "1100110011",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_SEGO_ACHG(76, "1010001001", "flat.installmentprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_OPRIN_OINT_SEGO_AVAL(77, "1100111100",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_OPRIN_OINT_SEGO_AVAL_ACHG(78, "1100111101",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    IINT_OINT_SEGO_AVAL_HONO(79, "0001011110", "installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_SEGO_ACHG(80, "1111101001",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_SEGO(81, "1111101000",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio"), //
    OPRIN_SEGO_AVAL_ACHG(82, "0000101101", "outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_OINT_HONO(83, "0110110010",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.honorarios"), //
    IINT_OINT_SEGO_AVAL_HONO_ACHG(84, "0001011111",
            "installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_HONO_ACHG(85, "1011100011",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_SEGO_AVAL_ACHG(86, "1011101101",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_SEGO_AVAL(87, "1011101100",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval"), //
    IPRIN_OPRIN_OINT_AVAL_HONO(88, "0010110110", "installmentprincipal.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    DISB_OPRIN_SEGO_ACHG(89, "0100101001", "disbursedamount.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_HONO(90, "1011100010", "flat.installmentprincipal.installmentinterest.outstandingprincipal.honorarios"), //
    FLAT_DISB_OINT(91, "1100010000", "flat.disbursedamount.outstandinginterest"), //
    FLAT_DISB_OINT_ACHG(92, "1100010001", "flat.disbursedamount.outstandinginterest.percentofanothercharge"), //
    IPRIN_OPRIN_OINT_AVAL_HONO_ACHG(93, "0010110111",
            "installmentprincipal.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_OINT_HONO_ACHG(94, "0110110011",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_OINT_SEGO_AVAL(95, "0110111100",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    DISB_IPRIN_OPRIN_OINT_SEGO_AVAL_ACHG(96, "0110111101",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_OPRIN_OINT_HONO(97, "1100110010", "flat.disbursedamount.outstandingprincipal.outstandinginterest.honorarios"), //
    DISB_OINT_AVAL(98, "0100010100", "disbursedamount.outstandinginterest.aval"), //
    FLAT_IPRIN_SEGO_HONO(99, "1010001010", "flat.installmentprincipal.seguroobrigatorio.honorarios"), //
    FLAT_DISB_OPRIN_OINT_ACHG(100, "1100110001", "flat.disbursedamount.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_IPRIN_SEGO_HONO_ACHG(101, "1010001011", "flat.installmentprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_OPRIN_OINT(102, "1100110000", "flat.disbursedamount.outstandingprincipal.outstandinginterest"), //
    DISB_IPRIN_OPRIN_OINT_SEGO_AVAL_HONO(103, "0110111110",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_OINT_AVAL_ACHG(104, "0100010101", "disbursedamount.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN(105, "1011100000", "flat.installmentprincipal.installmentinterest.outstandingprincipal"), //
    FLAT_IPRIN_IINT_OPRIN_ACHG(106, "1011100001",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_SEGO_AVAL_HONO(107, "0110101110",
            "disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    DISB_IPRIN_OPRIN_SEGO_AVAL_HONO_ACHG(108, "0110101111",
            "disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IINT_ACHG(109, "0001000001", "installmentinterest.percentofanothercharge"), //
    IINT(110, "0001000000", "installmentinterest"), //
    OPRIN(111, "0000100000", "outstandingprincipal"), //
    DISB_IPRIN_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(112, "0110111111",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_SEGO_AVAL_HONO_ACHG(113, "0111001111",
            "disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_SEGO_AVAL_HONO(114, "0111001110",
            "disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_IINT_OPRIN_SEGO(115, "1001101000", "flat.installmentinterest.outstandingprincipal.seguroobrigatorio"), //
    FLAT_IINT_OPRIN_SEGO_ACHG(116, "1001101001", "flat.installmentinterest.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_SEGO_AVAL_ACHG(117, "0110101101",
            "disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_OINT_SEGO_AVAL_HONO_ACHG(118, "1100011111",
            "flat.disbursedamount.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_OINT_SEGO_AVAL_HONO(119, "1100011110", "flat.disbursedamount.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_IPRIN_OPRIN_SEGO_AVAL(120, "0110101100", "disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval"), //
    DISB_IPRIN_OPRIN_HONO_ACHG(121, "0110100011",
            "disbursedamount.installmentprincipal.outstandingprincipal.honorarios.percentofanothercharge"), //
    IINT_SEGO_AVAL_ACHG(122, "0001001101", "installmentinterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_HONO(123, "0110100010", "disbursedamount.installmentprincipal.outstandingprincipal.honorarios"), //
    IINT_HONO_ACHG(124, "0001000011", "installmentinterest.honorarios.percentofanothercharge"), //
    IINT_SEGO_AVAL(125, "0001001100", "installmentinterest.seguroobrigatorio.aval"), //
    IINT_HONO(126, "0001000010", "installmentinterest.honorarios"), //
    OPRIN_SEGO_AVAL(127, "0000101100", "outstandingprincipal.seguroobrigatorio.aval"), //
    OPRIN_HONO_ACHG(128, "0000100011", "outstandingprincipal.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(129, "1011111111",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    OPRIN_HONO(130, "0000100010", "outstandingprincipal.honorarios"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_HONO(131, "1011111110",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_IPRIN_IINT_HONO(132, "0111000010", "disbursedamount.installmentprincipal.installmentinterest.honorarios"), //
    DISB_IPRIN_IINT_SEGO_AVAL(133, "0111001100", "disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.aval"), //
    DISB_IPRIN_IINT_HONO_ACHG(134, "0111000011",
            "disbursedamount.installmentprincipal.installmentinterest.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_SEGO_AVAL_ACHG(135, "0111001101",
            "disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.aval.percentofanothercharge"), //
    IPRIN_IINT_OINT_AVAL_HONO_ACHG(136, "0011010111",
            "installmentprincipal.installmentinterest.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    OPRIN_ACHG(137, "0000100001", "outstandingprincipal.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_ACHG(138, "1011111101",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_SEGO_HONO_ACHG(139, "1111101011",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_SEGO_HONO(140, "1111101010",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios"), //
    IPRIN_IINT_OINT_AVAL_HONO(141, "0011010110", "installmentprincipal.installmentinterest.outstandinginterest.aval.honorarios"), //
    FLAT_IINT_OPRIN_SEGO_HONO_ACHG(142, "1001101011",
            "flat.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_ACHG(143, "0110100001", "disbursedamount.installmentprincipal.outstandingprincipal.percentofanothercharge"), //
    FLAT_IINT_OPRIN_SEGO_HONO(144, "1001101010", "flat.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios"), //
    FLAT_DISB_OINT_HONO_ACHG(145, "1100010011", "flat.disbursedamount.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_OINT_SEGO_AVAL(146, "1100011100", "flat.disbursedamount.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_OINT_HONO(147, "1100010010", "flat.disbursedamount.outstandinginterest.honorarios"), //
    FLAT_DISB_OINT_SEGO_AVAL_ACHG(148, "1100011101",
            "flat.disbursedamount.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_OPRIN_AVAL_HONO_ACHG(149, "1000100111", "flat.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_AVAL_HONO(150, "1000100110", "flat.outstandingprincipal.aval.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_SEGO(151, "1111111000",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    IPRIN_OPRIN_AVAL(152, "0010100100", "installmentprincipal.outstandingprincipal.aval"), //
    IPRIN_IINT_OPRIN_OINT_AVAL_HONO_ACHG(153, "0011110111",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_AVAL_ACHG(154, "1111100101",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.aval.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT_AVAL_HONO(155, "0011110110",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_AVAL(156, "1111100100",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.aval"), //
    DISB_IINT_SEGO_HONO(157, "0101001010", "disbursedamount.installmentinterest.seguroobrigatorio.honorarios"), //
    OINT_SEGO_AVAL_ACHG(158, "0000011101", "outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IINT_SEGO_HONO_ACHG(159, "0101001011", "disbursedamount.installmentinterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    OINT_HONO_ACHG(160, "0000010011", "outstandinginterest.honorarios.percentofanothercharge"), //
    OINT_SEGO_AVAL(161, "0000011100", "outstandinginterest.seguroobrigatorio.aval"), //
    OINT_HONO(162, "0000010010", "outstandinginterest.honorarios"), //
    FLAT_OPRIN_OINT_SEGO_HONO(163, "1000111010", "flat.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_OPRIN_OINT_SEGO_HONO_ACHG(164, "1000111011",
            "flat.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_AVAL_HONO(165, "1111100110",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.aval.honorarios"), //
    DISB_IPRIN_IINT_OINT_ACHG(166, "0111010001",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT(167, "0111010000", "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest"), //
    IPRIN_OPRIN_OINT_SEGO_ACHG(168, "0010111001",
            "installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    IPRIN_OPRIN_OINT_SEGO(169, "0010111000", "installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    IINT_OPRIN_SEGO_AVAL_HONO_ACHG(170, "0001101111",
            "installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_AVAL_ACHG(171, "1000100101", "flat.outstandingprincipal.aval.percentofanothercharge"), //
    IINT_OPRIN_SEGO_AVAL_HONO(172, "0001101110", "installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    DISB_IPRIN(173, "0110000000", "disbursedamount.installmentprincipal"), //
    DISB_IPRIN_ACHG(174, "0110000001", "disbursedamount.installmentprincipal.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT_AVAL_ACHG(175, "0011110101",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT_AVAL(176, "0011110100", "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval"), //
    OINT_ACHG(177, "0000010001", "outstandinginterest.percentofanothercharge"), //
    OINT(178, "0000010000", "outstandinginterest"), //
    FLAT_OPRIN_OINT_SEGO_ACHG(179, "1000111001", "flat.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_OPRIN_OINT_SEGO(180, "1000111000", "flat.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    IPRIN_OPRIN_OINT_SEGO_HONO(181, "0010111010",
            "installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_OPRIN_SEGO(182, "1110101000", "flat.disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio"), //
    DISB_IPRIN_HONO(183, "0110000010", "disbursedamount.installmentprincipal.honorarios"), //
    IPRIN_OPRIN_OINT_SEGO_HONO_ACHG(184, "0010111011",
            "installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_SEGO_ACHG(185, "1110101001",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    IINT_OPRIN_HONO_ACHG(186, "0001100011", "installmentinterest.outstandingprincipal.honorarios.percentofanothercharge"), //
    IINT_OPRIN_SEGO_AVAL(187, "0001101100", "installmentinterest.outstandingprincipal.seguroobrigatorio.aval"), //
    IINT_OPRIN_SEGO_AVAL_ACHG(188, "0001101101", "installmentinterest.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IINT_OPRIN_AVAL_ACHG(189, "1001100101", "flat.installmentinterest.outstandingprincipal.aval.percentofanothercharge"), //
    FLAT_IINT_OPRIN_AVAL(190, "1001100100", "flat.installmentinterest.outstandingprincipal.aval"), //
    FLAT_IINT_OPRIN_OINT_SEGO(191, "1001111000", "flat.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_SEGO_HONO_ACHG(192, "1110111011",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_SEGO_HONO(193, "1110111010",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    DISB_IPRIN_OINT_HONO(194, "0110010010", "disbursedamount.installmentprincipal.outstandinginterest.honorarios"), //
    DISB_IPRIN_OINT_SEGO_AVAL(195, "0110011100", "disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    DISB_IPRIN_OINT_HONO_ACHG(196, "0110010011",
            "disbursedamount.installmentprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    IINT_OINT_ACHG(197, "0001010001", "installmentinterest.outstandinginterest.percentofanothercharge"), //
    IINT_OINT(198, "0001010000", "installmentinterest.outstandinginterest"), //
    DISB_IPRIN_OINT_SEGO_AVAL_ACHG(199, "0110011101",
            "disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT_SEGO_AVAL_HONO_ACHG(200, "0111011111",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT_SEGO_AVAL_HONO(201, "0111011110",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_IINT_OPRIN_AVAL_HONO(202, "1001100110", "flat.installmentinterest.outstandingprincipal.aval.honorarios"), //
    DISB_IPRIN_IINT_OPRIN_SEGO_AVAL_HONO_ACHG(203, "0111101111",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OPRIN_AVAL_HONO_ACHG(204, "1001100111",
            "flat.installmentinterest.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    DISB_SEGO_HONO_ACHG(205, "0100001011", "disbursedamount.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_SEGO_HONO_ACHG(206, "1111111011",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_SEGO_HONO(207, "1111111010",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    IPRIN_OPRIN_AVAL_HONO_ACHG(208, "0010100111", "installmentprincipal.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    IPRIN_OPRIN_AVAL_HONO(209, "0010100110", "installmentprincipal.outstandingprincipal.aval.honorarios"), //
    FLAT_IINT_OPRIN_OINT_SEGO_HONO(210, "1001111010",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_IINT_OPRIN_OINT_SEGO_HONO_ACHG(211, "1001111011",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_AVAL_HONO_ACHG(212, "1111100111",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_SEGO(213, "0101001000", "disbursedamount.installmentinterest.seguroobrigatorio"), //
    OINT_SEGO_AVAL_HONO_ACHG(214, "0000011111", "outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_SEGO_ACHG(215, "0101001001", "disbursedamount.installmentinterest.seguroobrigatorio.percentofanothercharge"), //
    OINT_SEGO_AVAL_HONO(216, "0000011110", "outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    IINT_OINT_HONO(217, "0001010010", "installmentinterest.outstandinginterest.honorarios"), //
    DISB_IPRIN_OINT_SEGO_AVAL_HONO_ACHG(218, "0110011111",
            "disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OPRIN_OINT_SEGO_ACHG(219, "1001111001",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_IPRIN_OINT_SEGO_AVAL_HONO(220, "0110011110",
            "disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    IINT_OINT_SEGO_AVAL_ACHG(221, "0001011101", "installmentinterest.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    IINT_OINT_HONO_ACHG(222, "0001010011", "installmentinterest.outstandinginterest.honorarios.percentofanothercharge"), //
    IINT_OINT_SEGO_AVAL(223, "0001011100", "installmentinterest.outstandinginterest.seguroobrigatorio.aval"), //
    IPRIN_OPRIN_AVAL_ACHG(224, "0010100101", "installmentprincipal.outstandingprincipal.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT_SEGO_AVAL_ACHG(225, "0111011101",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT_HONO_ACHG(226, "0111010011",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT_SEGO_AVAL(227, "0111011100",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_SEGO_ACHG(228, "1111111001",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT_HONO(229, "0111010010", "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.honorarios"), //
    IPRIN_IINT_OPRIN_AVAL_HONO(230, "0011100110", "installmentprincipal.installmentinterest.outstandingprincipal.aval.honorarios"), //
    DISB_SEGO(231, "0100001000", "disbursedamount.seguroobrigatorio"), //
    DISB_SEGO_ACHG(232, "0100001001", "disbursedamount.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_SEGO_AVAL_HONO_ACHG(233, "1101101111",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT_SEGO_ACHG(234, "0011111001",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_OPRIN_OINT_AVAL_HONO_ACHG(235, "1000110111",
            "flat.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT_SEGO_HONO_ACHG(236, "0011111011",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT_SEGO_HONO(237, "0011111010",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    DISB_IINT_OINT_SEGO_HONO_ACHG(238, "0101011011",
            "disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_AVAL(239, "1111110100",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval"), //
    DISB_SEGO_HONO(240, "0100001010", "disbursedamount.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_OPRIN_AVAL(241, "1110100100", "flat.disbursedamount.installmentprincipal.outstandingprincipal.aval"), //
    DISB_IINT_OINT_SEGO_HONO(242, "0101011010", "disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_AVAL_ACHG(243, "1111110101",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_AVAL_ACHG(244, "0011100101",
            "installmentprincipal.installmentinterest.outstandingprincipal.aval.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_AVAL(245, "0011100100", "installmentprincipal.installmentinterest.outstandingprincipal.aval"), //
    FLAT_DISB_IPRIN_OPRIN_AVAL_ACHG(246, "1110100101",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.aval.percentofanothercharge"), //
    FLAT_OPRIN_OINT_AVAL_ACHG(247, "1000110101", "flat.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_SEGO_ACHG(248, "1110111001",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_OPRIN_OINT_AVAL(249, "1000110100", "flat.outstandingprincipal.outstandinginterest.aval"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_SEGO(250, "1110111000",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    FLAT_OPRIN_OINT_AVAL_HONO(251, "1000110110", "flat.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    IPRIN_IINT_OPRIN_OINT_SEGO(252, "0011111000",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    DISB_IPRIN_OINT(253, "0110010000", "disbursedamount.installmentprincipal.outstandinginterest"), //
    DISB_IPRIN_OINT_ACHG(254, "0110010001", "disbursedamount.installmentprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_AVAL_HONO_ACHG(255, "1110100111",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_AVAL_HONO(256, "1110100110", "flat.disbursedamount.installmentprincipal.outstandingprincipal.aval.honorarios"), //
    IPRIN_IINT_OPRIN_AVAL_HONO_ACHG(257, "0011100111",
            "installmentprincipal.installmentinterest.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_AVAL(258, "0101000100", "disbursedamount.installmentinterest.aval"), //
    IPRIN_OPRIN_SEGO_HONO_ACHG(259, "0010101011",
            "installmentprincipal.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN(260, "1101100000", "flat.disbursedamount.installmentinterest.outstandingprincipal"), //
    IPRIN_OPRIN_SEGO_HONO(261, "0010101010", "installmentprincipal.outstandingprincipal.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IINT_OPRIN_ACHG(262, "1101100001", "flat.disbursedamount.installmentinterest.outstandingprincipal.percentofanothercharge"), //
    DISB_IINT_AVAL_ACHG(263, "0101000101", "disbursedamount.installmentinterest.aval.percentofanothercharge"), //
    FLAT_IINT_OPRIN_OINT_AVAL_ACHG(264, "1001110101",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_IINT_OPRIN_OINT_AVAL(265, "1001110100", "flat.installmentinterest.outstandingprincipal.outstandinginterest.aval"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_AVAL_HONO_ACHG(266, "1110110111",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_HONO(267, "1101100010", "flat.disbursedamount.installmentinterest.outstandingprincipal.honorarios"), //
    FLAT_OPRIN_AVAL(268, "1000100100", "flat.outstandingprincipal.aval"), //
    DISB_IINT_AVAL_HONO_ACHG(269, "0101000111", "disbursedamount.installmentinterest.aval.honorarios.percentofanothercharge"), //
    IPRIN_OPRIN_SEGO_ACHG(270, "0010101001", "installmentprincipal.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    DISB_IINT_AVAL_HONO(271, "0101000110", "disbursedamount.installmentinterest.aval.honorarios"), //
    IPRIN_OPRIN_SEGO(272, "0010101000", "installmentprincipal.outstandingprincipal.seguroobrigatorio"), //
    FLAT_DISB_IINT_OPRIN_HONO_ACHG(273, "1101100011",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_SEGO_AVAL(274, "1101101100",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.aval"), //
    FLAT_DISB_IINT_OPRIN_SEGO_AVAL_ACHG(275, "1101101101",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IINT_OPRIN_OINT_AVAL_HONO_ACHG(276, "1001110111",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OPRIN_OINT_AVAL_HONO(277, "1001110110", "flat.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    DISB_OINT_SEGO_HONO(278, "0100011010", "disbursedamount.outstandinginterest.seguroobrigatorio.honorarios"), //
    DISB_OINT_SEGO_HONO_ACHG(279, "0100011011", "disbursedamount.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_SEGO_AVAL_HONO(280, "1101101110",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    DISB_IINT_OINT_SEGO(281, "0101011000", "disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_AVAL_HONO_ACHG(282, "1111110111",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_OINT_SEGO_ACHG(283, "0101011001",
            "disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_AVAL_HONO(284, "1111110110",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_OINT_SEGO(285, "1000011000", "flat.outstandinginterest.seguroobrigatorio"), //
    FLAT_AVAL(286, "1000000100", "flat.aval"), //
    FLAT_AVAL_ACHG(287, "1000000101", "flat.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_OINT_HONO_ACHG(288, "0111110011",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_OINT_HONO(289, "0111110010",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.honorarios"), //
    DISB_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL(290, "0111111100",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    DISB_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_ACHG(291, "0111111101",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_SEGO_HONO_ACHG(292, "1010111011",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_SEGO_HONO(293, "1010111010",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_AVAL_HONO(294, "1000000110", "flat.aval.honorarios"), //
    FLAT_DISB_IINT_OPRIN_OINT_SEGO_AVAL_HONO(295, "1101111110",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IINT_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(296, "1101111111",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_OINT(297, "0101010000", "disbursedamount.installmentinterest.outstandinginterest"), //
    DISB_IINT_OINT_ACHG(298, "0101010001", "disbursedamount.installmentinterest.outstandinginterest.percentofanothercharge"), //
    IPRIN_AVAL(299, "0010000100", "installmentprincipal.aval"), //
    IPRIN_AVAL_ACHG(300, "0010000101", "installmentprincipal.aval.percentofanothercharge"), //
    IINT_OPRIN_OINT_ACHG(301, "0001110001", "installmentinterest.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    IINT_OPRIN_OINT(302, "0001110000", "installmentinterest.outstandingprincipal.outstandinginterest"), //
    DISB_IPRIN_IINT_OPRIN_OINT_ACHG(303, "0111110001",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_OINT(304, "0111110000",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest"), //
    IPRIN_OINT_SEGO(305, "0010011000", "installmentprincipal.outstandinginterest.seguroobrigatorio"), //
    DISB_IINT_OINT_SEGO_AVAL(306, "0101011100", "disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.aval"), //
    DISB_IINT_OINT_HONO_ACHG(307, "0101010011",
            "disbursedamount.installmentinterest.outstandinginterest.honorarios.percentofanothercharge"), //
    DISB_IINT_OINT_HONO(308, "0101010010", "disbursedamount.installmentinterest.outstandinginterest.honorarios"), //
    IPRIN_AVAL_HONO(309, "0010000110", "installmentprincipal.aval.honorarios"), //
    DISB_IINT_OINT_SEGO_AVAL_ACHG(310, "0101011101",
            "disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    IPRIN_AVAL_HONO_ACHG(311, "0010000111", "installmentprincipal.aval.honorarios.percentofanothercharge"), //
    DISB_OPRIN_OINT_AVAL(312, "0100110100", "disbursedamount.outstandingprincipal.outstandinginterest.aval"), //
    DISB_OPRIN_OINT_AVAL_ACHG(313, "0100110101", "disbursedamount.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    IPRIN_OINT_SEGO_HONO(314, "0010011010", "installmentprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_SEGO(315, "1110001000", "flat.disbursedamount.installmentprincipal.seguroobrigatorio"), //
    IPRIN_OINT_SEGO_HONO_ACHG(316, "0010011011",
            "installmentprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_SEGO_ACHG(317, "1110001001", "flat.disbursedamount.installmentprincipal.seguroobrigatorio.percentofanothercharge"), //
    IPRIN_OINT_SEGO_ACHG(318, "0010011001", "installmentprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_OINT(319, "1101110000", "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest"), //
    FLAT_DISB_IINT_OPRIN_OINT_ACHG(320, "1101110001",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_DISB_IPRIN_SEGO_HONO(321, "1110001010", "flat.disbursedamount.installmentprincipal.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_SEGO_HONO_ACHG(322, "1110001011",
            "flat.disbursedamount.installmentprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_OINT_AVAL_HONO(323, "1000010110", "flat.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_IINT_OPRIN_OINT_HONO(324, "1101110010",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.honorarios"), //
    FLAT_OINT_AVAL_HONO_ACHG(325, "1000010111", "flat.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_OINT_HONO_ACHG(326, "1101110011",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_OINT_SEGO_AVAL(327, "1101111100",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IINT_OPRIN_OINT_SEGO_AVAL_ACHG(328, "1101111101",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_SEGO_ACHG(329, "1010101001",
            "flat.installmentprincipal.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_SEGO(330, "1010101000", "flat.installmentprincipal.outstandingprincipal.seguroobrigatorio"), //
    SEGO_AVAL_ACHG(331, "0000001101", "seguroobrigatorio.aval.percentofanothercharge"), //
    HONO_ACHG(332, "0000000011", "honorarios.percentofanothercharge"), //
    SEGO_AVAL(333, "0000001100", "seguroobrigatorio.aval"), //
    DISB_IPRIN_IINT_OPRIN_HONO_ACHG(334, "0111100011",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_SEGO_AVAL(335, "0111101100",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval"), //
    DISB_IPRIN_IINT_OPRIN_SEGO_AVAL_ACHG(336, "0111101101",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IPRIN_IINT(337, "1011000000", "flat.installmentprincipal.installmentinterest"), //
    FLAT_OINT_AVAL(338, "1000010100", "flat.outstandinginterest.aval"), //
    FLAT_OINT_AVAL_ACHG(339, "1000010101", "flat.outstandinginterest.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_SEGO_AVAL_HONO(340, "0111101110",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    SEGO_AVAL_HONO(341, "0000001110", "seguroobrigatorio.aval.honorarios"), //
    ACHG(342, "0000000001", "percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_ACHG(343, "0111100001",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN(344, "0111100000", "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal"), //
    IPRIN_OINT_AVAL(345, "0010010100", "installmentprincipal.outstandinginterest.aval"), //
    IPRIN_OINT_AVAL_ACHG(346, "0010010101", "installmentprincipal.outstandinginterest.aval.percentofanothercharge"), //
    HONO(347, "0000000010", "honorarios"), //
    DISB_IPRIN_IINT_OPRIN_HONO(348, "0111100010",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.honorarios"), //
    DISB_IINT_OPRIN_SEGO_ACHG(349, "0101101001",
            "disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OINT_SEGO_AVAL_HONO_ACHG(350, "1011011111",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_ACHG(351, "1101010001", "flat.disbursedamount.installmentinterest.outstandinginterest.percentofanothercharge"), //
    DISB_IINT_OPRIN_SEGO(352, "0101101000", "disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio"), //
    FLAT_DISB_IINT_OINT(353, "1101010000", "flat.disbursedamount.installmentinterest.outstandinginterest"), //
    IINT_OPRIN_HONO(354, "0001100010", "installmentinterest.outstandingprincipal.honorarios"), //
    DISB_IPRIN_HONO_ACHG(355, "0110000011", "disbursedamount.installmentprincipal.honorarios.percentofanothercharge"), //
    DISB_IPRIN_SEGO_AVAL_ACHG(356, "0110001101", "disbursedamount.installmentprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_SEGO_AVAL(357, "0110001100", "disbursedamount.installmentprincipal.seguroobrigatorio.aval"), //
    IINT_OPRIN_OINT_SEGO_AVAL_ACHG(358, "0001111101",
            "installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    IINT_OPRIN_OINT_HONO(359, "0001110010", "installmentinterest.outstandingprincipal.outstandinginterest.honorarios"), //
    IINT_OPRIN_OINT_HONO_ACHG(360, "0001110011",
            "installmentinterest.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    IINT_OPRIN_OINT_SEGO_AVAL(361, "0001111100", "installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_IPRIN_IINT_SEGO_AVAL_HONO(362, "1011001110", "flat.installmentprincipal.installmentinterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_IPRIN_IINT_SEGO_AVAL_HONO_ACHG(363, "1011001111",
            "flat.installmentprincipal.installmentinterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_OINT_AVAL_HONO(364, "0010010110", "installmentprincipal.outstandinginterest.aval.honorarios"), //
    IPRIN_OINT_AVAL_HONO_ACHG(365, "0010010111", "installmentprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_SEGO_AVAL_ACHG(366, "1011001101",
            "flat.installmentprincipal.installmentinterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_SEGO_AVAL_HONO(367, "0110001110", "disbursedamount.installmentprincipal.seguroobrigatorio.aval.honorarios"), //
    IINT_OPRIN(368, "0001100000", "installmentinterest.outstandingprincipal"), //
    IINT_OPRIN_ACHG(369, "0001100001", "installmentinterest.outstandingprincipal.percentofanothercharge"), //
    DISB_IINT_OPRIN_SEGO_HONO(370, "0101101010", "disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios"), //
    FLAT_IPRIN_OPRIN_AVAL(371, "1010100100", "flat.installmentprincipal.outstandingprincipal.aval"), //
    FLAT_IPRIN_OPRIN_AVAL_ACHG(372, "1010100101", "flat.installmentprincipal.outstandingprincipal.aval.percentofanothercharge"), //
    SEGO_AVAL_HONO_ACHG(373, "0000001111", "seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_OPRIN_SEGO_HONO_ACHG(374, "0101101011",
            "disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    DISB_IPRIN_SEGO_AVAL_HONO_ACHG(375, "0110001111",
            "disbursedamount.installmentprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IINT_OPRIN_OINT_SEGO_AVAL_HONO(376, "0001111110",
            "installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    IINT_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(377, "0001111111",
            "installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_SEGO_AVAL(378, "1011001100", "flat.installmentprincipal.installmentinterest.seguroobrigatorio.aval"), //
    FLAT_IPRIN_IINT_HONO_ACHG(379, "1011000011", "flat.installmentprincipal.installmentinterest.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_HONO(380, "1011000010", "flat.installmentprincipal.installmentinterest.honorarios"), //
    DISB_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_HONO(381, "0111111110",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(382, "0111111111",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_SEGO_ACHG(383, "1010111001",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_IINT_ACHG(384, "1011000001", "flat.installmentprincipal.installmentinterest.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_SEGO(385, "1010111000", "flat.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    FLAT_IPRIN_OPRIN_AVAL_HONO(386, "1010100110", "flat.installmentprincipal.outstandingprincipal.aval.honorarios"), //
    FLAT_IPRIN_OPRIN_AVAL_HONO_ACHG(387, "1010100111",
            "flat.installmentprincipal.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OINT(388, "1011010000", "flat.installmentprincipal.installmentinterest.outstandinginterest"), //
    FLAT_IPRIN_IINT_OINT_ACHG(389, "1011010001",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_SEGO_AVAL_HONO_ACHG(390, "1101011111",
            "flat.disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_SEGO_AVAL_HONO(391, "1101011110",
            "flat.disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_OPRIN_OINT_SEGO_HONO_ACHG(392, "0100111011",
            "disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IINT_SEGO_HONO_ACHG(393, "1001001011", "flat.installmentinterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB(394, "1100000000", "flat.disbursedamount"), //
    FLAT_IINT_SEGO_HONO(395, "1001001010", "flat.installmentinterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_ACHG(396, "1100000001", "flat.disbursedamount.percentofanothercharge"), //
    FLAT_IINT_SEGO_ACHG(397, "1001001001", "flat.installmentinterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_HONO(398, "1100000010", "flat.disbursedamount.honorarios"), //
    IPRIN_IINT_SEGO(399, "0011001000", "installmentprincipal.installmentinterest.seguroobrigatorio"), //
    DISB_OPRIN_AVAL_HONO(400, "0100100110", "disbursedamount.outstandingprincipal.aval.honorarios"), //
    DISB_OPRIN_AVAL_HONO_ACHG(401, "0100100111", "disbursedamount.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OINT_HONO(402, "1011010010", "flat.installmentprincipal.installmentinterest.outstandinginterest.honorarios"), //
    FLAT_IPRIN_IINT_OINT_HONO_ACHG(403, "1011010011",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OINT_SEGO_AVAL(404, "1011011100",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IINT_OINT_SEGO_AVAL_ACHG(405, "1101011101",
            "flat.disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OINT_SEGO_AVAL_ACHG(406, "1011011101",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_HONO_ACHG(407, "1101010011",
            "flat.disbursedamount.installmentinterest.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_SEGO_AVAL(408, "1101011100", "flat.disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.aval"), //
    DISB_IINT_OPRIN_AVAL(409, "0101100100", "disbursedamount.installmentinterest.outstandingprincipal.aval"), //
    FLAT_DISB_IINT_OINT_HONO(410, "1101010010", "flat.disbursedamount.installmentinterest.outstandinginterest.honorarios"), //
    DISB_IINT_OPRIN_AVAL_ACHG(411, "0101100101", "disbursedamount.installmentinterest.outstandingprincipal.aval.percentofanothercharge"), //
    DISB_OPRIN_OINT_SEGO(412, "0100111000", "disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    DISB_OPRIN_OINT_SEGO_ACHG(413, "0100111001",
            "disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_ACHG(414, "1010000001", "flat.installmentprincipal.percentofanothercharge"), //
    FLAT_IPRIN(415, "1010000000", "flat.installmentprincipal"), //
    DISB_OPRIN_OINT_SEGO_HONO(416, "0100111010", "disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_IPRIN_IINT_OPRIN_SEGO_HONO_ACHG(417, "1011101011",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_SEGO_HONO(418, "1011101010",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios"), //
    FLAT_IPRIN_IINT_OINT_SEGO_AVAL_HONO(419, "1011011110",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_IINT_OPRIN_AVAL_HONO(420, "0101100110", "disbursedamount.installmentinterest.outstandingprincipal.aval.honorarios"), //
    DISB_IINT_OPRIN_AVAL_HONO_ACHG(421, "0101100111",
            "disbursedamount.installmentinterest.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OINT_SEGO_AVAL(422, "1010011100", "flat.installmentprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_IPRIN_OINT_HONO_ACHG(423, "1010010011", "flat.installmentprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OINT_HONO(424, "1010010010", "flat.installmentprincipal.outstandinginterest.honorarios"), //
    FLAT_DISB_IPRIN_IINT_SEGO_HONO(425, "1111001010",
            "flat.disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_IINT_SEGO_HONO_ACHG(426, "1111001011",
            "flat.disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_SEGO_ACHG(427, "0101111001",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_SEGO(428, "0101111000",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    FLAT_DISB_SEGO_AVAL_HONO_ACHG(429, "1100001111", "flat.disbursedamount.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_SEGO_ACHG(430, "1111001001",
            "flat.disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.percentofanothercharge"), //
    IPRIN_IINT_OINT_SEGO_HONO_ACHG(431, "0011011011",
            "installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OINT_ACHG(432, "1010010001", "flat.installmentprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_SEGO_ACHG(433, "1011101001",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_SEGO(434, "1011101000", "flat.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio"), //
    FLAT_DISB_IPRIN_OINT_AVAL_ACHG(435, "1110010101",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OINT_AVAL(436, "1110010100", "flat.disbursedamount.installmentprincipal.outstandinginterest.aval"), //
    FLAT_IPRIN_OINT_SEGO_AVAL_HONO(437, "1010011110", "flat.installmentprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_OPRIN_SEGO(438, "0100101000", "disbursedamount.outstandingprincipal.seguroobrigatorio"), //
    FLAT_IPRIN_OINT_SEGO_AVAL_HONO_ACHG(439, "1010011111",
            "flat.installmentprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_IINT_AVAL_HONO(440, "0011000110", "installmentprincipal.installmentinterest.aval.honorarios"), //
    IPRIN_IINT_OINT_SEGO_HONO(441, "0011011010",
            "installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios"), //
    IPRIN_IINT_AVAL_HONO_ACHG(442, "0011000111", "installmentprincipal.installmentinterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_SEGO(443, "1111001000", "flat.disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio"), //
    DISB_IINT_OPRIN_OINT_SEGO_HONO_ACHG(444, "0101111011",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IINT_SEGO(445, "1001001000", "flat.installmentinterest.seguroobrigatorio"), //
    FLAT_DISB_HONO_ACHG(446, "1100000011", "flat.disbursedamount.honorarios.percentofanothercharge"), //
    FLAT_DISB_SEGO_AVAL_ACHG(447, "1100001101", "flat.disbursedamount.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_SEGO_HONO(448, "0101111010",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_SEGO_AVAL(449, "1100001100", "flat.disbursedamount.seguroobrigatorio.aval"), //
    FLAT_DISB_SEGO_AVAL_HONO(450, "1100001110", "flat.disbursedamount.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IINT(451, "1101000000", "flat.disbursedamount.installmentinterest"), //
    FLAT_SEGO_HONO_ACHG(452, "1000001011", "flat.seguroobrigatorio.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OINT_SEGO_ACHG(453, "0011011001",
            "installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_SEGO_HONO(454, "1000001010", "flat.seguroobrigatorio.honorarios"), //
    IPRIN_IINT_OINT_SEGO(455, "0011011000", "installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio"), //
    FLAT_DISB_IINT_ACHG(456, "1101000001", "flat.disbursedamount.installmentinterest.percentofanothercharge"), //
    FLAT_IPRIN_OINT_SEGO_AVAL_ACHG(457, "1010011101",
            "flat.installmentprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OINT_AVAL_HONO(458, "1110010110", "flat.disbursedamount.installmentprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_IPRIN_OINT_AVAL_HONO_ACHG(459, "1110010111",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OINT_SEGO_HONO(460, "1001011010", "flat.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OINT_AVAL(461, "1111010100",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.aval"), //
    FLAT_DISB_IPRIN_IINT_OINT_AVAL_ACHG(462, "1111010101",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_IINT_AVAL_HONO(463, "1001000110", "flat.installmentinterest.aval.honorarios"), //
    FLAT_IINT_AVAL_HONO_ACHG(464, "1001000111", "flat.installmentinterest.aval.honorarios.percentofanothercharge"), //
    FLAT_SEGO_ACHG(465, "1000001001", "flat.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_IINT_HONO_ACHG(466, "1101000011", "flat.disbursedamount.installmentinterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_SEGO_AVAL(467, "1101001100", "flat.disbursedamount.installmentinterest.seguroobrigatorio.aval"), //
    FLAT_SEGO(468, "1000001000", "flat.seguroobrigatorio"), //
    FLAT_DISB_IINT_HONO(469, "1101000010", "flat.disbursedamount.installmentinterest.honorarios"), //
    FLAT_DISB_IINT_SEGO_AVAL_ACHG(470, "1101001101",
            "flat.disbursedamount.installmentinterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_SEGO_HONO(471, "1010101010", "flat.installmentprincipal.outstandingprincipal.seguroobrigatorio.honorarios"), //
    FLAT_IPRIN_OPRIN_SEGO_HONO_ACHG(472, "1010101011",
            "flat.installmentprincipal.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IINT_OINT_SEGO_ACHG(473, "1001011001", "flat.installmentinterest.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IINT_OINT_SEGO(474, "1001011000", "flat.installmentinterest.outstandinginterest.seguroobrigatorio"), //
    FLAT_DISB_IPRIN_IINT_OINT_AVAL_HONO(475, "1111010110",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.aval.honorarios"), //
    FLAT_IPRIN_OINT(476, "1010010000", "flat.installmentprincipal.outstandinginterest"), //
    FLAT_DISB_IPRIN_IINT_OINT_AVAL_HONO_ACHG(477, "1111010111",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    DISB_OPRIN_OINT_AVAL_HONO(478, "0100110110", "disbursedamount.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    DISB_OPRIN_OINT_AVAL_HONO_ACHG(479, "0100110111",
            "disbursedamount.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_AVAL_ACHG(480, "0101110101",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_AVAL(481, "0101110100", "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.aval"), //
    FLAT_DISB_IINT_SEGO_AVAL_HONO_ACHG(482, "1101001111",
            "flat.disbursedamount.installmentinterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_SEGO_AVAL_HONO(483, "1101001110", "flat.disbursedamount.installmentinterest.seguroobrigatorio.aval.honorarios"), //
    IPRIN_SEGO(484, "0010001000", "installmentprincipal.seguroobrigatorio"), //
    IPRIN_SEGO_ACHG(485, "0010001001", "installmentprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IINT_OINT_SEGO_HONO_ACHG(486, "1001011011",
            "flat.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OINT_SEGO(487, "1110011000", "flat.disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio"), //
    DISB_IINT_OPRIN_OINT_AVAL_HONO(488, "0101110110",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    DISB_IINT_OPRIN_OINT_AVAL_HONO_ACHG(489, "0101110111",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_AVAL_HONO(490, "1110000110", "flat.disbursedamount.installmentprincipal.aval.honorarios"), //
    FLAT_DISB_IPRIN_AVAL(491, "1110000100", "flat.disbursedamount.installmentprincipal.aval"), //
    FLAT_DISB_IPRIN_AVAL_ACHG(492, "1110000101", "flat.disbursedamount.installmentprincipal.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_AVAL_HONO(493, "1111000110", "flat.disbursedamount.installmentprincipal.installmentinterest.aval.honorarios"), //
    FLAT_DISB_IPRIN_IINT_AVAL_HONO_ACHG(494, "1111000111",
            "flat.disbursedamount.installmentprincipal.installmentinterest.aval.honorarios.percentofanothercharge"), //
    IPRIN_SEGO_HONO_ACHG(495, "0010001011", "installmentprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    IPRIN_SEGO_HONO(496, "0010001010", "installmentprincipal.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_OINT_SEGO_HONO(497, "1110011010",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_OINT_SEGO_HONO_ACHG(498, "1110011011",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_OINT_SEGO_HONO_ACHG(499, "1000011011", "flat.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_OINT_SEGO_HONO(500, "1000011010", "flat.outstandinginterest.seguroobrigatorio.honorarios"), //
    IPRIN_IINT_SEGO_ACHG(501, "0011001001", "installmentprincipal.installmentinterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_AVAL_HONO_ACHG(502, "1000000111", "flat.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_AVAL_HONO_ACHG(503, "1110000111", "flat.disbursedamount.installmentprincipal.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_AVAL(504, "1001000100", "flat.installmentinterest.aval"), //
    FLAT_IINT_AVAL_ACHG(505, "1001000101", "flat.installmentinterest.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_AVAL_ACHG(506, "1111000101",
            "flat.disbursedamount.installmentprincipal.installmentinterest.aval.percentofanothercharge"), //
    IPRIN_IINT_SEGO_HONO(507, "0011001010", "installmentprincipal.installmentinterest.seguroobrigatorio.honorarios"), //
    IPRIN_IINT_SEGO_HONO_ACHG(508, "0011001011",
            "installmentprincipal.installmentinterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_AVAL(509, "1111000100", "flat.disbursedamount.installmentprincipal.installmentinterest.aval"), //
    DISB_OPRIN_AVAL_ACHG(510, "0100100101", "disbursedamount.outstandingprincipal.aval.percentofanothercharge"), //
    FLAT_OINT_SEGO_ACHG(511, "1000011001", "flat.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_OPRIN_AVAL(512, "0100100100", "disbursedamount.outstandingprincipal.aval"), //
    FLAT_DISB_IPRIN_OINT_SEGO_ACHG(513, "1110011001",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OINT_SEGO_AVAL_HONO(514, "1111011110",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OINT_SEGO_AVAL_HONO_ACHG(515, "1111011111",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_SEGO_HONO_ACHG(516, "0111001011",
            "disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    OPRIN_OINT_SEGO(517, "0000111000", "outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    DISB_IPRIN_OPRIN_AVAL_HONO_ACHG(518, "0110100111",
            "disbursedamount.installmentprincipal.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_AVAL_HONO(519, "0110100110", "disbursedamount.installmentprincipal.outstandingprincipal.aval.honorarios"), //
    IINT_SEGO_ACHG(520, "0001001001", "installmentinterest.seguroobrigatorio.percentofanothercharge"), //
    IINT_SEGO(521, "0001001000", "installmentinterest.seguroobrigatorio"), //
    OPRIN_AVAL_HONO(522, "0000100110", "outstandingprincipal.aval.honorarios"), //
    DISB_IPRIN_IINT_SEGO_ACHG(523, "0111001001",
            "disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_IPRIN_IINT_SEGO(524, "0111001000", "disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio"), //
    DISB_IPRIN_IINT_SEGO_HONO(525, "0111001010", "disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.honorarios"), //
    OPRIN_AVAL_ACHG(526, "0000100101", "outstandingprincipal.aval.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_AVAL_HONO_ACHG(527, "1011110111",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    OPRIN_AVAL(528, "0000100100", "outstandingprincipal.aval"), //
    DISB_OINT_SEGO_AVAL_HONO(529, "0100011110", "disbursedamount.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_OINT_SEGO_AVAL_HONO_ACHG(530, "0100011111",
            "disbursedamount.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IINT_SEGO_HONO(531, "0001001010", "installmentinterest.seguroobrigatorio.honorarios"), //
    DISB_IPRIN_OPRIN_AVAL_ACHG(532, "0110100101", "disbursedamount.installmentprincipal.outstandingprincipal.aval.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_AVAL(533, "0110100100", "disbursedamount.installmentprincipal.outstandingprincipal.aval"), //
    FLAT_IINT_OINT_ACHG(534, "1001010001", "flat.installmentinterest.outstandinginterest.percentofanothercharge"), //
    FLAT_IINT_OINT(535, "1001010000", "flat.installmentinterest.outstandinginterest"), //
    FLAT_DISB_OPRIN_AVAL_HONO_ACHG(536, "1100100111", "flat.disbursedamount.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    IINT_SEGO_HONO_ACHG(537, "0001001011", "installmentinterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_AVAL_HONO(538, "1011110110",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_OPRIN_OINT_SEGO_HONO(539, "1100111010",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_OPRIN_OINT_SEGO_HONO_ACHG(540, "1100111011",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    DISB_OINT_HONO_ACHG(541, "0100010011", "disbursedamount.outstandinginterest.honorarios.percentofanothercharge"), //
    DISB_OINT_HONO(542, "0100010010", "disbursedamount.outstandinginterest.honorarios"), //
    FLAT_DISB_OPRIN_OINT_SEGO_ACHG(543, "1100111001",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_AVAL(544, "1011110100",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_AVAL_ACHG(545, "1011110101",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    DISB_OINT_SEGO_AVAL(546, "0100011100", "disbursedamount.outstandinginterest.seguroobrigatorio.aval"), //
    DISB_OINT_SEGO_AVAL_ACHG(547, "0100011101", "disbursedamount.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_OINT_AVAL_ACHG(548, "1100010101", "flat.disbursedamount.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_DISB_OINT_AVAL(549, "1100010100", "flat.disbursedamount.outstandinginterest.aval"), //
    FLAT_DISB_OPRIN_AVAL_ACHG(550, "1100100101", "flat.disbursedamount.outstandingprincipal.aval.percentofanothercharge"), //
    FLAT_DISB_OPRIN_AVAL(551, "1100100100", "flat.disbursedamount.outstandingprincipal.aval"), //
    OPRIN_OINT_AVAL_HONO(552, "0000110110", "outstandingprincipal.outstandinginterest.aval.honorarios"), //
    OPRIN_OINT_AVAL_HONO_ACHG(553, "0000110111", "outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_OPRIN_OINT_SEGO(554, "1100111000", "flat.disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    DISB_OINT_ACHG(555, "0100010001", "disbursedamount.outstandinginterest.percentofanothercharge"), //
    DISB_OINT(556, "0100010000", "disbursedamount.outstandinginterest"), //
    FLAT_DISB_OPRIN_AVAL_HONO(557, "1100100110", "flat.disbursedamount.outstandingprincipal.aval.honorarios"), //
    DISB_IPRIN_OPRIN_SEGO_HONO(558, "0110101010", "disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.honorarios"), //
    DISB_IPRIN_OPRIN_SEGO_HONO_ACHG(559, "0110101011",
            "disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    IINT_AVAL_ACHG(560, "0001000101", "installmentinterest.aval.percentofanothercharge"), //
    IINT_AVAL(561, "0001000100", "installmentinterest.aval"), //
    FLAT_IINT_OPRIN_ACHG(562, "1001100001", "flat.installmentinterest.outstandingprincipal.percentofanothercharge"), //
    IPRIN_IINT_OINT_SEGO_AVAL_HONO_ACHG(563, "0011011111",
            "installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OINT_SEGO_AVAL_HONO(564, "0011011110",
            "installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_IINT_OPRIN_SEGO_AVAL(565, "1001101100", "flat.installmentinterest.outstandingprincipal.seguroobrigatorio.aval"), //
    DISB_IPRIN_IINT_OINT_SEGO_HONO_ACHG(566, "0111011011",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IINT_OPRIN_HONO_ACHG(567, "1001100011", "flat.installmentinterest.outstandingprincipal.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT_SEGO_HONO(568, "0111011010",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_IINT_OPRIN_HONO(569, "1001100010", "flat.installmentinterest.outstandingprincipal.honorarios"), //
    DISB_IPRIN_OPRIN_SEGO(570, "0110101000", "disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio"), //
    DISB_IPRIN_OPRIN_SEGO_ACHG(571, "0110101001",
            "disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    IINT_AVAL_HONO_ACHG(572, "0001000111", "installmentinterest.aval.honorarios.percentofanothercharge"), //
    IINT_AVAL_HONO(573, "0001000110", "installmentinterest.aval.honorarios"), //
    FLAT_IINT_OPRIN_SEGO_AVAL_ACHG(574, "1001101101",
            "flat.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    OPRIN_SEGO(575, "0000101000", "outstandingprincipal.seguroobrigatorio"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_SEGO_HONO(576, "1011111010",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    OPRIN_SEGO_ACHG(577, "0000101001", "outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_SEGO_HONO_ACHG(578, "1011111011",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_SEGO_AVAL_ACHG(579, "1111101101",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT_AVAL_HONO(580, "0111000110", "disbursedamount.installmentprincipal.installmentinterest.aval.honorarios"), //
    DISB_IPRIN_IINT_AVAL_HONO_ACHG(581, "0111000111",
            "disbursedamount.installmentprincipal.installmentinterest.aval.honorarios.percentofanothercharge"), //
    IINT_OINT_SEGO(582, "0001011000", "installmentinterest.outstandinginterest.seguroobrigatorio"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_SEGO_AVAL_HONO_ACHG(583, "1111101111",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OINT_SEGO_AVAL_ACHG(584, "0011011101",
            "installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_SEGO_AVAL_HONO(585, "1111101110",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    IPRIN_IINT_OINT_HONO(586, "0011010010", "installmentprincipal.installmentinterest.outstandinginterest.honorarios"), //
    IPRIN_IINT_OINT_SEGO_AVAL(587, "0011011100", "installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval"), //
    IINT_OINT_SEGO_ACHG(588, "0001011001", "installmentinterest.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    IPRIN_IINT_OINT_HONO_ACHG(589, "0011010011",
            "installmentprincipal.installmentinterest.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_IINT_OPRIN_SEGO_AVAL_HONO(590, "1001101110", "flat.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    DISB_IPRIN_IINT_OINT_SEGO_ACHG(591, "0111011001",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IINT_OPRIN_SEGO_AVAL_HONO_ACHG(592, "1001101111",
            "flat.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_OPRIN_OINT(593, "0010110000", "installmentprincipal.outstandingprincipal.outstandinginterest"), //
    DISB_IPRIN_IINT_OINT_SEGO(594, "0111011000",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio"), //
    FLAT_DISB_OINT_AVAL_HONO(595, "1100010110", "flat.disbursedamount.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_OINT_AVAL_HONO_ACHG(596, "1100010111", "flat.disbursedamount.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_AVAL_ACHG(597, "1011100101",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.aval.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_SEGO_ACHG(598, "1011111001",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_OINT_SEGO(599, "1011111000",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    FLAT_DISB_AVAL_ACHG(600, "1100000101", "flat.disbursedamount.aval.percentofanothercharge"), //
    FLAT_DISB_AVAL(601, "1100000100", "flat.disbursedamount.aval"), //
    FLAT_DISB_OPRIN_OINT_AVAL_HONO_ACHG(602, "1100110111",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_OPRIN_OINT_AVAL_HONO(603, "1100110110", "flat.disbursedamount.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_IPRIN_SEGO_AVAL(604, "1010001100", "flat.installmentprincipal.seguroobrigatorio.aval"), //
    FLAT_IPRIN_SEGO_AVAL_ACHG(605, "1010001101", "flat.installmentprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    IINT_OINT_SEGO_HONO_ACHG(606, "0001011011",
            "installmentinterest.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OINT_ACHG(607, "0011010001", "installmentprincipal.installmentinterest.outstandinginterest.percentofanothercharge"), //
    OPRIN_AVAL_HONO_ACHG(608, "0000100111", "outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    IINT_OINT_SEGO_HONO(609, "0001011010", "installmentinterest.outstandinginterest.seguroobrigatorio.honorarios"), //
    DISB_IPRIN_OPRIN_OINT_SEGO(610, "0110111000",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    FLAT_IPRIN_HONO_ACHG(611, "1010000011", "flat.installmentprincipal.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_HONO(612, "1010000010", "flat.installmentprincipal.honorarios"), //
    IPRIN_IINT_OINT(613, "0011010000", "installmentprincipal.installmentinterest.outstandinginterest"), //
    FLAT_IPRIN_IINT_OPRIN_AVAL_HONO(614, "1011100110",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.aval.honorarios"), //
    FLAT_IPRIN_IINT_OPRIN_AVAL_HONO_ACHG(615, "1011100111",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    OPRIN_OINT_SEGO_ACHG(616, "0000111001", "outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_OINT_SEGO_ACHG(617, "0110111001",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_OPRIN_OINT_AVAL(618, "1100110100", "flat.disbursedamount.outstandingprincipal.outstandinginterest.aval"), //
    FLAT_IPRIN_SEGO_AVAL_HONO(619, "1010001110", "flat.installmentprincipal.seguroobrigatorio.aval.honorarios"), //
    FLAT_IPRIN_SEGO_AVAL_HONO_ACHG(620, "1010001111", "flat.installmentprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_OPRIN_OINT_AVAL_ACHG(621, "1100110101",
            "flat.disbursedamount.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_OINT_SEGO_HONO(622, "0110111010",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    DISB_IPRIN_OPRIN_OINT_SEGO_HONO_ACHG(623, "0110111011",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_AVAL(624, "1011100100", "flat.installmentprincipal.installmentinterest.outstandingprincipal.aval"), //
    OPRIN_OINT_SEGO_HONO(625, "0000111010", "outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    OPRIN_OINT_SEGO_HONO_ACHG(626, "0000111011",
            "outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    IINT_OPRIN_SEGO(627, "0001101000", "installmentinterest.outstandingprincipal.seguroobrigatorio"), //
    DISB_IPRIN_AVAL_HONO(628, "0110000110", "disbursedamount.installmentprincipal.aval.honorarios"), //
    DISB_IPRIN_AVAL_HONO_ACHG(629, "0110000111", "disbursedamount.installmentprincipal.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_OINT_SEGO_HONO(630, "0110011010", "disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_IINT_OPRIN_OINT_HONO(631, "1001110010", "flat.installmentinterest.outstandingprincipal.outstandinginterest.honorarios"), //
    IINT_OINT_AVAL_ACHG(632, "0001010101", "installmentinterest.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_IINT_OPRIN_OINT_ACHG(633, "1001110001",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_IINT_OPRIN_OINT(634, "1001110000", "flat.installmentinterest.outstandingprincipal.outstandinginterest"), //
    DISB_IPRIN_OINT_SEGO_ACHG(635, "0110011001",
            "disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_IPRIN_OINT_SEGO(636, "0110011000", "disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio"), //
    IINT_OINT_AVAL(637, "0001010100", "installmentinterest.outstandinginterest.aval"), //
    FLAT_DISB_IPRIN_OPRIN_SEGO_AVAL_HONO(638, "1110101110",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(639, "1111111111",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_SEGO_AVAL_HONO_ACHG(640, "1110101111",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_ACHG(641, "1000100001", "flat.outstandingprincipal.percentofanothercharge"), //
    FLAT_OPRIN(642, "1000100000", "flat.outstandingprincipal"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_HONO(643, "1111111110",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_IINT_OPRIN_OINT_SEGO_AVAL_HONO(644, "1001111110",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_IINT_OPRIN_OINT_HONO_ACHG(645, "1001110011",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_IINT_OPRIN_OINT_SEGO_AVAL(646, "1001111100",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    DISB_IPRIN_OINT_SEGO_HONO_ACHG(647, "0110011011",
            "disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IINT_OPRIN_OINT_SEGO_AVAL_ACHG(648, "1001111101",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    IINT_OINT_AVAL_HONO_ACHG(649, "0001010111", "installmentinterest.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    IINT_OINT_AVAL_HONO(650, "0001010110", "installmentinterest.outstandinginterest.aval.honorarios"), //
    DISB_IPRIN_IINT_OINT_AVAL_HONO_ACHG(651, "0111010111",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_ACHG(652, "1111111101",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT_AVAL_HONO(653, "0111010110",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_HONO_ACHG(654, "1111110011",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_SEGO_AVAL(655, "1111111100",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    AVAL_HONO_ACHG(656, "0000000111", "aval.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_SEGO_AVAL_ACHG(657, "1000101101", "flat.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    AVAL_HONO(658, "0000000110", "aval.honorarios"), //
    FLAT_OPRIN_HONO_ACHG(659, "1000100011", "flat.outstandingprincipal.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_SEGO_AVAL(660, "1000101100", "flat.outstandingprincipal.seguroobrigatorio.aval"), //
    DISB_IPRIN_IINT_OPRIN_SEGO_ACHG(661, "0111101001",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_OPRIN_HONO(662, "1000100010", "flat.outstandingprincipal.honorarios"), //
    IPRIN_OPRIN_OINT_ACHG(663, "0010110001", "installmentprincipal.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    OPRIN_SEGO_HONO_ACHG(664, "0000101011", "outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    IPRIN_OPRIN_ACHG(665, "0010100001", "installmentprincipal.outstandingprincipal.percentofanothercharge"), //
    IPRIN_OPRIN(666, "0010100000", "installmentprincipal.outstandingprincipal"), //
    IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_ACHG(667, "0011111101",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    OPRIN_SEGO_HONO(668, "0000101010", "outstandingprincipal.seguroobrigatorio.honorarios"), //
    IPRIN_IINT_OPRIN_OINT_HONO_ACHG(669, "0011110011",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT_SEGO_AVAL(670, "0011111100",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_ACHG(671, "1111100001",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.percentofanothercharge"), //
    DISB_IINT_SEGO_AVAL_HONO(672, "0101001110", "disbursedamount.installmentinterest.seguroobrigatorio.aval.honorarios"), //
    OINT_SEGO_ACHG(673, "0000011001", "outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_IINT_SEGO_AVAL_HONO_ACHG(674, "0101001111",
            "disbursedamount.installmentinterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_AVAL(675, "0111000100", "disbursedamount.installmentprincipal.installmentinterest.aval"), //
    OINT_SEGO(676, "0000011000", "outstandinginterest.seguroobrigatorio"), //
    DISB_IPRIN_IINT_AVAL_ACHG(677, "0111000101", "disbursedamount.installmentprincipal.installmentinterest.aval.percentofanothercharge"), //
    OINT_SEGO_HONO(678, "0000011010", "outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_IPRIN_OPRIN_OINT_AVAL(679, "1010110100", "flat.installmentprincipal.outstandingprincipal.outstandinginterest.aval"), //
    IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_HONO(680, "0011111110",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_IINT_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(681, "1001111111",
            "flat.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_HONO_ACHG(682, "1111100011",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_SEGO_AVAL(683, "1111101100",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_HONO(684, "1111100010",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.honorarios"), //
    IPRIN_OPRIN_OINT_HONO_ACHG(685, "0010110011",
            "installmentprincipal.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    IPRIN_OPRIN_OINT_SEGO_AVAL(686, "0010111100", "installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_OPRIN_ACHG(687, "1110100001", "flat.disbursedamount.installmentprincipal.outstandingprincipal.percentofanothercharge"), //
    IPRIN_OPRIN_OINT_HONO(688, "0010110010", "installmentprincipal.outstandingprincipal.outstandinginterest.honorarios"), //
    FLAT_DISB_IPRIN_OPRIN(689, "1110100000", "flat.disbursedamount.installmentprincipal.outstandingprincipal"), //
    DISB_IPRIN_IINT_OPRIN_SEGO_HONO(690, "0111101010",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios"), //
    DISB_IPRIN_IINT_OPRIN_SEGO_HONO_ACHG(691, "0111101011",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    AVAL(692, "0000000100", "aval"), //
    FLAT_OPRIN_SEGO_AVAL_HONO_ACHG(693, "1000101111", "flat.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IINT_OPRIN_SEGO_HONO(694, "0001101010", "installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios"), //
    FLAT_OPRIN_SEGO_AVAL_HONO(695, "1000101110", "flat.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    AVAL_ACHG(696, "0000000101", "aval.percentofanothercharge"), //
    IINT_OPRIN_SEGO_HONO_ACHG(697, "0001101011",
            "installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    DISB_IPRIN_AVAL_ACHG(698, "0110000101", "disbursedamount.installmentprincipal.aval.percentofanothercharge"), //
    IPRIN_OPRIN_OINT_SEGO_AVAL_ACHG(699, "0010111101",
            "installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_AVAL(700, "0110000100", "disbursedamount.installmentprincipal.aval"), //
    IPRIN_IINT_OPRIN_OINT_ACHG(701, "0011110001",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT(702, "0011110000", "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest"), //
    FLAT_IPRIN_OPRIN_OINT_AVAL_ACHG(703, "1010110101",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_AVAL_HONO(704, "1010110110",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_IPRIN_OPRIN_OINT_AVAL_HONO_ACHG(705, "1010110111",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT_HONO(706, "0011110010",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OPRIN(707, "1111100000", "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal"), //
    FLAT_DISB_IPRIN_OPRIN_HONO(708, "1110100010", "flat.disbursedamount.installmentprincipal.outstandingprincipal.honorarios"), //
    IPRIN_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(709, "0010111111",
            "installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OPRIN(710, "1001100000", "flat.installmentinterest.outstandingprincipal"), //
    FLAT_DISB_IPRIN_OPRIN_SEGO_AVAL_ACHG(711, "1110101101",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    IPRIN_OPRIN_OINT_SEGO_AVAL_HONO(712, "0010111110",
            "installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_OPRIN_HONO_ACHG(713, "1110100011",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_SEGO_AVAL(714, "1110101100",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval"), //
    IINT_OPRIN_SEGO_ACHG(715, "0001101001", "installmentinterest.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_SEGO(716, "0111101000",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio"), //
    DISB_IINT_ACHG(717, "0101000001", "disbursedamount.installmentinterest.percentofanothercharge"), //
    IPRIN_OPRIN_SEGO_AVAL_HONO_ACHG(718, "0010101111",
            "installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IINT(719, "0101000000", "disbursedamount.installmentinterest"), //
    IPRIN_OPRIN_SEGO_AVAL_HONO(720, "0010101110", "installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(721, "1110111111",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_SEGO_AVAL_HONO(722, "1110111110",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_OPRIN_OINT(723, "1000110000", "flat.outstandingprincipal.outstandinginterest"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_SEGO_AVAL_ACHG(724, "1110111101",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_OINT_AVAL_HONO(725, "0110010110", "disbursedamount.installmentprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_IINT_OPRIN_SEGO(726, "1101101000", "flat.disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio"), //
    DISB_IPRIN_OINT_AVAL_HONO_ACHG(727, "0110010111",
            "disbursedamount.installmentprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN(728, "0011100000", "installmentprincipal.installmentinterest.outstandingprincipal"), //
    DISB_SEGO_AVAL_HONO(729, "0100001110", "disbursedamount.seguroobrigatorio.aval.honorarios"), //
    DISB_SEGO_AVAL_HONO_ACHG(730, "0100001111", "disbursedamount.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_OPRIN_SEGO_AVAL_ACHG(731, "0010101101",
            "installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    IPRIN_OPRIN_HONO_ACHG(732, "0010100011", "installmentprincipal.outstandingprincipal.honorarios.percentofanothercharge"), //
    IPRIN_OPRIN_SEGO_AVAL(733, "0010101100", "installmentprincipal.outstandingprincipal.seguroobrigatorio.aval"), //
    DISB_IINT_HONO_ACHG(734, "0101000011", "disbursedamount.installmentinterest.honorarios.percentofanothercharge"), //
    IPRIN_OPRIN_HONO(735, "0010100010", "installmentprincipal.outstandingprincipal.honorarios"), //
    DISB_IINT_HONO(736, "0101000010", "disbursedamount.installmentinterest.honorarios"), //
    FLAT_DISB_IINT_OPRIN_SEGO_ACHG(737, "1101101001",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(738, "0011111111",
            "installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_SEGO_AVAL(739, "0101001100", "disbursedamount.installmentinterest.seguroobrigatorio.aval"), //
    DISB_IINT_SEGO_AVAL_ACHG(740, "0101001101", "disbursedamount.installmentinterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_SEGO_HONO(741, "1101101010",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IINT_OPRIN_SEGO_HONO_ACHG(742, "1101101011",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_HONO_ACHG(743, "0011100011",
            "installmentprincipal.installmentinterest.outstandingprincipal.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_SEGO_AVAL(744, "0011101100", "installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval"), //
    IPRIN_IINT_OPRIN_HONO(745, "0011100010", "installmentprincipal.installmentinterest.outstandingprincipal.honorarios"), //
    DISB_HONO_ACHG(746, "0100000011", "disbursedamount.honorarios.percentofanothercharge"), //
    DISB_SEGO_AVAL_ACHG(747, "0100001101", "disbursedamount.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_HONO(748, "0100000010", "disbursedamount.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_HONO(749, "1111110010",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.honorarios"), //
    DISB_SEGO_AVAL(750, "0100001100", "disbursedamount.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_OPRIN_OINT(751, "1110110000", "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest"), //
    OINT_AVAL_HONO_ACHG(752, "0000010111", "outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_OINT_SEGO_AVAL_ACHG(753, "1000111101",
            "flat.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    OINT_AVAL_HONO(754, "0000010110", "outstandinginterest.aval.honorarios"), //
    FLAT_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(755, "1000111111",
            "flat.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_OINT_SEGO_AVAL_HONO(756, "1000111110", "flat.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_IINT_OINT_SEGO_AVAL_HONO(757, "0101011110",
            "disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_IPRIN_IINT_OINT_AVAL(758, "0111010100", "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.aval"), //
    FLAT_DISB_IINT_OPRIN_OINT_SEGO_HONO_ACHG(759, "1101111011",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT_ACHG(760, "1111110001",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    DISB_IINT_OINT_SEGO_AVAL_HONO_ACHG(761, "0101011111",
            "disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_OPRIN_SEGO_HONO(762, "1100101010", "flat.disbursedamount.outstandingprincipal.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OPRIN_OINT(763, "1111110000",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest"), //
    FLAT_DISB_OPRIN_SEGO_HONO_ACHG(764, "1100101011",
            "flat.disbursedamount.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_ACHG(765, "0011100001", "installmentprincipal.installmentinterest.outstandingprincipal.percentofanothercharge"), //
    DISB_IPRIN_IINT_OINT_AVAL_ACHG(766, "0111010101",
            "disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.aval.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_SEGO_AVAL_HONO_ACHG(767, "0011101111",
            "installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_SEGO_AVAL_HONO(768, "0011101110",
            "installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    DISB(769, "0100000000", "disbursedamount"), //
    DISB_ACHG(770, "0100000001", "disbursedamount.percentofanothercharge"), //
    OINT_AVAL_ACHG(771, "0000010101", "outstandinginterest.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_HONO_ACHG(772, "1110110011",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_SEGO_AVAL(773, "1110111100",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    OINT_AVAL(774, "0000010100", "outstandinginterest.aval"), //
    FLAT_OPRIN_OINT_ACHG(775, "1000110001", "flat.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_HONO(776, "1110110010",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.honorarios"), //
    FLAT_OPRIN_OINT_HONO_ACHG(777, "1000110011", "flat.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_OINT_SEGO_AVAL(778, "1000111100", "flat.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_OPRIN_OINT_HONO(779, "1000110010", "flat.outstandingprincipal.outstandinginterest.honorarios"), //
    FLAT_DISB_IPRIN_OPRIN_OINT_ACHG(780, "1110110001",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    DISB_IPRIN_OINT_AVAL(781, "0110010100", "disbursedamount.installmentprincipal.outstandinginterest.aval"), //
    DISB_IPRIN_OINT_AVAL_ACHG(782, "0110010101", "disbursedamount.installmentprincipal.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_DISB_OPRIN_SEGO(783, "1100101000", "flat.disbursedamount.outstandingprincipal.seguroobrigatorio"), //
    FLAT_DISB_OPRIN_SEGO_ACHG(784, "1100101001", "flat.disbursedamount.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    IPRIN_IINT_OPRIN_SEGO_AVAL_ACHG(785, "0011101101",
            "installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    IINT_OPRIN_OINT_AVAL_HONO_ACHG(786, "0001110111",
            "installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_AVAL_ACHG(787, "1101100101",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.aval.percentofanothercharge"), //
    IINT_OPRIN_OINT_AVAL_HONO(788, "0001110110", "installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_IINT_OPRIN_AVAL(789, "1101100100", "flat.disbursedamount.installmentinterest.outstandingprincipal.aval"), //
    FLAT_IPRIN_IINT_SEGO_HONO_ACHG(790, "1011001011",
            "flat.installmentprincipal.installmentinterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_HONO(791, "1110000010", "flat.disbursedamount.installmentprincipal.honorarios"), //
    IPRIN_OINT_SEGO_AVAL_HONO(792, "0010011110", "installmentprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_IPRIN_IINT_SEGO_HONO(793, "1011001010", "flat.installmentprincipal.installmentinterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IPRIN_HONO_ACHG(794, "1110000011", "flat.disbursedamount.installmentprincipal.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_SEGO_AVAL(795, "1110001100", "flat.disbursedamount.installmentprincipal.seguroobrigatorio.aval"), //
    IPRIN_OINT_HONO_ACHG(796, "0010010011", "installmentprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    IPRIN_OINT_SEGO_AVAL(797, "0010011100", "installmentprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_ACHG(798, "1110000001", "flat.disbursedamount.installmentprincipal.percentofanothercharge"), //
    IPRIN_OINT_SEGO_AVAL_ACHG(799, "0010011101", "installmentprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_OINT_SEGO_AVAL_HONO_ACHG(800, "1000011111", "flat.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN(801, "1010100000", "flat.installmentprincipal.outstandingprincipal"), //
    FLAT_IPRIN_OPRIN_ACHG(802, "1010100001", "flat.installmentprincipal.outstandingprincipal.percentofanothercharge"), //
    FLAT_OINT_SEGO_AVAL_HONO(803, "1000011110", "flat.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IINT_OPRIN_OINT_SEGO_HONO(804, "1101111010",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IINT_OPRIN_AVAL_HONO(805, "1101100110", "flat.disbursedamount.installmentinterest.outstandingprincipal.aval.honorarios"), //
    FLAT_DISB_IINT_OPRIN_AVAL_HONO_ACHG(806, "1101100111",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_SEGO(807, "1011001000", "flat.installmentprincipal.installmentinterest.seguroobrigatorio"), //
    FLAT_IPRIN_IINT_SEGO_ACHG(808, "1011001001", "flat.installmentprincipal.installmentinterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_IPRIN_SEGO_AVAL_HONO(809, "1110001110", "flat.disbursedamount.installmentprincipal.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_SEGO_AVAL_HONO_ACHG(810, "1110001111",
            "flat.disbursedamount.installmentprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN_OINT_SEGO_AVAL_HONO_ACHG(811, "0010011111",
            "installmentprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_SEGO_AVAL_ACHG(812, "1110001101",
            "flat.disbursedamount.installmentprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_OINT_HONO_ACHG(813, "1000010011", "flat.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_OINT_SEGO_AVAL_ACHG(814, "1000011101", "flat.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_IINT_OPRIN_OINT_SEGO(815, "1101111000",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    FLAT_OINT_SEGO_AVAL(816, "1000011100", "flat.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IINT_OPRIN_OINT_SEGO_ACHG(817, "1101111001",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_HONO(818, "1010100010", "flat.installmentprincipal.outstandingprincipal.honorarios"), //
    FLAT_IPRIN_OPRIN_SEGO_AVAL(819, "1010101100", "flat.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval"), //
    FLAT_IPRIN_OPRIN_HONO_ACHG(820, "1010100011", "flat.installmentprincipal.outstandingprincipal.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_SEGO_AVAL_ACHG(821, "1010101101",
            "flat.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_OINT_HONO(822, "1000010010", "flat.outstandinginterest.honorarios"), //
    DISB_IPRIN_IINT_OPRIN_OINT_AVAL_HONO_ACHG(823, "0111110111",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_OINT_AVAL_HONO(824, "0111110110",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_OINT(825, "1000010000", "flat.outstandinginterest"), //
    FLAT_OINT_ACHG(826, "1000010001", "flat.outstandinginterest.percentofanothercharge"), //
    DISB_IINT_OINT_AVAL_ACHG(827, "0101010101", "disbursedamount.installmentinterest.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_SEGO_AVAL_HONO_ACHG(828, "1010101111",
            "flat.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    IPRIN(829, "0010000000", "installmentprincipal"), //
    DISB_IINT_OINT_AVAL(830, "0101010100", "disbursedamount.installmentinterest.outstandinginterest.aval"), //
    FLAT_IPRIN_OPRIN_SEGO_AVAL_HONO(831, "1010101110", "flat.installmentprincipal.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    IPRIN_ACHG(832, "0010000001", "installmentprincipal.percentofanothercharge"), //
    IINT_OPRIN_OINT_AVAL(833, "0001110100", "installmentinterest.outstandingprincipal.outstandinginterest.aval"), //
    IINT_OPRIN_OINT_AVAL_ACHG(834, "0001110101",
            "installmentinterest.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_OINT_AVAL_ACHG(835, "0111110101",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_OINT_AVAL(836, "0111110100",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.aval"), //
    DISB_IINT_OINT_AVAL_HONO(837, "0101010110", "disbursedamount.installmentinterest.outstandinginterest.aval.honorarios"), //
    IPRIN_HONO(838, "0010000010", "installmentprincipal.honorarios"), //
    IPRIN_HONO_ACHG(839, "0010000011", "installmentprincipal.honorarios.percentofanothercharge"), //
    IPRIN_SEGO_AVAL(840, "0010001100", "installmentprincipal.seguroobrigatorio.aval"), //
    DISB_IINT_OINT_AVAL_HONO_ACHG(841, "0101010111",
            "disbursedamount.installmentinterest.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_OPRIN_HONO(842, "0101100010", "disbursedamount.installmentinterest.outstandingprincipal.honorarios"), //
    DISB_IINT_OPRIN_SEGO_AVAL(843, "0101101100", "disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.aval"), //
    FLAT_DISB_IINT_OINT_AVAL(844, "1101010100", "flat.disbursedamount.installmentinterest.outstandinginterest.aval"), //
    DISB_IINT_OPRIN_HONO_ACHG(845, "0101100011",
            "disbursedamount.installmentinterest.outstandingprincipal.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_SEGO_ACHG(846, "1000101001", "flat.outstandingprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_OPRIN_SEGO(847, "1000101000", "flat.outstandingprincipal.seguroobrigatorio"), //
    DISB_IINT_OPRIN_SEGO_AVAL_ACHG(848, "0101101101",
            "disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IPRIN_SEGO_ACHG(849, "0110001001", "disbursedamount.installmentprincipal.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_AVAL_ACHG(850, "1101010101",
            "flat.disbursedamount.installmentinterest.outstandinginterest.aval.percentofanothercharge"), //
    IINT_OPRIN_OINT_SEGO(851, "0001111000", "installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    IINT_OPRIN_OINT_SEGO_ACHG(852, "0001111001",
            "installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT(853, "1010110000", "flat.installmentprincipal.outstandingprincipal.outstandinginterest"), //
    FLAT_IPRIN_OPRIN_OINT_ACHG(854, "1010110001",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_IPRIN_IINT_AVAL_HONO_ACHG(855, "1011000111",
            "flat.installmentprincipal.installmentinterest.aval.honorarios.percentofanothercharge"), //
    IINT_OPRIN_OINT_SEGO_HONO(856, "0001111010",
            "installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    DISB_IPRIN_SEGO_HONO_ACHG(857, "0110001011",
            "disbursedamount.installmentprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    DISB_IPRIN_SEGO_HONO(858, "0110001010", "disbursedamount.installmentprincipal.seguroobrigatorio.honorarios"), //
    FLAT_DISB_IINT_OPRIN_OINT_AVAL(859, "1101110100",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.aval"), //
    FLAT_DISB_IINT_OPRIN_OINT_AVAL_ACHG(860, "1101110101",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    IINT_OPRIN_AVAL(861, "0001100100", "installmentinterest.outstandingprincipal.aval"), //
    IINT_OPRIN_AVAL_ACHG(862, "0001100101", "installmentinterest.outstandingprincipal.aval.percentofanothercharge"), //
    DISB_IINT_OPRIN_SEGO_AVAL_HONO_ACHG(863, "0101101111",
            "disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_OPRIN_SEGO_AVAL_HONO(864, "0101101110",
            "disbursedamount.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    FLAT_OPRIN_SEGO_HONO_ACHG(865, "1000101011", "flat.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_OPRIN_SEGO_HONO(866, "1000101010", "flat.outstandingprincipal.seguroobrigatorio.honorarios"), //
    IINT_OPRIN_OINT_SEGO_HONO_ACHG(867, "0001111011",
            "installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_AVAL_HONO(868, "1011000110", "flat.installmentprincipal.installmentinterest.aval.honorarios"), //
    OINT_SEGO_HONO_ACHG(869, "0000011011", "outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_OINT_SEGO_HONO_ACHG(870, "0111111011",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_HONO_ACHG(871, "1010110011",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_SEGO_AVAL_ACHG(872, "1010111101",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IPRIN_IINT_AVAL_ACHG(873, "1011000101", "flat.installmentprincipal.installmentinterest.aval.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_SEGO_AVAL(874, "1010111100",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_IPRIN_IINT_AVAL(875, "1011000100", "flat.installmentprincipal.installmentinterest.aval"), //
    FLAT_IPRIN_OPRIN_OINT_HONO(876, "1010110010", "flat.installmentprincipal.outstandingprincipal.outstandinginterest.honorarios"), //
    FLAT_DISB_IINT_OPRIN_OINT_AVAL_HONO(877, "1101110110",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_DISB_IINT_OPRIN_OINT_AVAL_HONO_ACHG(878, "1101110111",
            "flat.disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_AVAL_HONO_ACHG(879, "0111100111",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    SEGO_ACHG(880, "0000001001", "seguroobrigatorio.percentofanothercharge"), //
    DISB_IPRIN_IINT_OPRIN_AVAL_HONO(881, "0111100110",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.aval.honorarios"), //
    DISB_IPRIN_IINT_OPRIN_OINT_SEGO_HONO(882, "0111111010",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    DISB_IPRIN_IINT_OPRIN_OINT_SEGO(883, "0111111000",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio"), //
    DISB_IPRIN_IINT_OPRIN_OINT_SEGO_ACHG(884, "0111111001",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(885, "1010111111",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OPRIN_OINT_SEGO_AVAL_HONO(886, "1010111110",
            "flat.installmentprincipal.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_OPRIN_SEGO_HONO_ACHG(887, "1110101011",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OPRIN_SEGO_HONO(888, "1110101010",
            "flat.disbursedamount.installmentprincipal.outstandingprincipal.seguroobrigatorio.honorarios"), //
    SEGO_HONO_ACHG(889, "0000001011", "seguroobrigatorio.honorarios.percentofanothercharge"), //
    SEGO_HONO(890, "0000001010", "seguroobrigatorio.honorarios"), //
    FLAT_IPRIN_IINT_OINT_SEGO(891, "1011011000", "flat.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio"), //
    DISB_IPRIN_IINT_OPRIN_AVAL(892, "0111100100", "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.aval"), //
    FLAT_IPRIN_IINT_OINT_SEGO_ACHG(893, "1011011001",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_AVAL_HONO_ACHG(894, "1101010111",
            "flat.disbursedamount.installmentinterest.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_AVAL_HONO(895, "1101010110", "flat.disbursedamount.installmentinterest.outstandinginterest.aval.honorarios"), //
    DISB_IPRIN_IINT_OPRIN_AVAL_ACHG(896, "0111100101",
            "disbursedamount.installmentprincipal.installmentinterest.outstandingprincipal.aval.percentofanothercharge"), //
    IPRIN_OINT_HONO(897, "0010010010", "installmentprincipal.outstandinginterest.honorarios"), //
    IPRIN_OINT(898, "0010010000", "installmentprincipal.outstandinginterest"), //
    IPRIN_OINT_ACHG(899, "0010010001", "installmentprincipal.outstandinginterest.percentofanothercharge"), //
    DISB_IPRIN_SEGO(900, "0110001000", "disbursedamount.installmentprincipal.seguroobrigatorio"), //
    IINT_OPRIN_AVAL_HONO(901, "0001100110", "installmentinterest.outstandingprincipal.aval.honorarios"), //
    IINT_OPRIN_AVAL_HONO_ACHG(902, "0001100111", "installmentinterest.outstandingprincipal.aval.honorarios.percentofanothercharge"), //
    SEGO(903, "0000001000", "seguroobrigatorio"), //
    FLAT_IPRIN_IINT_OINT_SEGO_HONO(904, "1011011010",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_IPRIN_IINT_OINT_SEGO_HONO_ACHG(905, "1011011011",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_OINT_SEGO(906, "1010011000", "flat.installmentprincipal.outstandinginterest.seguroobrigatorio"), //
    FLAT_IPRIN_OINT_SEGO_ACHG(907, "1010011001", "flat.installmentprincipal.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    IPRIN_IINT_SEGO_AVAL_HONO_ACHG(908, "0011001111",
            "installmentprincipal.installmentinterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_AVAL_ACHG(909, "1010000101", "flat.installmentprincipal.aval.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_HONO_ACHG(910, "0101110011",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_SEGO_AVAL_ACHG(911, "0101111101",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_SEGO_AVAL(912, "0101111100",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    DISB_IINT_OPRIN_OINT_HONO(913, "0101110010", "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.honorarios"), //
    FLAT_IPRIN_AVAL_HONO_ACHG(914, "1010000111", "flat.installmentprincipal.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_AVAL_HONO(915, "1010000110", "flat.installmentprincipal.aval.honorarios"), //
    FLAT_DISB_OINT_SEGO_HONO_ACHG(916, "1100011011",
            "flat.disbursedamount.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_OINT_SEGO_HONO(917, "1100011010", "flat.disbursedamount.outstandinginterest.seguroobrigatorio.honorarios"), //
    FLAT_IPRIN_OINT_SEGO_HONO_ACHG(918, "1010011011",
            "flat.installmentprincipal.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    DISB_OPRIN_HONO(919, "0100100010", "disbursedamount.outstandingprincipal.honorarios"), //
    FLAT_IPRIN_OINT_SEGO_HONO(920, "1010011010", "flat.installmentprincipal.outstandinginterest.seguroobrigatorio.honorarios"), //
    IPRIN_IINT_HONO_ACHG(921, "0011000011", "installmentprincipal.installmentinterest.honorarios.percentofanothercharge"), //
    IPRIN_IINT_SEGO_AVAL(922, "0011001100", "installmentprincipal.installmentinterest.seguroobrigatorio.aval"), //
    IPRIN_IINT_SEGO_AVAL_ACHG(923, "0011001101", "installmentprincipal.installmentinterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(924, "0101111111",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT_SEGO_AVAL_HONO(925, "0101111110",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_AVAL_HONO_ACHG(926, "1100000111", "flat.disbursedamount.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_AVAL_HONO(927, "1100000110", "flat.disbursedamount.aval.honorarios"), //
    FLAT_DISB_IINT_AVAL_ACHG(928, "1101000101", "flat.disbursedamount.installmentinterest.aval.percentofanothercharge"), //
    IPRIN_IINT_SEGO_AVAL_HONO(929, "0011001110", "installmentprincipal.installmentinterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IINT_AVAL(930, "1101000100", "flat.disbursedamount.installmentinterest.aval"), //
    DISB_OPRIN(931, "0100100000", "disbursedamount.outstandingprincipal"), //
    DISB_OPRIN_ACHG(932, "0100100001", "disbursedamount.outstandingprincipal.percentofanothercharge"), //
    FLAT_DISB_OINT_SEGO_ACHG(933, "1100011001", "flat.disbursedamount.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_DISB_OINT_SEGO(934, "1100011000", "flat.disbursedamount.outstandinginterest.seguroobrigatorio"), //
    FLAT_IPRIN_IINT_OINT_AVAL(935, "1011010100", "flat.installmentprincipal.installmentinterest.outstandinginterest.aval"), //
    FLAT_IPRIN_IINT_OINT_AVAL_ACHG(936, "1011010101",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_SEGO_HONO_ACHG(937, "1101011011",
            "flat.disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_SEGO_HONO(938, "1101011010",
            "flat.disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.honorarios"), //
    DISB_OPRIN_SEGO_AVAL_HONO(939, "0100101110", "disbursedamount.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    IPRIN_IINT(940, "0011000000", "installmentprincipal.installmentinterest"), //
    DISB_IPRIN_OPRIN_OINT_AVAL(941, "0110110100", "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.aval"), //
    IPRIN_IINT_ACHG(942, "0011000001", "installmentprincipal.installmentinterest.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_OINT_AVAL_ACHG(943, "0110110101",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_IINT_SEGO_AVAL_HONO(944, "1001001110", "flat.installmentinterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_IINT_HONO_ACHG(945, "1001000011", "flat.installmentinterest.honorarios.percentofanothercharge"), //
    FLAT_IINT_SEGO_AVAL_ACHG(946, "1001001101", "flat.installmentinterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_SEGO(947, "1100001000", "flat.disbursedamount.seguroobrigatorio"), //
    FLAT_IINT_SEGO_AVAL(948, "1001001100", "flat.installmentinterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IINT_AVAL_HONO(949, "1101000110", "flat.disbursedamount.installmentinterest.aval.honorarios"), //
    IPRIN_IINT_HONO(950, "0011000010", "installmentprincipal.installmentinterest.honorarios"), //
    FLAT_DISB_IINT_AVAL_HONO_ACHG(951, "1101000111", "flat.disbursedamount.installmentinterest.aval.honorarios.percentofanothercharge"), //
    DISB_OPRIN_HONO_ACHG(952, "0100100011", "disbursedamount.outstandingprincipal.honorarios.percentofanothercharge"), //
    DISB_OPRIN_SEGO_AVAL(953, "0100101100", "disbursedamount.outstandingprincipal.seguroobrigatorio.aval"), //
    DISB_OPRIN_SEGO_AVAL_ACHG(954, "0100101101", "disbursedamount.outstandingprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OINT_AVAL_HONO(955, "1011010110", "flat.installmentprincipal.installmentinterest.outstandinginterest.aval.honorarios"), //
    DISB_IINT_OPRIN(956, "0101100000", "disbursedamount.installmentinterest.outstandingprincipal"), //
    FLAT_IPRIN_IINT_OINT_AVAL_HONO_ACHG(957, "1011010111",
            "flat.installmentprincipal.installmentinterest.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_SEGO_ACHG(958, "1101011001",
            "flat.disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio.percentofanothercharge"), //
    DISB_IINT_OPRIN_ACHG(959, "0101100001", "disbursedamount.installmentinterest.outstandingprincipal.percentofanothercharge"), //
    FLAT_DISB_IINT_OINT_SEGO(960, "1101011000", "flat.disbursedamount.installmentinterest.outstandinginterest.seguroobrigatorio"), //
    DISB_IPRIN_OPRIN_OINT_AVAL_HONO_ACHG(961, "0110110111",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    DISB_IPRIN_OPRIN_OINT_AVAL_HONO(962, "0110110110",
            "disbursedamount.installmentprincipal.outstandingprincipal.outstandinginterest.aval.honorarios"), //
    FLAT_IINT_SEGO_AVAL_HONO_ACHG(963, "1001001111", "flat.installmentinterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_AVAL(964, "1010000100", "flat.installmentprincipal.aval"), //
    FLAT_DISB_IPRIN_IINT_SEGO_AVAL_HONO_ACHG(965, "1111001111",
            "flat.disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_SEGO_AVAL_HONO_ACHG(966, "1011101111",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    DISB_OPRIN_SEGO_AVAL_HONO_ACHG(967, "0100101111",
            "disbursedamount.outstandingprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IPRIN_IINT_OPRIN_SEGO_AVAL_HONO(968, "1011101110",
            "flat.installmentprincipal.installmentinterest.outstandingprincipal.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_OINT_HONO(969, "1110010010", "flat.disbursedamount.installmentprincipal.outstandinginterest.honorarios"), //
    IPRIN_SEGO_AVAL_ACHG(970, "0010001101", "installmentprincipal.seguroobrigatorio.aval.percentofanothercharge"), //
    OPRIN_OINT_AVAL(971, "0000110100", "outstandingprincipal.outstandinginterest.aval"), //
    OPRIN_OINT_AVAL_ACHG(972, "0000110101", "outstandingprincipal.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_IPRIN_OINT_AVAL_HONO(973, "1010010110", "flat.installmentprincipal.outstandinginterest.aval.honorarios"), //
    DISB_OPRIN_OINT(974, "0100110000", "disbursedamount.outstandingprincipal.outstandinginterest"), //
    FLAT_DISB_IPRIN_IINT_SEGO_AVAL_HONO(975, "1111001110",
            "flat.disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.aval.honorarios"), //
    DISB_OPRIN_OINT_ACHG(976, "0100110001", "disbursedamount.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_HONO_ACHG(977, "1111000011",
            "flat.disbursedamount.installmentprincipal.installmentinterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_SEGO_AVAL(978, "1111001100",
            "flat.disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_IINT_SEGO_AVAL_ACHG(979, "1111001101",
            "flat.disbursedamount.installmentprincipal.installmentinterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IPRIN_OINT_AVAL_ACHG(980, "1010010101", "flat.installmentprincipal.outstandinginterest.aval.percentofanothercharge"), //
    FLAT_IPRIN_OINT_AVAL(981, "1010010100", "flat.installmentprincipal.outstandinginterest.aval"), //
    IPRIN_SEGO_AVAL_HONO(982, "0010001110", "installmentprincipal.seguroobrigatorio.aval.honorarios"), //
    IPRIN_SEGO_AVAL_HONO_ACHG(983, "0010001111", "installmentprincipal.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OINT(984, "1110010000", "flat.disbursedamount.installmentprincipal.outstandinginterest"), //
    FLAT_DISB_IPRIN_OINT_ACHG(985, "1110010001", "flat.disbursedamount.installmentprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OINT_SEGO_AVAL_HONO(986, "1110011110",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_IINT_HONO(987, "1111000010", "flat.disbursedamount.installmentprincipal.installmentinterest.honorarios"), //
    FLAT_SEGO_AVAL_ACHG(988, "1000001101", "flat.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IINT_HONO(989, "1001000010", "flat.installmentinterest.honorarios"), //
    FLAT_DISB_SEGO_ACHG(990, "1100001001", "flat.disbursedamount.seguroobrigatorio.percentofanothercharge"), //
    FLAT_IINT(991, "1001000000", "flat.installmentinterest"), //
    FLAT_DISB_SEGO_HONO_ACHG(992, "1100001011", "flat.disbursedamount.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IINT_ACHG(993, "1001000001", "flat.installmentinterest.percentofanothercharge"), //
    FLAT_DISB_SEGO_HONO(994, "1100001010", "flat.disbursedamount.seguroobrigatorio.honorarios"), //
    FLAT_SEGO_AVAL_HONO_ACHG(995, "1000001111", "flat.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_SEGO_AVAL_HONO(996, "1000001110", "flat.seguroobrigatorio.aval.honorarios"), //
    FLAT_DISB_IPRIN_IINT(997, "1111000000", "flat.disbursedamount.installmentprincipal.installmentinterest"), //
    FLAT_DISB_IPRIN_IINT_ACHG(998, "1111000001", "flat.disbursedamount.installmentprincipal.installmentinterest.percentofanothercharge"), //
    FLAT_IPRIN_OINT_AVAL_HONO_ACHG(999, "1010010111",
            "flat.installmentprincipal.outstandinginterest.aval.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OINT_HONO_ACHG(1000, "1110010011",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_OINT_SEGO_AVAL(1001, "1110011100",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_OINT_SEGO_AVAL_ACHG(1002, "1110011101",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OINT(1003, "1111010000", "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest"), //
    FLAT_DISB_IPRIN_IINT_OINT_ACHG(1004, "1111010001",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.percentofanothercharge"), //
    DISB_OPRIN_OINT_SEGO_AVAL_HONO(1005, "0100111110",
            "disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    DISB_OPRIN_OINT_SEGO_AVAL_HONO_ACHG(1006, "0100111111",
            "disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_ACHG(1007, "1000000001", "flat.percentofanothercharge"), //
    FLAT_DISB_IINT_SEGO_HONO(1008, "1101001010", "flat.disbursedamount.installmentinterest.seguroobrigatorio.honorarios"), //
    FLAT_SEGO_AVAL(1010, "1000001100", "flat.seguroobrigatorio.aval"), //
    FLAT_DISB_IINT_SEGO_ACHG(1011, "1101001001", "flat.disbursedamount.installmentinterest.seguroobrigatorio.percentofanothercharge"), //
    FLAT_HONO_ACHG(1012, "1000000011", "flat.honorarios.percentofanothercharge"), //
    FLAT_DISB_IINT_SEGO(1013, "1101001000", "flat.disbursedamount.installmentinterest.seguroobrigatorio"), //
    FLAT_IINT_OINT_SEGO_AVAL_ACHG(1014, "1001011101",
            "flat.installmentinterest.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    FLAT_IINT_OINT_HONO(1015, "1001010010", "flat.installmentinterest.outstandinginterest.honorarios"), //
    FLAT_IINT_OINT_SEGO_AVAL(1016, "1001011100", "flat.installmentinterest.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_OINT_SEGO_AVAL_HONO_ACHG(1017, "1110011111",
            "flat.disbursedamount.installmentprincipal.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OINT_HONO_ACHG(1018, "1001010011", "flat.installmentinterest.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OINT_HONO(1019, "1111010010",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.honorarios"), //
    FLAT_DISB_IPRIN_IINT_OINT_HONO_ACHG(1020, "1111010011",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.honorarios.percentofanothercharge"), //
    FLAT_DISB_IPRIN_IINT_OINT_SEGO_AVAL(1021, "1111011100",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval"), //
    FLAT_DISB_IPRIN_IINT_OINT_SEGO_AVAL_ACHG(1022, "1111011101",
            "flat.disbursedamount.installmentprincipal.installmentinterest.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_OPRIN_OINT_HONO(1023, "0100110010", "disbursedamount.outstandingprincipal.outstandinginterest.honorarios"), //
    DISB_OPRIN_OINT_HONO_ACHG(1024, "0100110011",
            "disbursedamount.outstandingprincipal.outstandinginterest.honorarios.percentofanothercharge"), //
    DISB_OPRIN_OINT_SEGO_AVAL(1025, "0100111100", "disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval"), //
    DISB_OPRIN_OINT_SEGO_AVAL_ACHG(1026, "0100111101",
            "disbursedamount.outstandingprincipal.outstandinginterest.seguroobrigatorio.aval.percentofanothercharge"), //
    DISB_IINT_OPRIN_OINT(1027, "0101110000", "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest"), //
    DISB_IINT_OPRIN_OINT_ACHG(1028, "0101110001",
            "disbursedamount.installmentinterest.outstandingprincipal.outstandinginterest.percentofanothercharge"), //
    FLAT_DISB_IPRIN(1029, "1110000000", "flat.disbursedamount.installmentprincipal"), //
    FLAT(1030, "1000000000", "flat"), //
    FLAT_DISB_IINT_SEGO_HONO_ACHG(1031, "1101001011",
            "flat.disbursedamount.installmentinterest.seguroobrigatorio.honorarios.percentofanothercharge"), //
    FLAT_IINT_OINT_SEGO_AVAL_HONO_ACHG(1032, "1001011111",
            "flat.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios.percentofanothercharge"), //
    FLAT_IINT_OINT_SEGO_AVAL_HONO(1033, "1001011110", "flat.installmentinterest.outstandinginterest.seguroobrigatorio.aval.honorarios"), //
    FLAT_SEGOVOLUNTARIO(1034, "10000000001", "flat.segurovoluntarioasistencia"), //
    FLAT_HONO(1009, "1000000010", "flat.honorarios"), //
    IPRIN_SEGOVOLUNTARIO(1035, "00100000001", "installmentprincipal.segurovoluntarioasistencia"), //
    SEGO_SEGOVOLUNTARIO(1036, "00000010001", "seguroobrigatorio.segurovoluntarioasistencia"), //
    IPRIN_SEGO_SEGOVOLUNTARIO(1037, "00100010001", "installmentprincipal.seguroobrigatorio.segurovoluntarioasistencia"), //
    AVAL_SEGOVOLUNTARIO(1038, "00000001001", "aval.segurovoluntarioasistencia"), //
    IPRIN_AVAL_SEGOVOLUNTARIO(1039, "00100001001", "installmentprincipal.aval.segurovoluntarioasistencia"), //
    SEGO_AVAL_SEGOVOLUNTARIO(1040, "00000011001", "seguroobrigatorio.aval.segurovoluntarioasistencia"),  //
    IPRIN_SEGO_AVAL_SEGOVOLUNTARIO(1041, "00100011001", "installmentprincipal.seguroobrigatorio.aval.segurovoluntarioasistencia"), //
    IINT_SEGOVOLUNTARIO(1042, "00010000001", "installmentinterest.segurovoluntarioasistencia"),  //
    IPRIN_IINT_SEGOVOLUNTARIO(1043, "00110000001", "installmentprincipal.installmentinterest.segurovoluntarioasistencia"), //
    IINT_SEGO_SEGOVOLUNTARIO(1044, "00010010001", "installmentinterest.seguroobrigatorio.segurovoluntarioasistencia"),  //
    IPRIN_IINT_SEGO_SEGOVOLUNTARIO(1045, "00110010001", "installmentprincipal.installmentinterest.seguroobrigatorio.segurovoluntarioasistencia"), //
    IINT_AVAL_SEGOVOLUNTARIO(1046, "00010001001", "installmentinterest.aval.segurovoluntarioasistencia"),  //
    IPRIN_IINT_AVAL_SEGOVOLUNTARIO(1047, "00110001001", "installmentprincipal.installmentinterest.aval.segurovoluntarioasistencia"), //
    IINT_SEGO_AVAL_SEGOVOLUNTARIO(1048, "00010011001", "installmentinterest.seguroobrigatorio.aval.segurovoluntarioasistencia"),  //
    IPRIN_IINT_SEGO_AVAL_SEGOVOLUNTARIO(1049, "00110011001", "installmentprincipal.installmentinterest.seguroobrigatorio.aval.segurovoluntarioasistencia"), //
    SEGOVOLUNTARIO(1050,"00000000001", "segurovoluntarioasistencia"), //

    ;

    private final Integer value;
    private final String byteRepresentation;
    private final String code;

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static Object[] validValuesForLoan() {
        return new Integer[] { ChargeCalculationType.FLAT_AMOUNT.getValue(), ChargeCalculationType.PERCENT_OF_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_INTEREST.getValue(), ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_ANOTHER_CHARGE.getValue(),
                ChargeCalculationType.AMOUNT_FROM_EXTERNAL_CALCULATION.getValue() };
    }

    public static Object[] validValuesForSavings() {
        return new Integer[] { ChargeCalculationType.FLAT_AMOUNT.getValue(), ChargeCalculationType.PERCENT_OF_AMOUNT.getValue() };
    }

    public static Object[] validValuesForShares() {
        return new Integer[] { ChargeCalculationType.FLAT_AMOUNT.getValue(), ChargeCalculationType.PERCENT_OF_AMOUNT.getValue() };
    }

    public static Object[] validValuesForClients() {
        return new Integer[] { ChargeCalculationType.FLAT_AMOUNT.getValue() };
    }

    public static Object[] validValuesForShareAccountActivation() {
        return new Integer[] { ChargeCalculationType.FLAT_AMOUNT.getValue() };
    }

    public static Object[] validValuesForTrancheDisbursement() {
        return new Integer[] { ChargeCalculationType.FLAT_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getValue() };
    }

    public static ChargeCalculationType fromInt(final Integer chargeCalculation) {
        return Arrays.asList(ChargeCalculationType.values()).stream().filter(obj -> obj.getValue().compareTo(chargeCalculation) == 0)
                .findFirst().orElse(ChargeCalculationType.INVALID);
    }

    public boolean isFlat() {
        return this.value.equals(ChargeCalculationType.FLAT.getValue())
                || this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.FLAT.getIndex()) == '1';
    }

    public boolean isPercentageOfDisbursement() {
        return this.value.equals(ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getValue())
                || this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.DISBURSED_AMOUNT.getIndex()) == '1';
    }

    public boolean isPercentageOfInstallmentPrincipal() {
        return this.value.equals(ChargeCalculationType.PERCENT_OF_AMOUNT.getValue())
                || this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.PRINCIPAL_INSTALLMENT.getIndex()) == '1';
    }

    public boolean isPercentageOfInstallmentPrincipalAndInterest() {
        return this.value.equals(ChargeCalculationType.PERCENT_OF_AMOUNT_AND_INTEREST.getValue())
                || (this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.PRINCIPAL_INSTALLMENT.getIndex()) == '1'
                        && this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.INTEREST_INSTALLMENT.getIndex()) == '1');
    }

    public boolean isPercentageOfInstallmentInterest() {
        return this.value.equals(ChargeCalculationType.PERCENT_OF_INTEREST.getValue())
                || this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.INTEREST_INSTALLMENT.getIndex()) == '1';
    }

    public boolean isPercentageOfOutstandingPrincipal() {
        return this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.OUTSTANDING_PRINCIPAL.getIndex()) == '1';
    }

    public boolean isPercentageOfOutstandingInterest() {
        return this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.OUTSTANDING_INTEREST.getIndex()) == '1';
    }

    public boolean isPercentageOfInsurance() {
        return this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.SEGURO_OBRIGATORIO.getIndex()) == '1';
    }

    public boolean isPercentageOfAval() {
        return this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.AVAL.getIndex()) == '1' &&
        this.equals(DISB_AVAL) && isPercentageOfDisbursement();
    }

    public boolean isPercentageOfHonorarios() {
        return this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.HOORARIOS.getIndex()) == '1'
                && this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.FLAT.getIndex()) == '1';
    }

    public boolean isPercentageOfAnotherCharge() {
        return this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.PERCENT_OF_ANOTHER_CHARGE.getIndex()) == '1'
                || this.value.equals(ChargeCalculationType.PERCENT_OF_ANOTHER_CHARGE.getValue());
    }

    public boolean isAllowedSavingsChargeCalculationType() {
        return isFlat() || isPercentageOfInstallmentPrincipal();
    }

    public boolean isAllowedClientChargeCalculationType() {
        return isFlat();
    }

    public boolean isPercentageBased() {
        return isPercentageOfInstallmentPrincipal() || isPercentageOfInstallmentPrincipalAndInterest()
                || isPercentageOfInstallmentInterest() || isPercentageOfDisbursement() || isPercentageOfAnotherCharge();
    }

    public boolean isAmountFromExternal() {
        return this.value.equals(ChargeCalculationType.AMOUNT_FROM_EXTERNAL_CALCULATION.getValue());
    }

    public boolean isVoluntaryInsurance() {
        String val = this.byteRepresentation;
        if (this.byteRepresentation.length() == 10) {
            // Adding a leading 0 because voluntaryInsurance has position 10 while byterepresentation of all codes is 10
            // character
            val = this.byteRepresentation + "0";
        }
        return val.charAt(ChargeCalculationTypeBaseItemsEnum.SEGURO_VOLUNTARIO.getIndex()) == '1'
                && val.charAt(ChargeCalculationTypeBaseItemsEnum.FLAT.getIndex()) == '1';
    }

    // is voluntary or mandatory insurance
    public boolean isInsurance() {
        return isPercentageOfInsurance() || isVoluntaryInsurance();
    }

    public boolean isFlatHono() {
        return isFlat() && this.byteRepresentation.charAt(ChargeCalculationTypeBaseItemsEnum.HOORARIOS.getIndex()) == '1';
    }

    public boolean isFlatMandatoryInsurance() {
        return isPercentageOfInsurance() && isFlat();
    }

    public boolean isPercentageBasedMandatoryInsurance() {
        // Any charge which is percentage based and distributed among the installments will be added here
        return this.equals(DISB_SEGO) && isPercentageOfInsurance() && (isPercentageOfDisbursement());
    }

    public boolean isCustomPercentageBasedDistributedCharge() {
        // Charge is distributed among the installments
        return isPercentageBasedMandatoryInsurance() || isPercentageOfAval();
    }

    public boolean isCustomPercentageOfOutstandingPrincipalCharge() {
        // Charge is distributed among the installments
        return isPercentageOfOutstandingPrincipal() && this.equals(ChargeCalculationType.OPRIN_SEGO);
    }

    public boolean isTermCharge() {
        return this.equals(ChargeCalculationType.ACHG);
    }

    public boolean isMandatoryInsuranceCharge() {
        return isFlatMandatoryInsurance() || isPercentageBasedMandatoryInsurance() || isCustomPercentageOfOutstandingPrincipalCharge();
    }

}
