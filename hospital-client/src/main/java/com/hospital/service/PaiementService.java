package com.hospital.service;

import com.hospital.grpc.PaiementClient;
import com.hospital.grpc.paiement.Facture;
import com.hospital.grpc.paiement.Payment;
import com.hospital.grpc.paiement.ReceiptResponse;

import java.util.List;

public class PaiementService {

    private final PaiementClient client = new PaiementClient();

    public Facture createFacture(long patientId, double montant, String description) {
        return client.createFacture(patientId, montant, description);
    }

    public Facture getFacture(long id) {
        return client.getFacture(id);
    }

    public List<Facture> getFacturesByPatient(long patientId) {
        return client.getFacturesByPatient(patientId);
    }

    public Payment makePayment(long factureId, double montant, String mode, String reference) {
        return client.makePayment(factureId, montant, mode, reference);
    }

    public Payment getPayment(long id) {
        return client.getPayment(id);
    }

    public List<Payment> getPaymentsByPatient(long patientId) {
        return client.getPaymentsByPatient(patientId);
    }

    public ReceiptResponse generateReceipt(long paymentId) {
        return client.generateReceipt(paymentId);
    }
}
