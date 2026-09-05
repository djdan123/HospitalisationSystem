package com.hospital.hospitalisation.grpc;

import com.hospital.grpc.hospitalisation.*;
import com.hospital.hospitalisation.entity.Hospitalisation;
import com.hospital.hospitalisation.service.HospitalisationService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HospitalisationGrpcService extends HospitalisationServiceGrpc.HospitalisationServiceImplBase {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final HospitalisationService service;

    public HospitalisationGrpcService(HospitalisationService service) {
        this.service = service;
    }

    @Override
    public void admitPatient(AdmitPatientRequest request, StreamObserver<HospitalisationResponse> responseObserver) {
        Hospitalisation h = service.admit(
                request.getPatientId(), request.getMotif(), request.getObservations(),
                request.getChambreId() > 0 ? request.getChambreId() : null,
                request.getLitId() > 0 ? request.getLitId() : null);
        responseObserver.onNext(toResponse(h));
        responseObserver.onCompleted();
    }

    @Override
    public void getHospitalisation(GetHospitalisationRequest request, StreamObserver<HospitalisationResponse> responseObserver) {
        responseObserver.onNext(toResponse(service.getById(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getHospitalisations(GetHospitalisationsRequest request, StreamObserver<HospitalisationsResponse> responseObserver) {
        Long patientId = request.getPatientId() > 0 ? request.getPatientId() : null;
        String statut = request.getStatut().isBlank() ? null : request.getStatut();
        List<Hospitalisation> list = service.getAll(patientId, statut);
        responseObserver.onNext(toListResponse(list));
        responseObserver.onCompleted();
    }

    @Override
    public void assignRoom(AssignRoomRequest request, StreamObserver<HospitalisationResponse> responseObserver) {
        Hospitalisation h = service.assignRoom(request.getHospitalisationId(), request.getChambreId(), request.getLitId());
        responseObserver.onNext(toResponse(h));
        responseObserver.onCompleted();
    }

    @Override
    public void transferPatient(TransferPatientRequest request, StreamObserver<HospitalisationResponse> responseObserver) {
        Hospitalisation h = service.transfer(request.getHospitalisationId(),
                request.getNouvelleChambreId(), request.getNouveauLitId(), request.getMotifTransfert());
        responseObserver.onNext(toResponse(h));
        responseObserver.onCompleted();
    }

    @Override
    public void dischargePatient(DischargePatientRequest request, StreamObserver<HospitalisationResponse> responseObserver) {
        Hospitalisation h = service.discharge(request.getHospitalisationId(), request.getObservationsSortie());
        responseObserver.onNext(toResponse(h));
        responseObserver.onCompleted();
    }

    @Override
    public void getHospitalisationHistory(GetHospitalisationHistoryRequest request, StreamObserver<HospitalisationsResponse> responseObserver) {
        responseObserver.onNext(toListResponse(service.getHistory(request.getPatientId())));
        responseObserver.onCompleted();
    }

    private HospitalisationResponse toResponse(Hospitalisation h) {
        return HospitalisationResponse.newBuilder().setHospitalisation(toProto(h)).build();
    }

    private HospitalisationsResponse toListResponse(List<Hospitalisation> list) {
        HospitalisationsResponse.Builder b = HospitalisationsResponse.newBuilder();
        list.forEach(h -> b.addHospitalisations(toProto(h)));
        return b.build();
    }

    private com.hospital.grpc.hospitalisation.Hospitalisation toProto(Hospitalisation h) {
        var builder = com.hospital.grpc.hospitalisation.Hospitalisation.newBuilder()
                .setId(h.getId())
                .setPatientId(h.getPatientId())
                .setDateAdmission(h.getDateAdmission().format(FMT))
                .setStatut(h.getStatut());
        if (h.getDateSortie() != null) builder.setDateSortie(h.getDateSortie().format(FMT));
        if (h.getMotif() != null) builder.setMotif(h.getMotif());
        if (h.getChambreId() != null) builder.setChambreId(h.getChambreId());
        if (h.getLitId() != null) builder.setLitId(h.getLitId());
        if (h.getNumeroChambre() != null) builder.setNumeroChambre(h.getNumeroChambre());
        if (h.getNumeroLit() != null) builder.setNumeroLit(h.getNumeroLit());
        if (h.getObservations() != null) builder.setObservations(h.getObservations());
        return builder.build();
    }
}
