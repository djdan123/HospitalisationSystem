package com.hospital.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServiceLayerSmokeTest {

    @Test
    void allBusinessServicesShouldExist() throws Exception {
        assertNotNull(Class.forName("com.hospital.service.ConsultationService"));
        assertNotNull(Class.forName("com.hospital.service.HospitalisationService"));
        assertNotNull(Class.forName("com.hospital.service.LaboratoireService"));
        assertNotNull(Class.forName("com.hospital.service.MaterniteService"));
        assertNotNull(Class.forName("com.hospital.service.PaiementService"));
        assertNotNull(Class.forName("com.hospital.service.PharmacieService"));
    }
}
