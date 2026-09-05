package com.hospital.exception;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Exception métier côté client, avec message compréhensible pour l'utilisateur.
 */
public class GrpcClientException extends RuntimeException {

    private final Status.Code code;

    public GrpcClientException(String userMessage, Status.Code code) {
        super(userMessage);
        this.code = code;
    }

    public GrpcClientException(String userMessage, Status.Code code, Throwable cause) {
        super(userMessage, cause);
        this.code = code;
    }

    public Status.Code getCode() {
        return code;
    }

    /**
     * Transforme une StatusRuntimeException gRPC en message utilisateur clair.
     */
    public static GrpcClientException from(StatusRuntimeException e) {
        Status.Code code = e.getStatus().getCode();
        String description = e.getStatus().getDescription();
        String userMessage = switch (code) {
            case NOT_FOUND -> description != null && !description.isBlank()
                    ? description : "Élément introuvable.";
            case INVALID_ARGUMENT -> description != null && !description.isBlank()
                    ? description : "Données invalides. Vérifiez les informations saisies.";
            case ALREADY_EXISTS -> description != null && !description.isBlank()
                    ? description : "Cet élément existe déjà.";
            case FAILED_PRECONDITION -> description != null && !description.isBlank()
                    ? description : "Opération impossible dans l'état actuel.";
            case PERMISSION_DENIED -> "Vous n'avez pas les droits nécessaires pour cette opération.";
            case UNAUTHENTICATED -> "Authentification requise. Veuillez vous reconnecter.";
            case UNAVAILABLE -> "Impossible de contacter le serveur. Vérifiez votre connexion réseau et que le serveur est démarré.";
            case DEADLINE_EXCEEDED -> "Le serveur met trop de temps à répondre. Réessayez plus tard.";
            case INTERNAL -> "Erreur interne du serveur. Contactez l'administrateur.";
            default -> "Une erreur est survenue : " + (description != null ? description : code.name());
        };
        return new GrpcClientException(userMessage, code, e);
    }
}
