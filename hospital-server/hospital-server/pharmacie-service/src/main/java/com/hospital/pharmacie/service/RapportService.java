package com.hospital.pharmacie.service;

import com.hospital.pharmacie.entity.Medicament;
import com.hospital.pharmacie.entity.Ordonnance;
import com.hospital.pharmacie.entity.StockMouvement;
import com.hospital.pharmacie.repository.MedicamentRepository;
import com.hospital.pharmacie.repository.OrdonnanceRepository;
import com.hospital.pharmacie.repository.StockMouvementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rapports pharmacie : consommation, valorisation, sécurité, traçabilité, KPI.
 */
@Service
public class RapportService {

    private final MedicamentRepository medicamentRepository;
    private final StockMouvementRepository mouvementRepository;
    private final OrdonnanceRepository ordonnanceRepository;

    public RapportService(MedicamentRepository medicamentRepository,
                          StockMouvementRepository mouvementRepository,
                          OrdonnanceRepository ordonnanceRepository) {
        this.medicamentRepository = medicamentRepository;
        this.mouvementRepository = mouvementRepository;
        this.ordonnanceRepository = ordonnanceRepository;
    }

    // ---------- Valorisation du stock ----------
    public double valorisationStock() {
        return medicamentRepository.findAll().stream()
                .filter(m -> m.getStock() != null && m.getPrixUnitaire() != null)
                .filter(m -> !"DESACTIVE".equals(m.getStatut()) && !"BLOQUE".equals(m.getStatut()))
                .mapToDouble(m -> m.getStock() * m.getPrixUnitaire())
                .sum();
    }

    public List<LigneValorisation> detailValorisation() {
        return medicamentRepository.findAll().stream()
                .filter(m -> m.getStock() != null && m.getStock() > 0)
                .filter(m -> m.getPrixUnitaire() != null)
                .map(m -> new LigneValorisation(
                        m.getCode(), m.getNom(), m.getStock(), m.getPrixUnitaire(),
                        m.getStock() * m.getPrixUnitaire(), m.getUnite()))
                .sorted(Comparator.comparingDouble(LigneValorisation::valeur).reversed())
                .collect(Collectors.toList());
    }

    // ---------- Top consommations (sorties) ----------
    public List<TopConso> topConsommations(int limit, String serviceFilter, LocalDate depuis) {
        List<StockMouvement> sorties = allSorties().stream()
                .filter(mv -> depuis == null || !mv.getDateMouvement().toLocalDate().isBefore(depuis))
                .filter(mv -> serviceFilter == null || serviceFilter.isBlank()
                        || serviceFilter.equalsIgnoreCase(mv.getServiceSoins()))
                .collect(Collectors.toList());

        Map<Long, Integer> qtyByMed = new HashMap<>();
        Map<Long, String> serviceByMed = new HashMap<>();
        for (StockMouvement mv : sorties) {
            qtyByMed.merge(mv.getMedicamentId(), Math.abs(mv.getQuantite()), Integer::sum);
            if (mv.getServiceSoins() != null) serviceByMed.putIfAbsent(mv.getMedicamentId(), mv.getServiceSoins());
        }

        Map<Long, Medicament> meds = medicamentRepository.findAll().stream()
                .collect(Collectors.toMap(Medicament::getId, m -> m, (a, b) -> a));

        return qtyByMed.entrySet().stream()
                .map(e -> {
                    Medicament m = meds.get(e.getKey());
                    String nom = m != null ? m.getNom() : ("#" + e.getKey());
                    String code = m != null ? m.getCode() : "";
                    return new TopConso(code, nom, e.getValue(), serviceByMed.getOrDefault(e.getKey(), "—"));
                })
                .sorted(Comparator.comparingInt(TopConso::quantite).reversed())
                .limit(limit > 0 ? limit : 20)
                .collect(Collectors.toList());
    }

