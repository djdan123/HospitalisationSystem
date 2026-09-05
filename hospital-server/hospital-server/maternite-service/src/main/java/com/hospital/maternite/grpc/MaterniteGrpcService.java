package com.hospital.maternite.grpc;
import com.hospital.grpc.maternite.*;
import com.hospital.maternite.entity.DossierMaternite;
import com.hospital.maternite.service.MaterniteService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

@Service
public class MaterniteGrpcService extends MaterniteServiceGrpc.MaterniteServiceImplBase {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;
    private final MaterniteService service;
    public MaterniteGrpcService(MaterniteService service) { this.service = service; }

    @Override public void createDossierMaternite(CreateDossierMaterniteRequest r, StreamObserver<DossierMaterniteResponse> o) {
        o.onNext(toResp(service.create(r.getPatientId(), r.getDateDernieresRegles(), r.getNombreGrossesses(), r.getGroupeSanguin(), r.getObservations())));
        o.onCompleted();
    }
    @Override public void getDossierMaternite(GetDossierMaterniteRequest r, StreamObserver<DossierMaterniteResponse> o) {
        o.onNext(toResp(service.get(r.getId()))); o.onCompleted();
    }
    @Override public void addSuiviGrossesse(AddSuiviGrossesseRequest r, StreamObserver<SuiviGrossesseResponse> o) {
        o.onNext(SuiviGrossesseResponse.newBuilder().setSuivi(SuiviGrossesse.newBuilder()
                .setDossierId(r.getDossierId()).setDateSuivi(r.getDateSuivi())
                .setAgeGestationnelSemaines(r.getAgeGestationnelSemaines())
                .setPoidsKg(r.getPoidsKg()).setTensionSystolique(r.getTensionSystolique())
                .setTensionDiastolique(r.getTensionDiastolique()).setObservations(r.getObservations())
                .setMedecin(r.getMedecin()).build()).build());
        o.onCompleted();
    }
    @Override public void getSuiviGrossesse(GetSuiviGrossesseRequest r, StreamObserver<SuivisGrossesseResponse> o) {
        o.onNext(SuivisGrossesseResponse.newBuilder().build()); o.onCompleted();
    }
    @Override public void registerAccouchement(RegisterAccouchementRequest r, StreamObserver<AccouchementResponse> o) {
        o.onNext(AccouchementResponse.newBuilder().setAccouchement(Accouchement.newBuilder()
                .setDossierId(r.getDossierId()).setDateAccouchement(r.getDateAccouchement())
                .setTypeAccouchement(r.getTypeAccouchement()).setNombreEnfants(r.getNombreEnfants())
                .setObservations(r.getObservations()).build()).build());
        o.onCompleted();
    }
    @Override public void getAccouchement(GetAccouchementRequest r, StreamObserver<AccouchementResponse> o) {
        o.onNext(AccouchementResponse.newBuilder().build()); o.onCompleted();
    }
    @Override public void getHistoriqueMaternite(GetHistoriqueMaterniteRequest r, StreamObserver<HistoriqueMaterniteResponse> o) {
        HistoriqueMaterniteResponse.Builder b = HistoriqueMaterniteResponse.newBuilder();
        service.byPatient(r.getPatientId()).forEach(d -> b.addDossiers(toProto(d)));
        o.onNext(b.build()); o.onCompleted();
    }
    private DossierMaterniteResponse toResp(DossierMaternite d) { return DossierMaterniteResponse.newBuilder().setDossier(toProto(d)).build(); }
    private com.hospital.grpc.maternite.DossierMaternite toProto(DossierMaternite d) {
        var b = com.hospital.grpc.maternite.DossierMaternite.newBuilder()
                .setId(d.getId()).setPatientId(d.getPatientId()).setStatut(d.getStatut())
                .setNombreGrossesses(d.getNombreGrossesses() != null ? d.getNombreGrossesses() : 1);
        if (d.getDateOuverture() != null) b.setDateOuverture(d.getDateOuverture().format(DTF));
        if (d.getDateDernieresRegles() != null) b.setDateDernieresRegles(d.getDateDernieresRegles().format(DF));
        if (d.getDatePrevueAccouchement() != null) b.setDatePrevueAccouchement(d.getDatePrevueAccouchement().format(DF));
        if (d.getGroupeSanguin() != null) b.setGroupeSanguin(d.getGroupeSanguin());
        if (d.getObservations() != null) b.setObservations(d.getObservations());
        return b.build();
    }
}
