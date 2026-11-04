package com.bateriasapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "nombre")
    private String nombre;
    @Column(name = "tipo")
    private String tipo;
    // Integer (no int): en la BD amperaje y cca son NULL para claxon y luces.
    @Column(name = "amperaje")
    private Integer amperaje;
    @Column(name = "cca")
    private Integer cca;
    @Column(name = "tipo_caja")
    private String tipoCaja;
    @Column(name = "polaridad")
    private String polaridad;
    @Column(name = "precio_base")
    private BigDecimal precioBase;
    @Column(name = "condicion")
    private String condicion;
    @Column(name = "disponible")
    private boolean disponible;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getAmperaje() {
        return amperaje;
    }

    public void setAmperaje(Integer amperaje) {
        this.amperaje = amperaje;
    }

    public Integer getCca() {
        return cca;
    }

    public void setCca(Integer cca) {
        this.cca = cca;
    }

    public String getTipoCaja() {
        return tipoCaja;
    }

    public void setTipoCaja(String tipoCaja) {
        this.tipoCaja = tipoCaja;
    }

    public String getPolaridad() {
        return polaridad;
    }

    public void setPolaridad(String polaridad) {
        this.polaridad = polaridad;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase;
    }

    public String getCondicion() {
        return condicion;
    }

    public void setCondicion(String condicion) {
        this.condicion = condicion;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
}
