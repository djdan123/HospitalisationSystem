USE hospital_db;

-- Patients de démonstration
INSERT INTO patients (numero_dossier, nom, prenom, date_naissance, sexe, telephone, email, adresse, date_creation, statut) VALUES
('DOS-2026-001', 'DUPONT', 'Marie', '1990-05-15', 'F', '+221771234567', 'marie.dupont@email.com', 'Dakar, Plateau', NOW(), 'ACTIF'),
('DOS-2026-002', 'DIOP', 'Amadou', '1985-11-22', 'M', '+221781112233', 'amadou.diop@email.com', 'Thiès', NOW(), 'ACTIF'),
('DOS-2026-003', 'NDIAYE', 'Fatou', '1995-03-08', 'F', '+221761234567', 'fatou.ndiaye@email.com', 'Saint-Louis', NOW(), 'ACTIF');

-- Médicaments de démonstration
INSERT INTO medicaments (code, nom, description, stock, prix_unitaire, unite, statut) VALUES
('MED-001', 'Paracétamol 500mg', 'Antalgique et antipyrétique', 500, 2.50, 'comprimé', 'DISPONIBLE'),
('MED-002', 'Amoxicilline 500mg', 'Antibiotique', 200, 15.00, 'gélule', 'DISPONIBLE'),
('MED-003', 'Ibuprofène 400mg', 'Anti-inflammatoire', 300, 5.00, 'comprimé', 'DISPONIBLE');
