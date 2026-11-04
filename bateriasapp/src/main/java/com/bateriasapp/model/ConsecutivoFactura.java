package com.bateriasapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila única (id = 1) con el último número de factura emitido. */
@Entity
@Table(name = "consecutivos_factura")
public class ConsecutivoFactura {
    @Id
    private Integer id;
    @Column(name = "ultimo_numero")
    private Integer ultimoNumero;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUltimoNumero() {
        return ultimoNumero;
    }

    public void setUltimoNumero(Integer ultimoNumero) {
        this.ultimoNumero = ultimoNumero;
    }
}
