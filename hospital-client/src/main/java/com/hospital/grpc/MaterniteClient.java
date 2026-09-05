package com.hospital.grpc;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.maternite.*;
import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MaterniteClient {

    private final ManagedChannel channel;

    public MaterniteClient() {
        this.channel = GrpcConfig.getMaterniteChannel();
    }

    private MaterniteServiceGrpc.MaterniteServiceBlockingStub stub() {
        return MaterniteServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(AppConfig.getDeadlineSeconds(), TimeUnit.SECONDS);
    }

    public DossierMaternite createDossier(long patientId, String ddr, int nbGrossesses, String groupe,
            String observations) {
        try {
            return stub().createDossierMaternite(CreateDossierMaterniteRequest.newBuilder()
                    .setPatientId(patientId)
                    .setDateDernieresRegles(ddr != null ? ddr : "")
                    .setNombreGrossesses(nbGrossesses)
                    .setGroupeSanguin(groupe != null ? groupe : "")
                    .setObservations(observations != null ? observations : "")
                    .build()).getDossier();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public DossierMaternite getDossier(long id) {
        try {
            return stub().getDossierMaternite(GetDossierMaterniteRequest.newBuilder().setId(id).build()).getDossier();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public SuiviGrossesse addSuiviGrossesse(long dossierId, String dateSuivi, int ageGrossesse, double poidsKg,
            double tensionSystolique, double tensionDiastolique,
            String observations, String medecin) {
        try {
            return stub().addSuiviGrossesse(AddSuiviGrossesseRequest.newBuilder()
                    .setDossierId(dossierId)
                    .setDateSuivi(dateSuivi != null ? dateSuivi : "")
                    .setAgeGestationnelSemaines(ageGrossesse)
                    .setPoidsKg(poidsKg)
                    .setTensionSystolique(tensionSystolique)
                    .setTensionDiastolique(tensionDiastolique)
                    .setObservations(observations != null ? observations : "")
                    .setMedecin(medecin != null ? medecin : "")
                    .build()).getSuivi();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<SuiviGrossesse> getSuiviGrossesse(long dossierId) {
        try {
            return stub().getSuiviGrossesse(GetSuiviGrossesseRequest.newBuilder().setDossierId(dossierId).build())
                    .getSuivisList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Accouchement registerAccouchement(long dossierId, String dateAccouchement, String typeAccouchement,
            int nombreEnfants, String observations, List<NouveauNe> nouveauNes) {
        try {
            RegisterAccouchementRequest.Builder b = RegisterAccouchementRequest.newBuilder()
                    .setDossierId(dossierId)
                    .setDateAccouchement(dateAccouchement != null ? dateAccouchement : "")
                    .setTypeAccouchement(typeAccouchement != null ? typeAccouchement : "")
                    .setNombreEnfants(nombreEnfants)
                    .setObservations(observations != null ? observations : "");
            if (nouveauNes != null)
                b.addAllNouveauNes(nouveauNes);
            return stub().registerAccouchement(b.build()).getAccouchement();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Accouchement getAccouchement(long dossierId) {
        try {
            return stub().getAccouchement(GetAccouchementRequest.newBuilder().setDossierId(dossierId).build())
                    .getAccouchement();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<DossierMaternite> getHistorique(long patientId) {
        try {
            return stub().getHistoriqueMaternite(
                    GetHistoriqueMaterniteRequest.newBuilder().setPatientId(patientId).build()).getDossiersList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }
}
