package com.nomi.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nomi.persistence.LocalJsonStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Service
public class AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Path usersFile;

    public AuthService(ObjectProvider<JdbcTemplate> jdbcTemplate,
                       @Value("${nomi.data-directory:data}") String dataDirectory) {
        this.jdbcTemplate = jdbcTemplate.getIfAvailable();
        this.usersFile = Path.of(dataDirectory).resolve("users.json");
        if (this.jdbcTemplate != null) {
            this.jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS nomi_users (id VARCHAR(36) PRIMARY KEY, name VARCHAR(80) NOT NULL, email VARCHAR(254) UNIQUE NOT NULL, password_hash VARCHAR(100) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    public synchronized AuthResult signUp(String name, String email, String password) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = normalizeEmail(email);
        if (normalizedName.isEmpty() || normalizedName.length() > 80 || !isValidEmail(normalizedEmail)
                || password == null || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return AuthResult.failure("Enter a valid name and email and a password with 8 to 72 UTF-8 bytes.");
        }
        if (findByEmail(normalizedEmail).isPresent()) {
            return AuthResult.failure("An account with this email already exists.");
        }
        AuthUser user = new AuthUser(UUID.randomUUID().toString(), normalizedName, normalizedEmail);
        String hash = passwordEncoder.encode(password);
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.update("INSERT INTO nomi_users (id, name, email, password_hash) VALUES (?, ?, ?, ?)", user.id(), user.name(), user.email(), hash);
            } catch (DuplicateKeyException duplicateEmail) {
                return AuthResult.failure("An account with this email already exists.");
            }
        } else {
            List<StoredUser> users = readLocalUsers();
            users.add(new StoredUser(user.id(), user.name(), user.email(), hash));
            LocalJsonStore.write(usersFile, users);
        }
        return AuthResult.success(user);
    }

    public AuthResult signIn(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty() || password == null) return AuthResult.failure("Enter your email and password.");
        Optional<StoredUser> match = findByEmail(normalizedEmail);
        if (match.isEmpty() || !passwordEncoder.matches(password, match.get().passwordHash())) {
            return AuthResult.failure("Email or password is incorrect.");
        }
        StoredUser user = match.get();
        return AuthResult.success(new AuthUser(user.id(), user.name(), user.email()));
    }

    private Optional<StoredUser> findByEmail(String email) {
        if (jdbcTemplate != null) {
            List<StoredUser> users = jdbcTemplate.query(
                    "SELECT id, name, email, password_hash FROM nomi_users WHERE lower(email) = ?",
                    (row, i) -> new StoredUser(row.getString("id"), row.getString("name"), row.getString("email"), row.getString("password_hash")),
                    email);
            return users.stream().findFirst();
        }
        return readLocalUsers().stream().filter(user -> user.email().equalsIgnoreCase(email)).findFirst();
    }

    private List<StoredUser> readLocalUsers() {
        if (!Files.exists(usersFile)) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(LocalJsonStore.read(usersFile, new TypeReference<List<StoredUser>>() { }, List.of()));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String email) {
        return email.length() <= 254 && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private record StoredUser(String id, String name, String email, String passwordHash) {}
}
