package com.hospital.grpc;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.laboratoire.*;
import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LaboratoireClient {

    private final ManagedChannel channel;

    public LaboratoireClient() {
        this.channel = GrpcConfig.getLaboratoireChannel();
    }

    private LaboratoireServiceGrpc.LaboratoireServiceBlockingStub stub() {
        return LaboratoireServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(AppConfig.getDeadlineSeconds(), TimeUnit.SECONDS);
    }

    public Analyse createAnalyse(long patientId, String typeAnalyse, String observations) {
        try {
            return stub().createAnalyse(CreateAnalyseRequest.newBuilder()
                    .setPatientId(patientId)
                    .setTypeAnalyse(typeAnalyse)
                    .setObservations(observations != null ? observations : "")
                    .build()).getAnalyse();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Analyse getAnalyse(long id) {
        try {
            return stub().getAnalyse(GetAnalyseRequest.newBuilder().setId(id).build()).getAnalyse();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Analyse> getByPatient(long patientId) {
        try {
            return stub().getAnalysesByPatient(
                    GetAnalysesByPatientRequest.newBuilder().setPatientId(patientId).build()).getAnalysesList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Analyse updateAnalyse(long id, String statut, String datePrelevement, String observations) {
        try {
            return stub().updateAnalyse(UpdateAnalyseRequest.newBuilder()
                    .setId(id)
                    .setStatut(statut != null ? statut : "")
                    .setDatePrelevement(datePrelevement != null ? datePrelevement : "")
                    .setObservations(observations != null ? observations : "")
                    .build()).getAnalyse();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Resultat addResultat(long analyseId, String libelle, String valeur, String unite,
            String valeurReference, String interpretation) {
        try {
            return stub().addResultat(AddResultatRequest.newBuilder()
                    .setAnalyseId(analyseId)
                    .setLibelle(libelle != null ? libelle : "")
                    .setValeur(valeur != null ? valeur : "")
                    .setUnite(unite != null ? unite : "")
                    .setValeurReference(valeurReference != null ? valeurReference : "")
                    .setInterpretation(interpretation != null ? interpretation : "")
                    .build()).getResultat();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Resultat getResultat(long analyseId) {
        try {
            return stub().getResultat(GetResultatRequest.newBuilder().setAnalyseId(analyseId).build()).getResultat();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }
}
