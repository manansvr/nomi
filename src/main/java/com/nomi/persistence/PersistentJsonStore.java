package com.nomi.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import com.nomi.auth.NomiDataContext;

@Component
public class PersistentJsonStore {
    private final JdbcTemplate jdbcTemplate;
    private final Path dataDirectory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PersistentJsonStore(ObjectProvider<JdbcTemplate> jdbcTemplate, @Value("${nomi.data-directory:data}") String dataDirectory) {
        this.jdbcTemplate = jdbcTemplate.getIfAvailable();
        this.dataDirectory = Path.of(dataDirectory);
        if (this.jdbcTemplate != null) {
            this.jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS nomi_documents (document_key VARCHAR(120) PRIMARY KEY, payload JSONB NOT NULL, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    public <T> Optional<T> read(String fileName, TypeReference<T> type) {
        String documentKey = documentKey(fileName);
        Path file = dataDirectory.resolve(documentKey.replace(':', '_'));
        if (jdbcTemplate == null) {
            return Files.exists(file) ? Optional.ofNullable(LocalJsonStore.read(file, type, null)) : Optional.empty();
        }
        List<String> payloads = jdbcTemplate.query("SELECT payload::text FROM nomi_documents WHERE document_key = ?", (row, i) -> row.getString("payload"), documentKey);
        if (!payloads.isEmpty()) return Optional.of(readJson(payloads.get(0), type, documentKey));
        if (!Files.exists(file)) return Optional.empty();
        T imported = LocalJsonStore.read(file, type, null);
        write(fileName, imported);
        return Optional.ofNullable(imported);
    }

    public void write(String fileName, Object value) {
        String documentKey = documentKey(fileName);
        if (jdbcTemplate == null) { LocalJsonStore.write(dataDirectory.resolve(documentKey.replace(':', '_')), value); return; }
        try {
            String payload = objectMapper.writeValueAsString(value);
            jdbcTemplate.update("INSERT INTO nomi_documents (document_key, payload) VALUES (?, ?::jsonb) ON CONFLICT (document_key) DO UPDATE SET payload = EXCLUDED.payload, updated_at = CURRENT_TIMESTAMP", documentKey, payload);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Could not serialize nomi data for " + fileName, error);
        }
    }
    private String documentKey(String fileName) {
        return NomiDataContext.userId() + ":" + fileName;
    }

    private <T> T readJson(String payload, TypeReference<T> type, String fileName) {
        try { return objectMapper.readValue(payload, type); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Could not read nomi data for " + fileName, error); }
    }
}
