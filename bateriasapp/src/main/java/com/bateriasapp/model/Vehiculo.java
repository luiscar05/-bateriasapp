package com.bateriasapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehiculos_referencia")
public class Vehiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "marca")
    private String marca;
    @Column(name = "modelo")
    private String modelo;
    @Column(name = "anio")
    private Integer anio;
    @Column(name = "amperaje_recomendado")
    private Integer amperajeRecomendado;
    @Column(name = "cca_recomendado")
    private Integer ccaRecomendado;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getAmperajeRecomendado() {
        return amperajeRecomendado;
    }

    public void setAmperajeRecomendado(Integer amperajeRecomendado) {
        this.amperajeRecomendado = amperajeRecomendado;
    }

    public Integer getCcaRecomendado() {
        return ccaRecomendado;
    }

    public void setCcaRecomendado(Integer ccaRecomendado) {
        this.ccaRecomendado = ccaRecomendado;
    }
}
