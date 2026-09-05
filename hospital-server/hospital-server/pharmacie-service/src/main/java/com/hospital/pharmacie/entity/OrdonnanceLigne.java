package com.hospital.pharmacie.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ordonnance_lignes")
public class OrdonnanceLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordonnance_id", nullable = false)
    private Ordonnance ordonnance;

    @Column(name = "medicament_id", nullable = false)
    private Long medicamentId;

    @Column(name = "nom_medicament", length = 150)
    private String nomMedicament;

    @Column(nullable = false)
    private Integer quantite = 1;

    @Column(name = "quantite_delivree", nullable = false)
    private Integer quantiteDelivree = 0;

    @Column(length = 255)
    private String posologie;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Ordonnance getOrdonnance() { return ordonnance; }
    public void setOrdonnance(Ordonnance ordonnance) { this.ordonnance = ordonnance; }
    public Long getMedicamentId() { return medicamentId; }
    public void setMedicamentId(Long medicamentId) { this.medicamentId = medicamentId; }
    public String getNomMedicament() { return nomMedicament; }
    public void setNomMedicament(String nomMedicament) { this.nomMedicament = nomMedicament; }
    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }
    public Integer getQuantiteDelivree() { return quantiteDelivree; }
    public void setQuantiteDelivree(Integer quantiteDelivree) { this.quantiteDelivree = quantiteDelivree; }
    public String getPosologie() { return posologie; }
    public void setPosologie(String posologie) { this.posologie = posologie; }

    public int resteADelivrer() {
        return Math.max(0, quantite - (quantiteDelivree != null ? quantiteDelivree : 0));
    }
}
