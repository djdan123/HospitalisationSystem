package com.hospital.paiement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "factures")
public class Facture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id", nullable = false) private Long patientId;
    @Column(name = "numero_facture", unique = true) private String numeroFacture;
    @Column(name = "montant_total") private Double montantTotal = 0.0;
    @Column(name = "montant_paye") private Double montantPaye = 0.0;
    @Column private String statut = "IMPAYEE";
    @Column(name = "date_creation") private LocalDateTime dateCreation;
    @Column private String description;

    @PrePersist void onCreate() { if (dateCreation == null) dateCreation = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }
    public Double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(Double montantTotal) { this.montantTotal = montantTotal; }
    public Double getMontantPaye() { return montantPaye; }
    public void setMontantPaye(Double montantPaye) { this.montantPaye = montantPaye; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
