package com.hospital.grpc;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.consultation.*;
import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConsultationClient {

    private final ManagedChannel channel;

    public ConsultationClient() {
         this.channel = GrpcConfig.getConsultationChannel();
    }
    private ConsultationServiceGrpc.ConsultationServiceBlockingStub stub() {
    return ConsultationServiceGrpc.newBlockingStub(channel)
            .withDeadlineAfter(AppConfig.getDeadlineSeconds(), TimeUnit.SECONDS);
}

    public Consultation createConsultation(long patientId, long medecinId, String date, String motif, String observations) {
        try {
            return stub().createConsultation(CreateConsultationRequest.newBuilder()
                    .setPatientId(patientId)
                    .setMedecinId(medecinId)
                    .setDateConsultation(date != null ? date : "")
                    .setMotif(motif != null ? motif : "")
                    .setObservations(observations != null ? observations : "")
                    .build()).getConsultation();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Consultation getConsultation(long id) {
        try {
            return stub().getConsultation(GetConsultationRequest.newBuilder().setId(id).build()).getConsultation();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Consultation> getByPatient(long patientId) {
        try {
            return stub().getConsultationsByPatient(
                    GetConsultationsByPatientRequest.newBuilder().setPatientId(patientId).build()
            ).getConsultationsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Consultation> getByDoctor(long medecinId) {
        try {
            return stub().getConsultationsByDoctor(
                    GetConsultationsByDoctorRequest.newBuilder().setMedecinId(medecinId).build()
            ).getConsultationsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Consultation> getAll() {
        try {
            return stub().getAllConsultations(GetAllConsultationsRequest.getDefaultInstance()).getConsultationsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Consultation updateConsultation(long id, String diagnostic, String observations, String prescription, String statut) {
        try {
            return stub().updateConsultation(UpdateConsultationRequest.newBuilder()
                    .setId(id)
                    .setDiagnostic(diagnostic != null ? diagnostic : "")
                    .setObservations(observations != null ? observations : "")
                    .setPrescription(prescription != null ? prescription : "")
                    .setStatut(statut != null ? statut : "")
                    .build()).getConsultation();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Consultation cancelConsultation(long id, String motif) {
        try {
            return stub().cancelConsultation(CancelConsultationRequest.newBuilder()
                    .setId(id)
                    .setMotifAnnulation(motif != null ? motif : "")
                    .build()).getConsultation();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }
}
