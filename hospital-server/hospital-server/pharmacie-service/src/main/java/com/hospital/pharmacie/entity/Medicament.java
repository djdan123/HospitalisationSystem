package com.hospital.pharmacie.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "medicaments")
public class Medicament {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true) private String code;
    private String nom;
    private String description;
    private Integer stock = 0;
    @Column(name = "prix_unitaire") private Double prixUnitaire = 0.0;
    private String unite;
    private String statut = "DISPONIBLE";

    @Column(name = "seuil_min") private Integer seuilMin = 10;
    @Column(name = "seuil_max") private Integer seuilMax = 500;
    @Column(name = "date_peremption") private LocalDate datePeremption;
    @Column(name = "numero_lot") private String numeroLot;
    @Column(name = "is_stupefiant") private Boolean isStupefiant = false;
    private String emplacement;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(Double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public Integer getSeuilMin() { return seuilMin != null ? seuilMin : 10; }
    public void setSeuilMin(Integer seuilMin) { this.seuilMin = seuilMin; }
    public Integer getSeuilMax() { return seuilMax != null ? seuilMax : 500; }
    public void setSeuilMax(Integer seuilMax) { this.seuilMax = seuilMax; }
    public LocalDate getDatePeremption() { return datePeremption; }
    public void setDatePeremption(LocalDate datePeremption) { this.datePeremption = datePeremption; }
    public String getNumeroLot() { return numeroLot; }
    public void setNumeroLot(String numeroLot) { this.numeroLot = numeroLot; }
    public Boolean getIsStupefiant() { return isStupefiant != null && isStupefiant; }
    public void setIsStupefiant(Boolean isStupefiant) { this.isStupefiant = isStupefiant; }
    public String getEmplacement() { return emplacement; }
    public void setEmplacement(String emplacement) { this.emplacement = emplacement; }

    public boolean isStockFaible() { return getStock() <= getSeuilMin(); }
    public boolean isSurstock() { return getStock() >= getSeuilMax(); }
    public boolean isProchePeremption(int jours) {
        if (datePeremption == null) return false;
        return !datePeremption.isAfter(LocalDate.now().plusDays(jours));
    }
    public boolean isPerime() {
        return datePeremption != null && datePeremption.isBefore(LocalDate.now());
    }
}
