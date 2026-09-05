package com.hospital.service;

import com.hospital.grpc.HospitalisationClient;
import com.hospital.grpc.hospitalisation.Hospitalisation;

import java.util.List;

public class HospitalisationService {

    private final HospitalisationClient client = new HospitalisationClient();

    public Hospitalisation admitPatient(long patientId, String motif, String observations, Long chambreId, Long litId) {
        return client.admitPatient(patientId, motif, observations, chambreId, litId);
    }

    public Hospitalisation getHospitalisation(long id) {
        return client.getHospitalisation(id);
    }

    public List<Hospitalisation> getHospitalisations(Long patientId, String statut) {
        return client.getHospitalisations(patientId, statut);
    }

    public Hospitalisation assignRoom(long hospitalisationId, long chambreId, long litId) {
        return client.assignRoom(hospitalisationId, chambreId, litId);
    }

    public Hospitalisation transferPatient(long hospitalisationId, long nouvelleChambreId, long nouveauLitId, String motifTransfert) {
        return client.transferPatient(hospitalisationId, nouvelleChambreId, nouveauLitId, motifTransfert);
    }

    public Hospitalisation dischargePatient(long hospitalisationId, String observations) {
        return client.dischargePatient(hospitalisationId, observations);
    }

    public List<Hospitalisation> getHistory(long patientId) {
        return client.getHistory(patientId);
    }
}
