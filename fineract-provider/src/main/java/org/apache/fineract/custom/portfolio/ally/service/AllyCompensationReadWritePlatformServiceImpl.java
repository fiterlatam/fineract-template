package org.apache.fineract.custom.portfolio.ally.service;

import jakarta.persistence.PersistenceException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fineract.custom.portfolio.ally.data.AllySettlementCompansationCollectionData;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllySettlementData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensation;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensationRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AllyCompensationReadWritePlatformServiceImpl implements AllyCompensationReadWritePlatformService {

    private static final Log log = LogFactory.getLog(AllyCompensationReadWritePlatformServiceImpl.class);
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;

    @Autowired
    public AllyCompensationReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.context = context;
    }

    private static final class AllyCompensationSettlementRowMaper implements RowMapper<AllySettlementCompansationCollectionData> {

        public String schema(String purchaseCondition, String collectionCondition) {
            return " distinct start_date, end_date, cca.id as id, cca.nit , cca.company_name , cca.account_number ,bank_name ,account_type, account_number,\n"
                    + "purchase_amount, comission_amount, va_comission_amount, net_purchase_amount, collection_amount, coalesce (net_purchase_amount-collection_amount,0) as compensation_amount, cca.last_job_run as lastCollectionDate, cca.last_job_run_purchase as lastpurchaseDate \n"
                    + "from custom.c_client_ally cca \n"
                    + "inner join (select id,code_value as bank_name from m_code_value ) mcv on mcv.id= cca.bank_entity_id \n"
                    + "inner join (select id,code_value as account_type from m_code_value ) mcv1 on mcv1.id= cca.account_type_id\n"
                    + "inner join (select  min(purchase_date) as start_date,client_ally_id,company_name, sum(maps.buy_amount) as purchase_amount,\n"
                    + "sum(maps.amount_comission) as comission_amount , sum(maps.amount_va_commision) as va_comission_amount, sum(maps.amount_to_pay) as net_purchase_amount\n"
                    + "from m_ally_purchase_settlement maps " + purchaseCondition + " \n"
                    + " group by maps.client_ally_id , company_name) maps on maps.client_ally_id = cca.id\n"
                    + "inner join (select  max(macs.collection_date) end_date,client_ally_id,company_name, sum(macs.collection_amount) as collection_amount \n"
                    + "from m_ally_collection_settlement macs " + collectionCondition + " \n"
                    + " group by macs.client_ally_id ,company_name) macs on macs.client_ally_id= cca.id";
        }

        @Override
        public AllySettlementCompansationCollectionData mapRow(ResultSet rs, int rowNum) throws SQLException {
            return AllySettlementCompansationCollectionData.builder().startDate(rs.getString("start_date"))
                    .endDate(rs.getString("end_date")).clientAllyId(rs.getLong("id")).nit(rs.getString("nit"))
                    .companyName(rs.getString("company_name")).accountNumber(rs.getString("account_number"))
                    .accountType(rs.getString("account_type")).bankName(rs.getString("bank_name"))
                    .purchaseAmount(rs.getBigDecimal("purchase_amount")).comissionAmount(rs.getBigDecimal("comission_amount"))
                    .vaComissionAmount(rs.getBigDecimal("va_comission_amount")).netPurchaseAmount(rs.getBigDecimal("net_purchase_amount"))
                    .collectionAmount(rs.getBigDecimal("collection_amount")).compensationAmount(rs.getBigDecimal("compensation_amount"))
                    .lastCollectionDate(rs.getString("lastCollectionDate")).lastPurchaseDate(rs.getString("lastPurchaseDate")).build();
        }
    }

    @Override
    public List<AllySettlementCompansationCollectionData> getListCompensationSettlement() {
        final AllyCompensationReadWritePlatformServiceImpl.AllyCompensationSettlementRowMaper rm = new AllyCompensationReadWritePlatformServiceImpl.AllyCompensationSettlementRowMaper();
        final String sql = "select" + rm.schema("", "") + " ";
        return this.jdbcTemplate.query(sql, rm);
    }

    @Override
    public Optional<AllySettlementCompansationCollectionData> getCompensationSettlementByNit(String nit, LocalDate startDate,
            LocalDate endDate) {
        try {
            final AllyCompensationReadWritePlatformServiceImpl.AllyCompensationSettlementRowMaper rm = new AllyCompensationReadWritePlatformServiceImpl.AllyCompensationSettlementRowMaper();
            String purchaseCondition = "where purchase_date <= '" + endDate + "' and purchase_date >= '" + startDate + "'";
            String collectionCondtion = "where collection_date <= '" + endDate + "' and collection_date >= '" + startDate + "'";
            final String sql = "select" + rm.schema(purchaseCondition, collectionCondtion) + " Where nit = ?";
            return Optional.ofNullable(this.jdbcTemplate.queryForObject(sql, rm, nit));
        } catch (EmptyResultDataAccessException e) {
            log.info("result not found " + e);
            return Optional.ofNullable(null);
        }
    }

    @Autowired
    private AllyCompensationRepository repository;

    @Override
    public void create(AllyCompensation allyCompensation) {
        try {
            this.context.authenticatedUser();
            repository.saveAndFlush(allyCompensation);
        } catch (final PersistenceException e) {
            throw new PlatformDataIntegrityException("error.msg.allyCollection.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.");
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long id) {
        try {
            this.context.authenticatedUser();
            AllyCompensation allyCompensation = new AllyCompensation();
            final Boolean status = command.booleanObjectValueOfParameterNamed("status");
            Optional<AllyCompensation> dbEntity = repository.findById(id);
            if (dbEntity.isPresent()) {
                allyCompensation = dbEntity.get();
                allyCompensation.setSettlementStatus(status);
                repository.save(allyCompensation);
            }
            return new CommandProcessingResultBuilder().withEntityId(allyCompensation.getId()).build();
        } catch (final PersistenceException e) {
            throw new PlatformDataIntegrityException("error.msg.allyCompensation.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.");
        }
    }

    private static final class ClientAllySettlementMaper implements RowMapper<ClientAllySettlementData> {

        public String schema() {
            return " cca.id, cca.nit, \n"
                    + "last_job_run as lastClientCollectionJobRun, last_job_run_purchase as lastClientPurchaseJobRun\n"
                    + ",min(maps.purchase_date) as purchaseDate, max(collection_date) as collectionDate\n"
                    + "from custom.c_client_ally cca \n" + "inner join m_ally_collection_settlement macs on macs.client_ally_id = cca.id\n"
                    + "inner join m_ally_purchase_settlement maps on maps.client_ally_id = cca.id where last_job_run_purchase is not null and last_job_run is not null \n"
                    + "group by cca.id";
        }

        @Override
        public ClientAllySettlementData mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ClientAllySettlementData.builder().clientAllyId(rs.getLong("id")).nit(rs.getString("nit"))
                    .collectionDate(rs.getString("collectionDate")).purchaseDate(rs.getString("purchaseDate"))
                    .lastClientCollectionJobRun(rs.getString("lastClientCollectionJobRun"))
                    .lastClientPurchaseJobRun(rs.getString("lastClientPurchaseJobRun")).build();
        }
    }

    @Override
    public List<ClientAllySettlementData> getClientAllySettlement() {
        final AllyCompensationReadWritePlatformServiceImpl.ClientAllySettlementMaper rm = new AllyCompensationReadWritePlatformServiceImpl.ClientAllySettlementMaper();
        final String sql = "select" + rm.schema() + " ";
        return this.jdbcTemplate.query(sql, rm);
    }

}
