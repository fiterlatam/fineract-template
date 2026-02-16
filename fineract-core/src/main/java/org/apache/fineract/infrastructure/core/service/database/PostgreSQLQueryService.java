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
package org.apache.fineract.infrastructure.core.service.database;

import static java.lang.String.format;

import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component
public class PostgreSQLQueryService implements DatabaseQueryService {

    private final DatabaseTypeResolver databaseTypeResolver;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PostgreSQLQueryService(DatabaseTypeResolver databaseTypeResolver, JdbcTemplate jdbcTemplate) {
        this.databaseTypeResolver = databaseTypeResolver;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isSupported() {
        return databaseTypeResolver.isPostgreSQL();
    }

    @Override
    public boolean isTablePresent(DataSource dataSource, String tableName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer result = jdbcTemplate.queryForObject(format("SELECT COUNT(table_name) " + "FROM information_schema.tables "
                + "WHERE table_schema = 'public' " + "AND table_name = '%s';", tableName), Integer.class);
        return Objects.equals(result, 1);
    }

    @Override
    public SqlRowSet getTableColumns(DataSource dataSource, String tableName) {

        String sqlEntity = String.format("""
                SELECT application_table_name
                FROM public.x_registered_table
                WHERE registered_table_name = '%s'
                """, tableName);

        String entityName = this.jdbcTemplate.queryForObject(sqlEntity, String.class);
        String columnName = "";

        if (entityName != null && !entityName.isEmpty()) {

            // Remueve prefijo m_
            if (entityName.startsWith("m_")) {
                entityName = entityName.substring(2);
            }
            columnName = entityName + "_id";
        }

        this.jdbcTemplate.setDataSource(dataSource);

        String sql = String.format("""
                SELECT column_name,
                       is_nullable,
                       data_type,
                       coalesce(character_maximum_length, numeric_precision, datetime_precision) AS max_length,
                       column_name = '%s' AS column_key,
                       col_description(
                           (quote_ident(table_schema) || '.' || quote_ident(table_name))::regclass::oid,
                           ordinal_position
                       ) AS column_comment
                FROM information_schema.columns
                WHERE table_catalog = current_catalog
                  AND table_schema = current_schema
                  AND table_name = '%s'
                ORDER BY ordinal_position
                """, columnName, tableName);

        final SqlRowSet columnDefinitions = jdbcTemplate.queryForRowSet(sql); // NOSONAR
        if (columnDefinitions.next()) {
            return columnDefinitions;
        } else {
            throw new IllegalArgumentException("Table " + tableName + " is not found");
        }
    }

    @Override
    public List<IndexDetail> getTableIndexes(DataSource dataSource, String tableName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String sql = "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = '" + tableName + "'";
        final SqlRowSet indexDefinitions = jdbcTemplate.queryForRowSet(sql); // NOSONAR
        if (indexDefinitions.next()) {
            return DatabaseIndexMapper.getIndexDetails(indexDefinitions);
        } else {
            throw new IllegalArgumentException("Table " + tableName + " is not found");
        }
    }
}
