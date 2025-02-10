
package org.apache.fineract.portfolio.collectionhousemanagement.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "clientAccountNo", "nit", "collectionHouseCode" })

public class CollectionHouseUpdate {

    @JsonProperty("clientAccountNo")
    private String clientAccountNo;
    @JsonProperty("nit")
    private String nit;
    @JsonProperty("collectionHouseCode")
    private String collectionHouseCode;

    @JsonProperty("clientAccountNo")
    public String getClientAccountNo() {
        return clientAccountNo;
    }

    @JsonProperty("clientAccountNo")
    public void setClientAccountNo(String clientAccountNo) {
        this.clientAccountNo = clientAccountNo;
    }

    @JsonProperty("nit")
    public String getNit() {
        return nit;
    }

    @JsonProperty("nit")
    public void setNit(String nit) {
        this.nit = nit;
    }

    @JsonProperty("collectionHouseCode")
    public String getCollectionHouseCode() {
        return collectionHouseCode;
    }

    @JsonProperty("collectionHouseCode")
    public void setCollectionHouseCode(String collectionHouseCode) {
        this.collectionHouseCode = collectionHouseCode;
    }

}
