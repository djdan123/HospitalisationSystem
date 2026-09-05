# Hospital Management System — Client

Application desktop **JavaFX** professionnelle communiquant exclusivement via **gRPC** avec les microservices hospitaliers.

## Architecture

```
MACHINE CLIENT (192.168.1.20)
        │
        │  gRPC / HTTP/2
        ▼
MACHINE SERVEUR (192.168.1.10)
   ├── :50051  Accueil
   ├── :50052  Hospitalisation
   ├── :50053  Paiement
   ├── :50054  Consultation
   ├── :50055  Laboratoire
   ├── :50056  Pharmacie
   └── :50057  Maternité
```

Le client **ne se connecte jamais** directement à MySQL.

## Technologies

- Java 25 LTS
- Apache Maven
- JavaFX 21
- gRPC + Protocol Buffers
- Ikonli (icônes)
- SLF4J / Logback
- JUnit 5

## Prérequis

1. **Java 25** installé (`java -version`)
2. **Maven 3.9+** (`mvn -version`)
3. Machine serveur démarrée (microservices gRPC + MySQL)

## Configuration

Éditer `src/main/resources/application.properties` :

```properties
grpc.server.host=192.168.1.10
grpc.accueil.port=50051
grpc.hospitalisation.port=50052
# ...
```

## Compilation

```bash
cd hospital-client
mvn clean install
```

Les classes Java sont générées automatiquement à partir des fichiers `.proto`.

## Lancement

```bash
# Avec le plugin JavaFX
mvn javafx:run

# Ou via le JAR
java --module-path <chemin-javafx> --add-modules javafx.controls,javafx.fxml -jar target/hospital-client.jar
```

Sous Windows / Linux avec JavaFX inclus dans le runtime :

```bash
java -jar target/hospital-client.jar
```

## Comptes de démonstration

| Identifiant | Mot de passe | Rôle            |
|-------------|--------------|-----------------|
| admin       | admin123     | ADMIN           |
| medecin     | med123       | MEDECIN         |
| recep       | recep123     | RECEPTIONNISTE  |
| pharma      | pharma123    | PHARMACIEN      |
| caisse      | caisse123    | CAISSIER        |

## Fonctionnalités

- **Login** moderne avec indicateur de connexion serveur
- **Dashboard** avec cartes statistiques (patients, hospitalisations, médicaments…)
- **Patients** : liste, recherche, création, modification, désactivation (gRPC Accueil)
- **Sidebar** de navigation
- **Paramètres** : test de connexion serveur
- Clients gRPC prêts pour : Hospitalisation, Consultation, Laboratoire, Pharmacie, Paiement, Maternité
- Gestion propre des erreurs gRPC (messages utilisateur clairs)
- Appels réseau asynchrones (ne bloque pas l’UI)

## Structure

```
hospital-client/
├── pom.xml
├── src/main/
│   ├── java/com/hospital/
│   │   ├── HospitalClientApplication.java
│   │   ├── config/          # AppConfig, GrpcConfig
│   │   ├── grpc/            # Clients gRPC
│   │   ├── controller/      # Contrôleurs JavaFX
│   │   ├── session/         # UserSession
│   │   ├── exception/
│   │   └── util/
│   ├── proto/               # Fichiers .proto (identiques au serveur)
│   └── resources/
│       ├── fxml/
│       ├── css/style.css
│       └── application.properties
└── README.md
```

## Dépannage

| Problème | Solution |
|----------|----------|
| Serveur déconnecté | Vérifier que les microservices tournent sur 192.168.1.10 |
| Port inaccessible | Firewall / `telnet 192.168.1.10 50051` |
| Classes protobuf manquantes | `mvn clean compile` |
| JavaFX non trouvé | Installer JavaFX ou utiliser un JDK qui l’inclut |

## Licence

Projet éducatif / professionnel.
