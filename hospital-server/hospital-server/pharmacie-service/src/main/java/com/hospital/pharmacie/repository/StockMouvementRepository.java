package com.hospital.pharmacie.repository;

import com.hospital.pharmacie.entity.StockMouvement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMouvementRepository extends JpaRepository<StockMouvement, Long> {
    List<StockMouvement> findByMedicamentIdOrderByDateMouvementDesc(Long medicamentId);
    List<StockMouvement> findTop50ByOrderByDateMouvementDesc();
    List<StockMouvement> findByTypeMouvementOrderByDateMouvementDesc(String type);
}
