package org.apache.fineract.portfolio.collectionhousemanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

@Entity
@Table(name = "m_collection_house_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "instance")
public class ColletionHouseHistory extends AbstractAuditableWithUTCDateTimeCustom {

    @Column(name = "client_account_number")
    private String clientAccountNumber;

    @Column(name = "collection_nit")
    private String collectionNit;

    @Column(name = "collection_house_code")
    private String collectionCode;

}
