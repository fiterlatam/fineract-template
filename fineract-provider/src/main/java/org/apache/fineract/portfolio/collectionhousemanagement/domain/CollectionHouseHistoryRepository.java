package org.apache.fineract.portfolio.collectionhousemanagement.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionHouseHistoryRepository
        extends JpaRepository<ColletionHouseHistory, Long>, JpaSpecificationExecutor<ColletionHouseHistory> {

    static final String UPDATE_COLLECTION_HISTORY = """
            delete from m_collection_house_history
            where id not in (
            select
            case
            	when mchhp.id is not null then mchhp.id
            	when mchhe.id is not null then mchhe.id
            	else '0'
            end as id
            from
            m_client mc
            join m_loan ml on ml.client_id = mc.id
            join m_loan_arrears_aging mlaa on mlaa.loan_id = ml.id
            left join campos_cliente_persona ccp on ccp.client_id = mc.id
            left join campos_cliente_empresas cce on cce.client_id = mc.id
            left join m_collection_house_history mchhp on mchhp.collection_nit = ccp."Cedula"
            left join m_collection_house_history mchhe on mchhe.collection_nit = cce."NIT"
            )
            """;

    @Query("select colletionHouseHistory from ColletionHouseHistory colletionHouseHistory where colletionHouseHistory.clientAccountNumber = :clientAccountNumber")
    Optional<ColletionHouseHistory> getCollectionHouseHistoryByAccountNo(@Param("clientAccountNumber") String clientAccountNumber);

    @Modifying
    @Query(nativeQuery = true, value = UPDATE_COLLECTION_HISTORY)
    void updateCollectionHouseHistory();
}
