package com.nomi.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void signUpAndSignInWithEmailPassword() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        ObjectProvider<org.springframework.jdbc.core.JdbcTemplate> provider = factory.getBeanProvider(org.springframework.jdbc.core.JdbcTemplate.class);
        AuthService service = new AuthService(provider, tempDir.toString());

        AuthResult signUp = service.signUp("Manan", "Manan@example.com", "password123");
        assertTrue(signUp.success());
        assertEquals("manan@example.com", signUp.user().email());

        assertFalse(service.signUp("Manan", "manan@example.com", "password123").success());

        AuthResult signIn = service.signIn("MANAN@example.com", "password123");
        assertTrue(signIn.success());
        assertEquals("Manan", signIn.user().name());

        assertFalse(service.signIn("manan@example.com", "wrongpassword").success());
    }

    @Test
    void rejectsMalformedEmailAndPasswordsOutsideBcryptByteLimit() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        ObjectProvider<org.springframework.jdbc.core.JdbcTemplate> provider = factory.getBeanProvider(org.springframework.jdbc.core.JdbcTemplate.class);
        AuthService service = new AuthService(provider, tempDir.toString());

        assertFalse(service.signUp("Manan", "not-an-email", "password123").success());
        assertFalse(service.signUp("Manan", "manan@example.com", "é".repeat(37)).success());
    }
}
