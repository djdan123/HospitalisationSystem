# Hospital Microservices Server (gRPC)

Serveur professionnel de gestion hospitalière basé sur une **architecture microservices** utilisant **gRPC** exclusivement pour la communication client ↔ serveur.

## Architecture

| Service            | Port  | Module                  |
|--------------------|-------|-------------------------|
| Accueil            | 50051 | `accueil-service`       |
| Hospitalisation    | 50052 | `hospitalisation-service` |
| Paiement           | 50053 | `paiement-service`      |
| Consultation       | 50054 | `consultation-service`  |
| Laboratoire        | 50055 | `laboratoire-service`   |
| Pharmacie          | 50056 | `pharmacie-service`     |
| Maternité          | 50057 | `maternite-service`     |

- **IP serveur recommandée** : `192.168.1.10`
- **Base de données** : MySQL (`hospital_db`)
- **Java** : 21 (LTS)
- **Build** : Apache Maven
- **Communication** : gRPC / Protocol Buffers (proto3) sur HTTP/2

## Prérequis

1. **Java 21** (OpenJDK ou Oracle)
2. **Apache Maven 3.9+**
3. **MySQL 8.x**
4. (Optionnel) Docker & Docker Compose

## Installation MySQL

```bash
# Créer la base et l'utilisateur
mysql -u root -p < database/schema.sql
mysql -u root -p hospital_db < database/data.sql
```

Ou avec Docker :

```bash
docker-compose up -d mysql
```

## Configuration

Éditez les fichiers `*/src/main/resources/application.properties` de chaque service :

```properties
server.ip=192.168.1.10
spring.datasource.url=jdbc:mysql://192.168.1.10:3306/hospital_db?...
spring.datasource.username=hospital
spring.datasource.password=hospital123
```

**Important** : ne jamais committer de vrais mots de passe en production.

## Compilation

À la racine du projet :

```bash
mvn clean install
```

ou

```bash
mvn clean package -DskipTests
```

Les classes Java sont générées automatiquement à partir des fichiers `.proto`.

## Lancement des services

Chaque service est un Spring Boot indépendant. Lancez-les un par un (ou en parallèle) :

```bash
# Accueil
cd accueil-service && mvn spring-boot:run

# Hospitalisation
cd hospitalisation-service && mvn spring-boot:run

# Paiement
cd paiement-service && mvn spring-boot:run

# Consultation
cd consultation-service && mvn spring-boot:run

# Laboratoire
cd laboratoire-service && mvn spring-boot:run

# Pharmacie
cd pharmacie-service && mvn spring-boot:run

# Maternité
cd maternite-service && mvn spring-boot:run
```

Ou via les JARs générés :

```bash
java -jar accueil-service/target/accueil-service-1.0.0-SNAPSHOT.jar
# etc.
```

## Vérification

Au démarrage, chaque service affiche un message du type :

```
SERVICE ACCUEIL gRPC démarré sur 192.168.1.10:50051
```

Depuis la machine client :

```bash
ping 192.168.1.10
# Tester les ports (ex. avec nmap ou telnet)
telnet 192.168.1.10 50051
```

## Structure des dossiers

```
hospital-server/
├── pom.xml                          # Parent multi-module
├── common/                          # Exceptions, interceptors, utils
├── accueil-service/
├── hospitalisation-service/
├── paiement-service/
├── consultation-service/
├── laboratoire-service/
├── pharmacie-service/
├── maternite-service/
├── database/
│   ├── schema.sql
│   └── data.sql
├── docker-compose.yml
└── README.md
```

## Tests

```bash
mvn test
```

## Client gRPC

Le client (machine distante) doit :

1. Utiliser les mêmes fichiers `.proto`
2. Générer les stubs Java (ou autre langage)
3. Se connecter aux adresses `192.168.1.10:5005x`

Exemple de channel :

```java
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("192.168.1.10", 50051)
    .usePlaintext()   // pour les tests locaux
    .build();
```

## Sécurité (prochaine version)

Le code est structuré pour ajouter facilement :

- TLS (certificats)
- Intercepteur JWT / token
- Contrôle des permissions

Pour le moment, le serveur tourne en plaintext pour faciliter les tests sur le réseau local.

## Résolution des problèmes courants

| Problème | Solution |
|----------|----------|
| `Connection refused` | Vérifier que le service est démarré et le port ouvert dans le firewall |
| Erreur MySQL | Vérifier IP, utilisateur, mot de passe et que MySQL écoute sur `0.0.0.0` |
| Classes protobuf non générées | `mvn clean compile` |
| Port déjà utilisé | Changer le port dans `application.properties` |

## Licence

Projet éducatif / professionnel – utilisation libre.
