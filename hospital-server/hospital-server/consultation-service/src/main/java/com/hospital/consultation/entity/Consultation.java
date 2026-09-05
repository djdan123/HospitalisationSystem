package com.hospital.consultation.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "consultations")
public class Consultation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "patient_id", nullable = false) private Long patientId;
    @Column(name = "medecin_id") private Long medecinId;
    @Column(name = "date_consultation") private LocalDateTime dateConsultation;
    private String motif;
    private String diagnostic;
    private String observations;
    private String prescription;
    private String statut = "PLANIFIEE";
    @PrePersist void onCreate() { if (dateConsultation == null) dateConsultation = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getMedecinId() { return medecinId; } public void setMedecinId(Long medecinId) { this.medecinId = medecinId; }
    public LocalDateTime getDateConsultation() { return dateConsultation; } public void setDateConsultation(LocalDateTime d) { this.dateConsultation = d; }
    public String getMotif() { return motif; } public void setMotif(String motif) { this.motif = motif; }
    public String getDiagnostic() { return diagnostic; } public void setDiagnostic(String diagnostic) { this.diagnostic = diagnostic; }
    public String getObservations() { return observations; } public void setObservations(String observations) { this.observations = observations; }
    public String getPrescription() { return prescription; } public void setPrescription(String prescription) { this.prescription = prescription; }
    public String getStatut() { return statut; } public void setStatut(String statut) { this.statut = statut; }
}
