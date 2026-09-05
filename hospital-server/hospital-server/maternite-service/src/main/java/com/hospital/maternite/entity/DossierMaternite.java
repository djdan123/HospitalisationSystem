package com.hospital.maternite.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name = "dossiers_maternite")
public class DossierMaternite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "patient_id", nullable = false) private Long patientId;
    @Column(name = "date_ouverture") private LocalDateTime dateOuverture;
    @Column(name = "date_dernieres_regles") private LocalDate dateDernieresRegles;
    @Column(name = "date_prevue_accouchement") private LocalDate datePrevueAccouchement;
    @Column(name = "nombre_grossesses") private Integer nombreGrossesses = 1;
    @Column(name = "groupe_sanguin") private String groupeSanguin;
    private String statut = "OUVERT";
    private String observations;
    @PrePersist void onCreate() { if (dateOuverture == null) dateOuverture = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long patientId) { this.patientId = patientId; }
    public LocalDateTime getDateOuverture() { return dateOuverture; } public void setDateOuverture(LocalDateTime d) { this.dateOuverture = d; }
    public LocalDate getDateDernieresRegles() { return dateDernieresRegles; } public void setDateDernieresRegles(LocalDate d) { this.dateDernieresRegles = d; }
    public LocalDate getDatePrevueAccouchement() { return datePrevueAccouchement; } public void setDatePrevueAccouchement(LocalDate d) { this.datePrevueAccouchement = d; }
    public Integer getNombreGrossesses() { return nombreGrossesses; } public void setNombreGrossesses(Integer n) { this.nombreGrossesses = n; }
    public String getGroupeSanguin() { return groupeSanguin; } public void setGroupeSanguin(String g) { this.groupeSanguin = g; }
    public String getStatut() { return statut; } public void setStatut(String statut) { this.statut = statut; }
    public String getObservations() { return observations; } public void setObservations(String observations) { this.observations = observations; }
}
