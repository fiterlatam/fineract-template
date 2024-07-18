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
package org.apache.fineract.portfolio.client.domain.search;

import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchingClientRepositoryImpl implements SearchingClientRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Page<SearchedClient> searchByText(String searchText, Pageable pageable, String officeHierarchy) {
        return searchByTextWithJdbcTemplate(searchText, pageable, officeHierarchy);
    }

    private Page<SearchedClient> searchByTextWithJdbcTemplate(String searchText, Pageable pageable, String officeHierarchy) {
        String hierarchyLikeValue = officeHierarchy + "%";
        String searchLikeValue = "";
        if (StringUtils.isNotBlank(searchText)) {
            searchLikeValue = "%" + searchText.toLowerCase(Locale.UK) + "%";
        }

        String baseSql = """
                FROM m_client c
                JOIN m_office o ON c.office_id = o.id
                LEFT JOIN m_blocking_reason_setting brs ON c.blocking_reason_id = brs.id
                LEFT JOIN campos_cliente_empresas cce ON c.id = cce.client_id
                LEFT JOIN campos_cliente_persona ccp ON c.id = ccp.client_id
                WHERE o.hierarchy LIKE ?
                """;

        if ((StringUtils.isNotBlank(searchText))) {
            baseSql += """
                    AND (LOWER(c.display_name) LIKE ? OR LOWER(c.external_id) LIKE ? OR c.account_no LIKE ?
                    OR LOWER(o.name) LIKE ? OR c.mobile_no LIKE ? OR LOWER(cce."NIT") LIKE ? OR LOWER(ccp."Cedula") LIKE ?)
                    """;
        }

        String countSql = "SELECT COUNT(*) " + baseSql;

        String sortSql = pageable.getSort().stream().map(order -> " " + order.getProperty() + " " + order.getDirection().name())
                .collect(Collectors.joining(", "));
        if (sortSql.isEmpty()) {
            sortSql = "c.id DESC";
        }

        String querySql = """
                SELECT c.id AS id,
                       c.display_name AS displayName,
                       c.firstname AS firstName,
                       c.middlename AS middleName,
                       cce."NIT" AS nit,
                       ccp."Cedula" AS cedula,
                       c.lastName AS lastName,
                       c.second_lastname AS secondLastName,
                       c.external_id AS externalId,
                       c.account_no AS accountNumber,
                       o.id AS officeId,
                       o.name AS officeName,
                       c.mobile_no AS mobileNo,
                       CASE WHEN c.blocking_reason_id IS NOT NULL THEN 900 ELSE c.status_enum END AS status,
                       c.activation_date AS activationDate,
                       c.created_on_utc AS createdDate,
                       CASE WHEN cce.client_id IS NOT NULL THEN 'Empresa' ELSE 'Persona' END AS clientType,
                       CASE WHEN cce."NIT" IS NOT NULL THEN cce."NIT" ELSE ccp."Cedula" END AS cedularornit
                """ + baseSql + " ORDER BY " + sortSql + " LIMIT ? OFFSET ?";

        RowMapper<SearchedClient> rowMapper = (rs, rowNum) -> {
            Date activationDate = rs.getDate("activationDate");
            String firstName = rs.getString("firstName");
            String middleName = rs.getString("middleName");
            String lastName = rs.getString("lastName");
            String secondLastName = rs.getString("secondLastName");
            String fullName = Objects.toString(firstName, "") + " " + Objects.toString(middleName, "") + " "
                    + Objects.toString(lastName, "") + " " + Objects.toString(secondLastName, "");
            String name = StringUtils.isBlank(fullName) ? rs.getString("displayName") : fullName;
            String nit = rs.getString("nit");
            String cedula = rs.getString("cedula");
            String cedulaOrNit = rs.getString("cedularornit");
            String clientType = rs.getString("clientType");
            return new SearchedClient(rs.getLong("id"), name, getExternalId(rs.getString("externalId")), rs.getString("accountNumber"),
                    rs.getLong("officeId"), rs.getString("officeName"), rs.getString("mobileNo"), rs.getInt("status"),
                    activationDate != null ? activationDate.toLocalDate() : null, rs.getObject("createdDate", OffsetDateTime.class),
                    cedulaOrNit, clientType);
        };

        List<SearchedClient> clients = jdbcTemplate.query(querySql, rowMapper,
                getQueryParams(hierarchyLikeValue, searchLikeValue, pageable));

        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, getCountParams(hierarchyLikeValue, searchLikeValue));

        return new PageImpl<>(clients, pageable, total);
    }

    private ExternalId getExternalId(String externalId) {
        return externalId == null ? ExternalId.empty() : new ExternalId(externalId);
    }

    private Object[] getQueryParams(String hierarchyLikeValue, String searchLikeValue, Pageable pageable) {
        if (StringUtils.isBlank(searchLikeValue)) {
            return new Object[] { hierarchyLikeValue, pageable.getPageSize(), pageable.getOffset() };
        }
        return new Object[] { hierarchyLikeValue, searchLikeValue, searchLikeValue, searchLikeValue, searchLikeValue, searchLikeValue,
                searchLikeValue, searchLikeValue, pageable.getPageSize(), pageable.getOffset() };
    }

    private Object[] getCountParams(String hierarchyLikeValue, String searchLikeValue) {
        if ((StringUtils.isBlank(searchLikeValue))) {
            return new Object[] { hierarchyLikeValue };
        }
        return new Object[] { hierarchyLikeValue, searchLikeValue, searchLikeValue, searchLikeValue, searchLikeValue, searchLikeValue,
                searchLikeValue, searchLikeValue };
    }
}
