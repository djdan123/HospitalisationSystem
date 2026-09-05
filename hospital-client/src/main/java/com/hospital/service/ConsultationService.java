package com.hospital.service;

import com.hospital.grpc.ConsultationClient;
import com.hospital.grpc.consultation.Consultation;

import java.util.List;

public class ConsultationService {

    private final ConsultationClient client = new ConsultationClient();

    public Consultation createConsultation(long patientId, long medecinId, String date, String motif, String observations) {
        return client.createConsultation(patientId, medecinId, date, motif, observations);
    }

    public Consultation getConsultation(long id) {
        return client.getConsultation(id);
    }

    public List<Consultation> getByPatient(long patientId) {
        return client.getByPatient(patientId);
    }

    public List<Consultation> getByDoctor(long medecinId) {
        return client.getByDoctor(medecinId);
    }

    public Consultation updateConsultation(long id, String diagnostic, String observations, String prescription, String statut) {
        return client.updateConsultation(id, diagnostic, observations, prescription, statut);
    }

    public Consultation cancelConsultation(long id, String motif) {
        return client.cancelConsultation(id, motif);
    }
}
