package com.hospital.pharmacie.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordonnances")
public class Ordonnance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "medecin_id")
    private Long medecinId;

    @Column(name = "date_ordonnance", nullable = false)
    private LocalDateTime dateOrdonnance;

    @Column(nullable = false, length = 30)
    private String statut = "CREEE"; // CREEE / DELIVREE / PARTIELLE / ANNULEE

    /** AMBULATOIRE ou HOSPITALISE */
    @Column(name = "type_patient", nullable = false, length = 30)
    private String typePatient = "AMBULATOIRE";

    @Column(name = "service_soins", length = 150)
    private String serviceSoins;

    @Column(length = 1000)
    private String observations;

    @OneToMany(mappedBy = "ordonnance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrdonnanceLigne> lignes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (dateOrdonnance == null) dateOrdonnance = LocalDateTime.now();
        if (statut == null || statut.isBlank()) statut = "CREEE";
        if (typePatient == null || typePatient.isBlank()) typePatient = "AMBULATOIRE";
    }

    public void addLigne(OrdonnanceLigne ligne) {
        lignes.add(ligne);
        ligne.setOrdonnance(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getMedecinId() { return medecinId; }
    public void setMedecinId(Long medecinId) { this.medecinId = medecinId; }
    public LocalDateTime getDateOrdonnance() { return dateOrdonnance; }
    public void setDateOrdonnance(LocalDateTime dateOrdonnance) { this.dateOrdonnance = dateOrdonnance; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getTypePatient() { return typePatient; }
    public void setTypePatient(String typePatient) { this.typePatient = typePatient; }
    public String getServiceSoins() { return serviceSoins; }
    public void setServiceSoins(String serviceSoins) { this.serviceSoins = serviceSoins; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public List<OrdonnanceLigne> getLignes() { return lignes; }
    public void setLignes(List<OrdonnanceLigne> lignes) { this.lignes = lignes; }
}
