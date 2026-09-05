package com.hospital.grpc;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.hospitalisation.*;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.ManagedChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HospitalisationClient {

    private static final Logger log = LoggerFactory.getLogger(HospitalisationClient.class);
    private final ManagedChannel channel;

    public HospitalisationClient() {
        this.channel = GrpcConfig.getHospitalisationChannel();
    }
    private HospitalisationServiceGrpc.HospitalisationServiceBlockingStub stub() {
    return HospitalisationServiceGrpc.newBlockingStub(channel)
            .withDeadlineAfter(AppConfig.getDeadlineSeconds(), TimeUnit.SECONDS);
}
    public Hospitalisation admitPatient(long patientId, String motif, String observations, Long chambreId, Long litId) {
        try {
            AdmitPatientRequest.Builder b = AdmitPatientRequest.newBuilder()
                    .setPatientId(patientId)
                    .setMotif(motif)
                    .setObservations(observations != null ? observations : "");
            if (chambreId != null) b.setChambreId(chambreId);
            if (litId != null) b.setLitId(litId);
            Hospitalisation h = stub().admitPatient(b.build()).getHospitalisation();
            log.info("Admission patient {} → hosp id={}", patientId, h.getId());
            return h;
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Hospitalisation getHospitalisation(long id) {
        try {
            return stub().getHospitalisation(GetHospitalisationRequest.newBuilder().setId(id).build()).getHospitalisation();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Hospitalisation> getHospitalisations(Long patientId, String statut) {
        try {
            GetHospitalisationsRequest.Builder b = GetHospitalisationsRequest.newBuilder();
            if (patientId != null) b.setPatientId(patientId);
            if (statut != null) b.setStatut(statut);
            return stub().getHospitalisations(b.build()).getHospitalisationsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Hospitalisation assignRoom(long hospitalisationId, long chambreId, long litId) {
        try {
            return stub().assignRoom(AssignRoomRequest.newBuilder()
                    .setHospitalisationId(hospitalisationId)
                    .setChambreId(chambreId)
                    .setLitId(litId)
                    .build()).getHospitalisation();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Hospitalisation transferPatient(long hospitalisationId, long nouvelleChambreId, long nouveauLitId, String motifTransfert) {
        try {
            return stub().transferPatient(TransferPatientRequest.newBuilder()
                    .setHospitalisationId(hospitalisationId)
                    .setNouvelleChambreId(nouvelleChambreId)
                    .setNouveauLitId(nouveauLitId)
                    .setMotifTransfert(motifTransfert != null ? motifTransfert : "")
                    .build()).getHospitalisation();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Hospitalisation dischargePatient(long hospitalisationId, String observations) {
        try {
            return stub().dischargePatient(DischargePatientRequest.newBuilder()
                    .setHospitalisationId(hospitalisationId)
                    .setObservationsSortie(observations != null ? observations : "")
                    .build()).getHospitalisation();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Hospitalisation> getHistory(long patientId) {
        try {
            return stub().getHospitalisationHistory(
                    GetHospitalisationHistoryRequest.newBuilder().setPatientId(patientId).build()
            ).getHospitalisationsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }
}
