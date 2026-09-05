package com.hospital.grpc;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.accueil.*;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.ManagedChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client gRPC pour le microservice Accueil (patients).
 */
public class AccueilClient {

    private static final Logger log = LoggerFactory.getLogger(AccueilClient.class);
    private final ManagedChannel channel;

    public AccueilClient() {
        this.channel = GrpcConfig.getAccueilChannel();
    }
    private AccueilServiceGrpc.AccueilServiceBlockingStub stub() {
    return AccueilServiceGrpc.newBlockingStub(channel)
            .withDeadlineAfter(AppConfig.getDeadlineSeconds(), TimeUnit.SECONDS);
}

    public Patient createPatient(String numeroDossier, String nom, String prenom, String dateNaissance,
                                 String sexe, String telephone, String email, String adresse) {
        try {
            CreatePatientRequest request = CreatePatientRequest.newBuilder()
                    .setNumeroDossier(numeroDossier)
                    .setNom(nom)
                    .setPrenom(prenom)
                    .setDateNaissance(dateNaissance)
                    .setSexe(sexe)
                    .setTelephone(telephone != null ? telephone : "")
                    .setEmail(email != null ? email : "")
                    .setAdresse(adresse != null ? adresse : "")
                    .build();
            PatientResponse response = stub().createPatient(request);
            log.info("Patient créé: {}", response.getPatient().getNumeroDossier());
            return response.getPatient();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Patient getPatient(long id) {
        try {
            return stub().getPatient(GetPatientRequest.newBuilder().setId(id).build()).getPatient();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Patient getPatientByDossier(String numeroDossier) {
        try {
            return stub().getPatientByDossier(
                    GetPatientByDossierRequest.newBuilder().setNumeroDossier(numeroDossier).build()
            ).getPatient();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Patient> searchPatients(String nom, String prenom) {
        try {
            SearchPatientsRequest.Builder builder = SearchPatientsRequest.newBuilder().setNom(nom);
            if (prenom != null) builder.setPrenom(prenom);
            return stub().searchPatients(builder.build()).getPatientsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Patient> getPatients(boolean includeInactive) {
        try {
            return stub().getPatients(
                    GetPatientsRequest.newBuilder().setIncludeInactive(includeInactive).build()
            ).getPatientsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Patient updatePatient(long id, String nom, String prenom, String dateNaissance,
                                 String sexe, String telephone, String email, String adresse, String statut) {
        try {
            UpdatePatientRequest.Builder b = UpdatePatientRequest.newBuilder().setId(id);
            if (nom != null) b.setNom(nom);
            if (prenom != null) b.setPrenom(prenom);
            if (dateNaissance != null) b.setDateNaissance(dateNaissance);
            if (sexe != null) b.setSexe(sexe);
            if (telephone != null) b.setTelephone(telephone);
            if (email != null) b.setEmail(email);
            if (adresse != null) b.setAdresse(adresse);
            if (statut != null) b.setStatut(statut);
            return stub().updatePatient(b.build()).getPatient();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public void deletePatient(long id) {
        try {
            stub().deletePatient(DeletePatientRequest.newBuilder().setId(id).build());
            log.info("Patient désactivé: id={}", id);
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public boolean patientExists(long id) {
        try {
            return stub().patientExists(PatientExistsRequest.newBuilder().setId(id).build()).getExists();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }
}
