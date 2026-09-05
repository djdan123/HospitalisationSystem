package com.hospital.pharmacie.repository;
import com.hospital.pharmacie.entity.Medicament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface MedicamentRepository extends JpaRepository<Medicament, Long> {
    List<Medicament> findByStatut(String statut);
}
