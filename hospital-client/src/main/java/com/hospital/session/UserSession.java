package com.hospital.session;

import java.util.Objects;

public final class UserSession {

    public enum Role {
        ADMIN, MEDECIN, INFIRMIER, LABORANTIN, PHARMACIEN, CAISSIER, RECEPTIONNISTE
    }

    private static volatile UserSession instance;

    private String username;
    private String fullName;
    private Role role;
    private String token;
    private boolean authenticated;
    private long medecinId;  // ID du médecin (0 si non défini)

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    public void login(String username, String fullName, Role role) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.authenticated = true;
        this.medecinId = 0; // reset
    }

    public void logout() {
        this.username = null;
        this.fullName = null;
        this.role = null;
        this.token = null;
        this.authenticated = false;
        this.medecinId = 0;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName != null ? fullName : username;
    }

    public Role getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getMedecinId() {
        return medecinId;
    }

    public void setMedecinId(long medecinId) {
        this.medecinId = medecinId;
    }

    public boolean hasRole(Role... roles) {
        if (role == null) return false;
        for (Role r : roles) {
            if (role == r) return true;
        }
        return false;
    }

    public boolean canAccessAdmin() {
        return hasRole(Role.ADMIN);
    }

    public boolean canAccessPatients() {
        return hasRole(Role.ADMIN, Role.RECEPTIONNISTE, Role.MEDECIN, Role.INFIRMIER);
    }

    public boolean canAccessPharmacie() {
        return hasRole(Role.ADMIN, Role.PHARMACIEN);
    }

    public boolean canAccessPaiement() {
        return hasRole(Role.ADMIN, Role.CAISSIER, Role.RECEPTIONNISTE);
    }
}