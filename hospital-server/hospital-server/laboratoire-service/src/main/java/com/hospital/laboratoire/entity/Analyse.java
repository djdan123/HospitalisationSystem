package com.hospital.laboratoire.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name = "analyses")
public class Analyse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "patient_id", nullable = false) private Long patientId;
    @Column(name = "type_analyse") private String typeAnalyse;
    @Column(name = "date_demande") private LocalDateTime dateDemande;
    @Column(name = "date_prelevement") private LocalDateTime datePrelevement;
    private String statut = "DEMANDEE";
    private String observations;
    @PrePersist void onCreate() { if (dateDemande == null) dateDemande = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getTypeAnalyse() { return typeAnalyse; } public void setTypeAnalyse(String typeAnalyse) { this.typeAnalyse = typeAnalyse; }
    public LocalDateTime getDateDemande() { return dateDemande; } public void setDateDemande(LocalDateTime d) { this.dateDemande = d; }
    public LocalDateTime getDatePrelevement() { return datePrelevement; } public void setDatePrelevement(LocalDateTime d) { this.datePrelevement = d; }
    public String getStatut() { return statut; } public void setStatut(String statut) { this.statut = statut; }
    public String getObservations() { return observations; } public void setObservations(String observations) { this.observations = observations; }
}
