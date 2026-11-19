package com.nomi.auth;

public record AuthResult(boolean success, String error, AuthUser user) {
    public static AuthResult success(AuthUser user) { return new AuthResult(true, null, user); }
    public static AuthResult failure(String error) { return new AuthResult(false, error, null); }
}
