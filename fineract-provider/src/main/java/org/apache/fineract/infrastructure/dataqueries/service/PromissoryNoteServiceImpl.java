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
package org.apache.fineract.infrastructure.dataqueries.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateFive;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateFour;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateOne;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateSix;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateThree;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateTwo;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromissoryNoteServiceImpl implements PromissoryNoteService {

    private final PromissoryNoteTemplateOne promissoryNoteTemplateOne;
    private final PromissoryNoteTemplateTwo promissoryNoteTemplateTwo;
    private final PromissoryNoteTemplateThree promissoryNoteTemplateThree;
    private final PromissoryNoteTemplateFour promissoryNoteTemplateFour;
    private final PromissoryNoteTemplateFive promissoryNoteTemplateFive;
    private final PromissoryNoteTemplateSix promissoryNoteTemplateSix;

    @Override
    public String generatePromissoryNote(String type, String json) {
        return redirectPromissoryNote(type, json);
    }

    private String redirectPromissoryNote(String type, String json) {

        return switch (type) {
            case "1" -> promissoryNoteTemplateOne.generatePdf(json);
            case "2" -> promissoryNoteTemplateTwo.generatePdf(json);
            case "3" -> promissoryNoteTemplateThree.generatePdf(json);
            case "4" -> promissoryNoteTemplateFour.generatePdf(json);
            case "5" -> promissoryNoteTemplateFive.generatePdf(json);
            case "6" -> promissoryNoteTemplateSix.generatePdf(json);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}
