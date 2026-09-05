package com.hospital.grpc;

import com.hospital.config.AppConfig;
import com.hospital.config.GrpcConfig;
import com.hospital.exception.GrpcClientException;
import com.hospital.grpc.paiement.*;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client gRPC pour le service de paiement.
 */
public class PaiementClient {

    private final ManagedChannel channel;

    public PaiementClient() {
        this.channel = GrpcConfig.getPaiementChannel();
    }

    private PaiementServiceGrpc.PaiementServiceBlockingStub stub() {
        return PaiementServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(AppConfig.getDeadlineSeconds(), TimeUnit.SECONDS);
    }

    // ---------- Factures ----------
    public Facture createFacture(long patientId, double montant, String description) {
        try {
            return stub().createFacture(CreateFactureRequest.newBuilder()
                    .setPatientId(patientId)
                    .setMontantTotal(montant)
                    .setDescription(description != null ? description : "")
                    .build()).getFacture();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Facture getFacture(long id) {
        try {
            return stub().getFacture(GetFactureRequest.newBuilder().setId(id).build()).getFacture();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Facture> getFacturesByPatient(long patientId) {
        try {
            return stub().getFacturesByPatient(
                    GetFacturesByPatientRequest.newBuilder().setPatientId(patientId).build()
            ).getFacturesList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    // ---------- Paiements ----------
    public Payment makePayment(long factureId, double montant, String mode, String reference) {
        try {
            return stub().makePayment(MakePaymentRequest.newBuilder()
                    .setFactureId(factureId)
                    .setMontant(montant)
                    .setModePaiement(mode != null ? mode : "")
                    .setReference(reference != null ? reference : "")
                    .build()).getPayment();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public Payment getPayment(long id) {
        try {
            return stub().getPayment(GetPaymentRequest.newBuilder().setId(id).build()).getPayment();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    public List<Payment> getPaymentsByPatient(long patientId) {
        try {
            return stub().getPaymentsByPatient(
                    GetPaymentsByPatientRequest.newBuilder().setPatientId(patientId).build()
            ).getPaymentsList();
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }

    // ---------- Reçus ----------
    public ReceiptResponse generateReceipt(long paymentId) {
        try {
            return stub().generateReceipt(GenerateReceiptRequest.newBuilder()
                    .setPaymentId(paymentId)
                    .build());
        } catch (StatusRuntimeException e) {
            throw GrpcClientException.from(e);
        }
    }
}