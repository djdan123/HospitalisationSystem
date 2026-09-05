package com.hospital.paiement.grpc;

import com.hospital.grpc.paiement.*;
import com.hospital.paiement.entity.Facture;
import com.hospital.paiement.service.PaiementService;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PaiementGrpcService extends PaiementServiceGrpc.PaiementServiceImplBase {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final PaiementService service;

    public PaiementGrpcService(PaiementService service) { this.service = service; }

    @Override
    public void createFacture(CreateFactureRequest request, StreamObserver<FactureResponse> responseObserver) {
        Facture f = service.createFacture(request.getPatientId(), request.getMontantTotal(), request.getDescription());
        responseObserver.onNext(toResponse(f));
        responseObserver.onCompleted();
    }

    @Override
    public void getFacture(GetFactureRequest request, StreamObserver<FactureResponse> responseObserver) {
        responseObserver.onNext(toResponse(service.getFacture(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getFacturesByPatient(GetFacturesByPatientRequest request, StreamObserver<FacturesResponse> responseObserver) {
        List<Facture> list = service.getByPatient(request.getPatientId());
        FacturesResponse.Builder b = FacturesResponse.newBuilder();
        list.forEach(f -> b.addFactures(toProto(f)));
        responseObserver.onNext(b.build());
        responseObserver.onCompleted();
    }

    @Override
    public void makePayment(MakePaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        Facture f = service.makePayment(request.getFactureId(), request.getMontant(), request.getModePaiement());
        Payment p = Payment.newBuilder()
                .setId(0)
                .setFactureId(f.getId())
                .setPatientId(f.getPatientId())
                .setMontant(request.getMontant())
                .setModePaiement(request.getModePaiement())
                .setDatePaiement(java.time.LocalDateTime.now().format(FMT))
                .setReference(request.getReference())
                .build();
        responseObserver.onNext(PaymentResponse.newBuilder().setPayment(p).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPayment(GetPaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        responseObserver.onNext(PaymentResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPaymentsByPatient(GetPaymentsByPatientRequest request, StreamObserver<PaymentsResponse> responseObserver) {
        responseObserver.onNext(PaymentsResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void generateReceipt(GenerateReceiptRequest request, StreamObserver<ReceiptResponse> responseObserver) {
        responseObserver.onNext(ReceiptResponse.newBuilder()
                .setNumeroRecu("REC-" + request.getPaymentId())
                .setContenu("Reçu de paiement")
                .setDateEmission(java.time.LocalDateTime.now().format(FMT))
                .build());
        responseObserver.onCompleted();
    }

    private FactureResponse toResponse(Facture f) {
        return FactureResponse.newBuilder().setFacture(toProto(f)).build();
    }

    private com.hospital.grpc.paiement.Facture toProto(Facture f) {
        return com.hospital.grpc.paiement.Facture.newBuilder()
                .setId(f.getId())
                .setPatientId(f.getPatientId())
                .setNumeroFacture(f.getNumeroFacture() != null ? f.getNumeroFacture() : "")
                .setMontantTotal(f.getMontantTotal())
                .setMontantPaye(f.getMontantPaye())
                .setMontantRestant(f.getMontantTotal() - f.getMontantPaye())
                .setStatut(f.getStatut())
                .setDateCreation(f.getDateCreation() != null ? f.getDateCreation().format(FMT) : "")
                .setDescription(f.getDescription() != null ? f.getDescription() : "")
                .build();
    }
}
