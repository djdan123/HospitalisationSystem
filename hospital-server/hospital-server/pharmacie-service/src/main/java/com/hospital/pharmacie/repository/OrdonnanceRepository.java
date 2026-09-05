package com.hospital.pharmacie.repository;

import com.hospital.pharmacie.entity.Ordonnance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdonnanceRepository extends JpaRepository<Ordonnance, Long> {

    List<Ordonnance> findByPatientIdOrderByDateOrdonnanceDesc(Long patientId);

    List<Ordonnance> findByStatutOrderByDateOrdonnanceDesc(String statut);

    @Query("SELECT o FROM Ordonnance o WHERE o.statut IN :statuts ORDER BY o.dateOrdonnance DESC")
    List<Ordonnance> findByStatutIn(@Param("statuts") List<String> statuts);
}
