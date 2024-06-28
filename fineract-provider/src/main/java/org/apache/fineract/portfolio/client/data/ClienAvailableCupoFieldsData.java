package org.apache.fineract.portfolio.client.data;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClienAvailableCupoFieldsData {

    private Long clientId;
    private String tipo;
    private String nit;
    private String cedula;
    private BigDecimal availableCupo;
    private BigDecimal totalOutstandingPrincipalAmount;
}
