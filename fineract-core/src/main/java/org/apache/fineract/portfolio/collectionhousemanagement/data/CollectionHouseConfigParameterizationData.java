package org.apache.fineract.portfolio.collectionhousemanagement.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "instance")
public class CollectionHouseConfigParameterizationData {

    private Long id;
    private String collectionName;
    private String collectionNit;
    private String collectionCode;
    private Integer collectionVerificationCode;
}
