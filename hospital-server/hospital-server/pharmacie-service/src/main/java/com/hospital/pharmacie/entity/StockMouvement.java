package com.hospital.pharmacie.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_mouvements")
public class StockMouvement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medicament_id", nullable = false)
    private Long medicamentId;

    @Column(name = "type_mouvement", nullable = false, length = 30)
    private String typeMouvement; // ENTREE, SORTIE, RETOUR, INVENTAIRE, DOTATION

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "stock_avant") private Integer stockAvant;
    @Column(name = "stock_apres") private Integer stockApres;
    @Column(name = "numero_lot") private String numeroLot;
    @Column(name = "date_peremption") private LocalDate datePeremption;
    private String motif;
    @Column(name = "source_dest") private String sourceDest;
    @Column(name = "patient_id") private Long patientId;
    @Column(name = "user_login") private String userLogin;
    @Column(name = "service_soins") private String serviceSoins;
    @Column(name = "double_controle") private Boolean doubleControle = false;
    private String controleur;
    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    @PrePersist
    public void prePersist() {
        if (dateMouvement == null) dateMouvement = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMedicamentId() { return medicamentId; }
    public void setMedicamentId(Long medicamentId) { this.medicamentId = medicamentId; }
    public String getTypeMouvement() { return typeMouvement; }
    public void setTypeMouvement(String typeMouvement) { this.typeMouvement = typeMouvement; }
    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }
    public Integer getStockAvant() { return stockAvant; }
    public void setStockAvant(Integer stockAvant) { this.stockAvant = stockAvant; }
    public Integer getStockApres() { return stockApres; }
    public void setStockApres(Integer stockApres) { this.stockApres = stockApres; }
    public String getNumeroLot() { return numeroLot; }
    public void setNumeroLot(String numeroLot) { this.numeroLot = numeroLot; }
    public LocalDate getDatePeremption() { return datePeremption; }
    public void setDatePeremption(LocalDate datePeremption) { this.datePeremption = datePeremption; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getSourceDest() { return sourceDest; }
    public void setSourceDest(String sourceDest) { this.sourceDest = sourceDest; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getUserLogin() { return userLogin; }
    public void setUserLogin(String userLogin) { this.userLogin = userLogin; }
    public String getServiceSoins() { return serviceSoins; }
    public void setServiceSoins(String serviceSoins) { this.serviceSoins = serviceSoins; }
    public Boolean getDoubleControle() { return doubleControle != null && doubleControle; }
    public void setDoubleControle(Boolean doubleControle) { this.doubleControle = doubleControle; }
    public String getControleur() { return controleur; }
    public void setControleur(String controleur) { this.controleur = controleur; }
    public LocalDateTime getDateMouvement() { return dateMouvement; }
    public void setDateMouvement(LocalDateTime dateMouvement) { this.dateMouvement = dateMouvement; }
}
