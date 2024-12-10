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
package org.apache.fineract.infrastructure.codes.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeValueRepository extends JpaRepository<CodeValue, Long>, JpaSpecificationExecutor<CodeValue> {

    CodeValue findByCodeNameAndId(String codeName, Long id);

    CodeValue findByCodeNameAndLabel(String codeName, String label);

    @Query(value = """
            SELECT
               mv2.code_score AS ciudad_code_score,
               mv2.code_value AS ciudad_code_value,
               mcv.code_score AS departamento_code_score,
               mcv.code_id AS departamento_code_id,
               mcv.code_value AS departamento_code_value
            FROM m_code_value mcv
            INNER JOIN (
                SELECT *,
                CASE
                    WHEN LEFT(LPAD(code_score::TEXT, 5, '0'), 2)::INTEGER < 10 THEN
                        LEFT(LPAD(code_score::TEXT, 5, '0'), 2)::INTEGER::VARCHAR
                    ELSE
                        LEFT(LPAD(code_score::TEXT, 5, '0'), 2)
                END AS ciudadCoderScore
                FROM m_code_value mcv2
                WHERE code_id = (select * from m_code mc where mc.code_name ='Ciudad')
            ) mv2
            ON mv2.ciudadCoderScore = mcv.code_score
            WHERE mcv.code_id = (select * from m_code mc where mc.code_name ='Departamento') AND mv2.code_score = :codeScore
            """, nativeQuery = true)
    Optional<CodeValue> findCiudadAndDepartamentoData(@Param("codeScore") String codeScore);
}
