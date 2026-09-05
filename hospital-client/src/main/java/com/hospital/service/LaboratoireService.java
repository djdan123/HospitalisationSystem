package com.hospital.service;

import com.hospital.grpc.LaboratoireClient;
import com.hospital.grpc.laboratoire.Analyse;
import com.hospital.grpc.laboratoire.Resultat;

import java.util.List;

public class LaboratoireService {

    private final LaboratoireClient client = new LaboratoireClient();

    public Analyse createAnalyse(long patientId, String typeAnalyse, String observations) {
        return client.createAnalyse(patientId, typeAnalyse, observations);
    }

    public Analyse getAnalyse(long id) {
        return client.getAnalyse(id);
    }

    public List<Analyse> getByPatient(long patientId) {
        return client.getByPatient(patientId);
    }

    public Analyse updateAnalyse(long id, String statut, String datePrelevement, String observations) {
        return client.updateAnalyse(id, statut, datePrelevement, observations);
    }

    public Resultat addResultat(long analyseId, String libelle, String valeur, String unite, String valeurReference, String interpretation) {
        return client.addResultat(analyseId, libelle, valeur, unite, valeurReference, interpretation);
    }

    public Resultat getResultat(long analyseId) {
        return client.getResultat(analyseId);
    }
}
