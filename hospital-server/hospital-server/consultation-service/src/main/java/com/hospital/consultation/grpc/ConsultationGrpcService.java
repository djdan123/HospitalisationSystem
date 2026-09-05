package com.hospital.consultation.grpc;
import com.hospital.consultation.entity.Consultation;
import com.hospital.consultation.service.ConsultationService;
import com.hospital.grpc.consultation.*;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ConsultationGrpcService extends ConsultationServiceGrpc.ConsultationServiceImplBase {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ConsultationService service;
    public ConsultationGrpcService(ConsultationService service) { this.service = service; }

    @Override public void createConsultation(CreateConsultationRequest r, StreamObserver<ConsultationResponse> o) {
        Consultation c = service.create(r.getPatientId(), r.getMedecinId(), r.getDateConsultation(), r.getMotif(), r.getObservations());
        o.onNext(toResp(c)); o.onCompleted();
    }
    @Override public void getConsultation(GetConsultationRequest r, StreamObserver<ConsultationResponse> o) {
        o.onNext(toResp(service.get(r.getId()))); o.onCompleted();
    }
    @Override public void getConsultationsByPatient(GetConsultationsByPatientRequest r, StreamObserver<ConsultationsResponse> o) {
        o.onNext(toList(service.byPatient(r.getPatientId()))); o.onCompleted();
    }
    @Override public void getConsultationsByDoctor(GetConsultationsByDoctorRequest r, StreamObserver<ConsultationsResponse> o) {
        o.onNext(toList(service.byDoctor(r.getMedecinId()))); o.onCompleted();
    }
    @Override public void getAllConsultations(GetAllConsultationsRequest r, StreamObserver<ConsultationsResponse> o) {
        o.onNext(toList(service.all())); o.onCompleted();
    }
    @Override public void updateConsultation(UpdateConsultationRequest r, StreamObserver<ConsultationResponse> o) {
        o.onNext(toResp(service.update(r.getId(), r.getDiagnostic(), r.getObservations(), r.getPrescription(), r.getStatut()))); o.onCompleted();
    }
    @Override public void cancelConsultation(CancelConsultationRequest r, StreamObserver<ConsultationResponse> o) {
        o.onNext(toResp(service.cancel(r.getId(), r.getMotifAnnulation()))); o.onCompleted();
    }
    private ConsultationResponse toResp(Consultation c) { return ConsultationResponse.newBuilder().setConsultation(toProto(c)).build(); }
    private ConsultationsResponse toList(List<Consultation> list) {
        ConsultationsResponse.Builder b = ConsultationsResponse.newBuilder();
        list.forEach(c -> b.addConsultations(toProto(c)));
        return b.build();
    }
    private com.hospital.grpc.consultation.Consultation toProto(Consultation c) {
        var b = com.hospital.grpc.consultation.Consultation.newBuilder()
                .setId(c.getId()).setPatientId(c.getPatientId()).setStatut(c.getStatut());
        if (c.getMedecinId() != null) b.setMedecinId(c.getMedecinId());
        if (c.getDateConsultation() != null) b.setDateConsultation(c.getDateConsultation().format(FMT));
        if (c.getMotif() != null) b.setMotif(c.getMotif());
        if (c.getDiagnostic() != null) b.setDiagnostic(c.getDiagnostic());
        if (c.getObservations() != null) b.setObservations(c.getObservations());
        if (c.getPrescription() != null) b.setPrescription(c.getPrescription());
        return b.build();
    }
}
