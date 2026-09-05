package com.hospital.laboratoire.grpc;
import com.hospital.grpc.laboratoire.*;
import com.hospital.laboratoire.entity.Analyse;
import com.hospital.laboratoire.service.LaboratoireService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LaboratoireGrpcService extends LaboratoireServiceGrpc.LaboratoireServiceImplBase {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final LaboratoireService service;
    public LaboratoireGrpcService(LaboratoireService service) { this.service = service; }

    @Override public void createAnalyse(CreateAnalyseRequest r, StreamObserver<AnalyseResponse> o) {
        o.onNext(toResp(service.create(r.getPatientId(), r.getTypeAnalyse(), r.getObservations()))); o.onCompleted();
    }
    @Override public void getAnalyse(GetAnalyseRequest r, StreamObserver<AnalyseResponse> o) {
        o.onNext(toResp(service.get(r.getId()))); o.onCompleted();
    }
    @Override public void getAnalysesByPatient(GetAnalysesByPatientRequest r, StreamObserver<AnalysesResponse> o) {
        AnalysesResponse.Builder b = AnalysesResponse.newBuilder();
        service.byPatient(r.getPatientId()).forEach(a -> b.addAnalyses(toProto(a)));
        o.onNext(b.build()); o.onCompleted();
    }
    @Override public void updateAnalyse(UpdateAnalyseRequest r, StreamObserver<AnalyseResponse> o) {
        o.onNext(toResp(service.update(r.getId(), r.getStatut(), r.getDatePrelevement(), r.getObservations()))); o.onCompleted();
    }
    @Override public void addResultat(AddResultatRequest r, StreamObserver<ResultatResponse> o) {
        o.onNext(ResultatResponse.newBuilder().setResultat(Resultat.newBuilder()
                .setAnalyseId(r.getAnalyseId()).setLibelle(r.getLibelle()).setValeur(r.getValeur())
                .setUnite(r.getUnite()).setValeurReference(r.getValeurReference()).setInterpretation(r.getInterpretation())
                .setDateResultat(java.time.LocalDateTime.now().format(FMT)).build()).build());
        o.onCompleted();
    }
    @Override public void getResultat(GetResultatRequest r, StreamObserver<ResultatResponse> o) {
        o.onNext(ResultatResponse.newBuilder().build()); o.onCompleted();
    }
    private AnalyseResponse toResp(Analyse a) { return AnalyseResponse.newBuilder().setAnalyse(toProto(a)).build(); }
    private com.hospital.grpc.laboratoire.Analyse toProto(Analyse a) {
        var b = com.hospital.grpc.laboratoire.Analyse.newBuilder()
                .setId(a.getId()).setPatientId(a.getPatientId()).setStatut(a.getStatut());
        if (a.getTypeAnalyse() != null) b.setTypeAnalyse(a.getTypeAnalyse());
        if (a.getDateDemande() != null) b.setDateDemande(a.getDateDemande().format(FMT));
        if (a.getDatePrelevement() != null) b.setDatePrelevement(a.getDatePrelevement().format(FMT));
        if (a.getObservations() != null) b.setObservations(a.getObservations());
        return b.build();
    }
}
