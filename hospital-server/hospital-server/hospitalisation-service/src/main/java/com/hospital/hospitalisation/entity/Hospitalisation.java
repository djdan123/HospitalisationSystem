package com.hospital.hospitalisation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospitalisations", indexes = {
        @Index(name = "idx_hosp_patient", columnList = "patient_id"),
        @Index(name = "idx_hosp_statut", columnList = "statut")
})
public class Hospitalisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "date_admission", nullable = false)
    private LocalDateTime dateAdmission;

    @Column(name = "date_sortie")
    private LocalDateTime dateSortie;

    @Column(length = 500)
    private String motif;

    @Column(nullable = false, length = 30)
    private String statut = "EN_COURS";

    @Column(name = "chambre_id")
    private Long chambreId;

    @Column(name = "lit_id")
    private Long litId;

    @Column(name = "numero_chambre", length = 20)
    private String numeroChambre;

    @Column(name = "numero_lit", length = 20)
    private String numeroLit;

    @Column(length = 1000)
    private String observations;

    @PrePersist
    protected void onCreate() {
        if (dateAdmission == null) dateAdmission = LocalDateTime.now();
        if (statut == null) statut = "EN_COURS";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public LocalDateTime getDateAdmission() { return dateAdmission; }
    public void setDateAdmission(LocalDateTime dateAdmission) { this.dateAdmission = dateAdmission; }
    public LocalDateTime getDateSortie() { return dateSortie; }
    public void setDateSortie(LocalDateTime dateSortie) { this.dateSortie = dateSortie; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public Long getChambreId() { return chambreId; }
    public void setChambreId(Long chambreId) { this.chambreId = chambreId; }
    public Long getLitId() { return litId; }
    public void setLitId(Long litId) { this.litId = litId; }
    public String getNumeroChambre() { return numeroChambre; }
    public void setNumeroChambre(String numeroChambre) { this.numeroChambre = numeroChambre; }
    public String getNumeroLit() { return numeroLit; }
    public void setNumeroLit(String numeroLit) { this.numeroLit = numeroLit; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