    // ---------- Dépenses par service ----------
    public List<DepenseService> depensesParService(LocalDate depuis) {
        List<StockMouvement> sorties = allSorties().stream()
                .filter(mv -> depuis == null || !mv.getDateMouvement().toLocalDate().isBefore(depuis))
                .filter(mv -> mv.getServiceSoins() != null && !mv.getServiceSoins().isBlank())
                .collect(Collectors.toList());

        Map<Long, Medicament> meds = medicamentRepository.findAll().stream()
                .collect(Collectors.toMap(Medicament::getId, m -> m, (a, b) -> a));

        Map<String, double[]> agg = new HashMap<>(); // [qty, valeur]
        for (StockMouvement mv : sorties) {
            Medicament m = meds.get(mv.getMedicamentId());
            double prix = m != null && m.getPrixUnitaire() != null ? m.getPrixUnitaire() : 0;
            int q = Math.abs(mv.getQuantite());
            agg.computeIfAbsent(mv.getServiceSoins(), k -> new double[2]);
            agg.get(mv.getServiceSoins())[0] += q;
            agg.get(mv.getServiceSoins())[1] += q * prix;
        }
        return agg.entrySet().stream()
                .map(e -> new DepenseService(e.getKey(), (int) e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingDouble(DepenseService::valeur).reversed())
                .collect(Collectors.toList());
    }

    // ---------- Péremption 30/60/90 ----------
    public List<Medicament> peremptionDans(int jours) {
        LocalDate limite = LocalDate.now().plusDays(jours);
        return medicamentRepository.findAll().stream()
                .filter(m -> m.getDatePeremption() != null)
                .filter(m -> !m.getDatePeremption().isAfter(limite))
                .sorted(Comparator.comparing(Medicament::getDatePeremption))
                .collect(Collectors.toList());
    }

    // ---------- Ruptures ----------
    public List<Medicament> rupturesActuelles() {
        return medicamentRepository.findAll().stream()
                .filter(m -> m.getStock() == null || m.getStock() <= 0
                        || "RUPTURE".equals(m.getStatut()))
                .collect(Collectors.toList());
    }

    // ---------- Rappel de lot : patients / services touchés ----------
    public List<StockMouvement> distributionsLot(String numeroLot) {
        if (numeroLot == null || numeroLot.isBlank()) return List.of();
        return mouvementRepository.findTop50ByOrderByDateMouvementDesc().stream()
                .filter(mv -> numeroLot.equalsIgnoreCase(mv.getNumeroLot()))
                .filter(mv -> "SORTIE".equals(mv.getTypeMouvement()) || "DOTATION".equals(mv.getTypeMouvement()))
                .collect(Collectors.toList());
    }

    // ---------- Registre stupéfiants ----------
    public List<StockMouvement> registreStupefiants(LocalDate depuis) {
        Set<Long> stupIds = medicamentRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsStupefiant()))
                .map(Medicament::getId)
                .collect(Collectors.toSet());
        if (stupIds.isEmpty()) return List.of();
        return mouvementRepository.findTop50ByOrderByDateMouvementDesc().stream()
                .filter(mv -> stupIds.contains(mv.getMedicamentId()))
                .filter(mv -> depuis == null || !mv.getDateMouvement().toLocalDate().isBefore(depuis))
                .sorted(Comparator.comparing(StockMouvement::getDateMouvement).reversed())
                .collect(Collectors.toList());
    }

    // ---------- Ordonnances stats ----------
    public OrdonnanceStats statsOrdonnances() {
        List<Ordonnance> all;
        try {
            all = ordonnanceRepository.findAll();
        } catch (Exception e) {
            return new OrdonnanceStats(0, 0, 0, 0);
        }
        int total = all.size();
        int delivrees = (int) all.stream().filter(o -> "DELIVREE".equals(o.getStatut())).count();
        int partielles = (int) all.stream().filter(o -> "PARTIELLE".equals(o.getStatut())).count();
        int creees = (int) all.stream().filter(o -> "CREEE".equals(o.getStatut())).count();
        return new OrdonnanceStats(total, delivrees, partielles, creees);
    }

    // ---------- Audit utilisateurs (mouvements) ----------
    public List<StockMouvement> auditUtilisateurs(String userFilter) {
        return mouvementRepository.findTop50ByOrderByDateMouvementDesc().stream()
                .filter(mv -> userFilter == null || userFilter.isBlank()
                        || userFilter.equalsIgnoreCase(mv.getUserLogin()))
                .collect(Collectors.toList());
    }

    // ---------- KPI rotation (sorties / stock moyen approximatif) ----------
    public List<KpiRotation> rotationStock(int jours) {
        LocalDate depuis = LocalDate.now().minusDays(jours > 0 ? jours : 30);
        Map<Long, Integer> sorties = new HashMap<>();
        for (StockMouvement mv : allSorties()) {
            if (mv.getDateMouvement().toLocalDate().isBefore(depuis)) continue;
            sorties.merge(mv.getMedicamentId(), Math.abs(mv.getQuantite()), Integer::sum);
        }
        Map<Long, Medicament> meds = medicamentRepository.findAll().stream()
                .collect(Collectors.toMap(Medicament::getId, m -> m, (a, b) -> a));
        List<KpiRotation> list = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : sorties.entrySet()) {
            Medicament m = meds.get(e.getKey());
            if (m == null) continue;
            int stock = m.getStock() != null ? m.getStock() : 0;
            double rotation = stock > 0 ? (double) e.getValue() / stock : e.getValue();
            list.add(new KpiRotation(m.getCode(), m.getNom(), e.getValue(), stock, rotation));
        }
        list.sort(Comparator.comparingDouble(KpiRotation::rotation).reversed());
        return list;
    }

    private List<StockMouvement> allSorties() {
        try {
            return mouvementRepository.findByTypeMouvementOrderByDateMouvementDesc("SORTIE");
        } catch (Exception e) {
            return List.of();
        }
    }

    // ---- DTOs ----
    public record LigneValorisation(String code, String nom, int stock, double prix, double valeur, String unite) {}
    public record TopConso(String code, String nom, int quantite, String service) {}
    public record DepenseService(String service, int quantite, double valeur) {}
    public record OrdonnanceStats(int total, int delivrees, int partielles, int creees) {}
    public record KpiRotation(String code, String nom, int sorties, int stockActuel, double rotation) {}
}
