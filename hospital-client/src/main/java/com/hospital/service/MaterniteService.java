package com.hospital.service;

import com.hospital.grpc.MaterniteClient;
import com.hospital.grpc.maternite.Accouchement;
import com.hospital.grpc.maternite.DossierMaternite;
import com.hospital.grpc.maternite.NouveauNe;
import com.hospital.grpc.maternite.SuiviGrossesse;

import java.util.List;

public class MaterniteService {

    private final MaterniteClient client = new MaterniteClient();

    public DossierMaternite createDossier(long patientId, String ddr, int nbGrossesses, String groupe, String observations) {
        return client.createDossier(patientId, ddr, nbGrossesses, groupe, observations);
    }

    public DossierMaternite getDossier(long id) {
        return client.getDossier(id);
    }

    public SuiviGrossesse addSuiviGrossesse(long dossierId, String dateSuivi, int ageGrossesse, double poidsKg,
                                          double tensionSystolique, double tensionDiastolique,
                                          String observations, String medecin) {
        return client.addSuiviGrossesse(dossierId, dateSuivi, ageGrossesse, poidsKg, tensionSystolique, tensionDiastolique, observations, medecin);
    }

    public List<SuiviGrossesse> getSuiviGrossesse(long dossierId) {
        return client.getSuiviGrossesse(dossierId);
    }

    public Accouchement registerAccouchement(long dossierId, String dateAccouchement, String typeAccouchement,
                                            int nombreEnfants, String observations, List<NouveauNe> nouveauNes) {
        return client.registerAccouchement(dossierId, dateAccouchement, typeAccouchement, nombreEnfants, observations, nouveauNes);
    }

    public Accouchement getAccouchement(long dossierId) {
        return client.getAccouchement(dossierId);
    }

    public List<DossierMaternite> getHistorique(long patientId) {
        return client.getHistorique(patientId);
    }
}
