package org.apache.fineract.portfolio.collectionhousemanagement.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseConfigParameterizationData;

@Entity
@Table(name = "m_collection_house_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "instance")
public class CollectionHouseConfiguration extends AbstractAuditableWithUTCDateTimeCustom {

    @Column(name = "collection_name")
    private String collectionName;

    @Column(name = "collection_nit")
    private String collectionNit;

    @Column(name = "collection_code")
    private String collectionCode;

    @Column(name = "collection_verification_code")
    private Integer collectionVerificationCode;

    public CollectionHouseConfigParameterizationData toData() {
        return CollectionHouseConfigParameterizationData.instance(getId(), this.collectionName, this.collectionNit, this.collectionCode,
                this.collectionVerificationCode);
    }

    public static CollectionHouseConfiguration createNewCollectionHouse(JsonCommand jsonCommand) {
        String collectionName = jsonCommand.stringValueOfParameterNamed("collectionName");
        String collectionNit = jsonCommand.stringValueOfParameterNamed("collectionNit");
        String collectionCode = jsonCommand.stringValueOfParameterNamed("collectionCode");
        Integer collectionVerificationCode = jsonCommand.integerValueOfParameterNamed("collectionVerificationCode");
        return new CollectionHouseConfiguration(collectionName, collectionNit, collectionCode, collectionVerificationCode);
    }

}
