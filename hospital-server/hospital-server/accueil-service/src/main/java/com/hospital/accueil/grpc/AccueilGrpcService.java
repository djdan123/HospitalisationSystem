package com.hospital.accueil.grpc;

import com.hospital.accueil.entity.Patient;
import com.hospital.accueil.service.PatientService;
import com.hospital.grpc.accueil.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AccueilGrpcService extends AccueilServiceGrpc.AccueilServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AccueilGrpcService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PatientService patientService;

    public AccueilGrpcService(PatientService patientService) {
        this.patientService = patientService;
    }

    @Override
    public void createPatient(CreatePatientRequest request, StreamObserver<PatientResponse> responseObserver) {
        Patient patient = patientService.create(
                request.getNumeroDossier(),
                request.getNom(),
                request.getPrenom(),
                request.getDateNaissance(),
                request.getSexe(),
                request.getTelephone(),
                request.getEmail(),
                request.getAdresse()
        );
        responseObserver.onNext(toResponse(patient));
        responseObserver.onCompleted();
    }

    @Override
    public void getPatient(GetPatientRequest request, StreamObserver<PatientResponse> responseObserver) {
        Patient patient = patientService.getById(request.getId());
        responseObserver.onNext(toResponse(patient));
        responseObserver.onCompleted();
    }

    @Override
    public void getPatientByDossier(GetPatientByDossierRequest request, StreamObserver<PatientResponse> responseObserver) {
        Patient patient = patientService.getByDossier(request.getNumeroDossier());
        responseObserver.onNext(toResponse(patient));
        responseObserver.onCompleted();
    }

    @Override
    public void searchPatients(SearchPatientsRequest request, StreamObserver<PatientsResponse> responseObserver) {
        List<Patient> patients = patientService.search(request.getNom(), request.getPrenom());
        responseObserver.onNext(toListResponse(patients));
        responseObserver.onCompleted();
    }

    @Override
    public void getPatients(GetPatientsRequest request, StreamObserver<PatientsResponse> responseObserver) {
        List<Patient> patients = patientService.getAll(request.getIncludeInactive());
        responseObserver.onNext(toListResponse(patients));
        responseObserver.onCompleted();
    }

    @Override
    public void updatePatient(UpdatePatientRequest request, StreamObserver<PatientResponse> responseObserver) {
        Patient patient = patientService.update(
                request.getId(),
                request.getNom(),
                request.getPrenom(),
                request.getDateNaissance(),
                request.getSexe(),
                request.getTelephone(),
                request.getEmail(),
                request.getAdresse(),
                request.getStatut()
        );
        responseObserver.onNext(toResponse(patient));
        responseObserver.onCompleted();
    }

    @Override
    public void deletePatient(DeletePatientRequest request, StreamObserver<DeletePatientResponse> responseObserver) {
        patientService.delete(request.getId());
        responseObserver.onNext(DeletePatientResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Patient désactivé avec succès")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void patientExists(PatientExistsRequest request, StreamObserver<PatientExistsResponse> responseObserver) {
        boolean exists = patientService.exists(request.getId());
        responseObserver.onNext(PatientExistsResponse.newBuilder().setExists(exists).build());
        responseObserver.onCompleted();
    }

    private PatientResponse toResponse(Patient p) {
        return PatientResponse.newBuilder()
                .setPatient(toProto(p))
                .build();
    }

    private PatientsResponse toListResponse(List<Patient> list) {
        PatientsResponse.Builder builder = PatientsResponse.newBuilder();
        list.forEach(p -> builder.addPatients(toProto(p)));
        return builder.build();
    }

    private com.hospital.grpc.accueil.Patient toProto(Patient p) {
        com.hospital.grpc.accueil.Patient.Builder b = com.hospital.grpc.accueil.Patient.newBuilder()
                .setId(p.getId())
                .setNumeroDossier(p.getNumeroDossier())
                .setNom(p.getNom())
                .setPrenom(p.getPrenom())
                .setDateNaissance(p.getDateNaissance().format(DATE_FMT))
                .setSexe(p.getSexe())
                .setDateCreation(p.getDateCreation().format(DATETIME_FMT))
                .setStatut(p.getStatut());

        if (p.getTelephone() != null) b.setTelephone(p.getTelephone());
        if (p.getEmail() != null) b.setEmail(p.getEmail());
        if (p.getAdresse() != null) b.setAdresse(p.getAdresse());
        return b.build();
    }
}
