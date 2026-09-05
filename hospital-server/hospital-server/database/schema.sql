-- ============================================================
-- HOSPITAL MICROSERVICES - Schema MySQL
-- Base: hospital_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS hospital_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_db;

-- Patients (Service Accueil)
CREATE TABLE IF NOT EXISTS patients (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_dossier  VARCHAR(50)  NOT NULL UNIQUE,
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    date_naissance  DATE         NOT NULL,
    sexe            VARCHAR(20)  NOT NULL,
    telephone       VARCHAR(20),
    email           VARCHAR(150),
    adresse         VARCHAR(255),
    date_creation   DATETIME     NOT NULL,
    statut          VARCHAR(20)  NOT NULL DEFAULT 'ACTIF',
    INDEX idx_patients_nom (nom),
    INDEX idx_patients_statut (statut)
) ENGINE=InnoDB;

-- Hospitalisations
CREATE TABLE IF NOT EXISTS hospitalisations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id      BIGINT       NOT NULL,
    date_admission  DATETIME     NOT NULL,
    date_sortie     DATETIME,
    motif           VARCHAR(500),
    statut          VARCHAR(30)  NOT NULL DEFAULT 'EN_COURS',
    chambre_id      BIGINT,
    lit_id          BIGINT,
    numero_chambre  VARCHAR(20),
    numero_lit      VARCHAR(20),
    observations    VARCHAR(1000),
    INDEX idx_hosp_patient (patient_id),
    INDEX idx_hosp_statut (statut)
) ENGINE=InnoDB;

-- Consultations
CREATE TABLE IF NOT EXISTS consultations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id          BIGINT       NOT NULL,
    medecin_id          BIGINT,
    date_consultation   DATETIME,
    motif               VARCHAR(500),
    diagnostic          TEXT,
    observations        TEXT,
    prescription        TEXT,
    statut              VARCHAR(30)  NOT NULL DEFAULT 'PLANIFIEE',
    INDEX idx_cons_patient (patient_id),
    INDEX idx_cons_medecin (medecin_id)
) ENGINE=InnoDB;

-- Analyses laboratoire
CREATE TABLE IF NOT EXISTS analyses (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id          BIGINT       NOT NULL,
    type_analyse        VARCHAR(150),
    date_demande        DATETIME,
    date_prelevement    DATETIME,
    statut              VARCHAR(30)  NOT NULL DEFAULT 'DEMANDEE',
    observations        TEXT,
    INDEX idx_ana_patient (patient_id)
) ENGINE=InnoDB;

-- Médicaments
CREATE TABLE IF NOT EXISTS medicaments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(50) UNIQUE,
    nom             VARCHAR(150),
    description     TEXT,
    stock           INT DEFAULT 0,
    prix_unitaire   DOUBLE DEFAULT 0,
    unite           VARCHAR(30),
    statut          VARCHAR(30) DEFAULT 'DISPONIBLE'
) ENGINE=InnoDB;

-- Factures
CREATE TABLE IF NOT EXISTS factures (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id      BIGINT       NOT NULL,
    numero_facture  VARCHAR(50)  UNIQUE,
    montant_total   DOUBLE DEFAULT 0,
    montant_paye    DOUBLE DEFAULT 0,
    statut          VARCHAR(30)  DEFAULT 'IMPAYEE',
    date_creation   DATETIME,
    description     TEXT,
    INDEX idx_fac_patient (patient_id)
) ENGINE=InnoDB;

-- Dossiers maternité
CREATE TABLE IF NOT EXISTS dossiers_maternite (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id                  BIGINT       NOT NULL,
    date_ouverture              DATETIME,
    date_dernieres_regles       DATE,
    date_prevue_accouchement    DATE,
    nombre_grossesses           INT DEFAULT 1,
    groupe_sanguin              VARCHAR(10),
    statut                      VARCHAR(30)  DEFAULT 'OUVERT',
    observations                TEXT,
    INDEX idx_mat_patient (patient_id)
) ENGINE=InnoDB;

-- Utilisateur application (optionnel)
CREATE USER IF NOT EXISTS 'hospital'@'%' IDENTIFIED BY 'hospital123';
GRANT ALL PRIVILEGES ON hospital_db.* TO 'hospital'@'%';
FLUSH PRIVILEGES;
