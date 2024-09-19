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
    private BigDecimal cupo;
    private BigDecimal availableCupo;
    private BigDecimal availableCupoAvance;
    private BigDecimal totalOutstandingPrincipalAmount;

    public ClienAvailableCupoFieldsData(Long clientId, String tipo, String nit, String cedula, BigDecimal cupo, BigDecimal availableCupo,
            BigDecimal totalOutstandingPrincipalAmount) {
        this.clientId = clientId;
        this.tipo = tipo;
        this.nit = nit;
        this.cedula = cedula;
        this.cupo = cupo;
        this.availableCupo = availableCupo;
        this.totalOutstandingPrincipalAmount = totalOutstandingPrincipalAmount;
    }
}
