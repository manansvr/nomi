package com.nomi.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nomi.auth.NomiDataContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentJsonStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void storesDocumentsSeparatelyForEachUser() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        ObjectProvider<org.springframework.jdbc.core.JdbcTemplate> provider =
                factory.getBeanProvider(org.springframework.jdbc.core.JdbcTemplate.class);
        PersistentJsonStore store = new PersistentJsonStore(provider, tempDir.toString());

        try {
            NomiDataContext.setUserId("user-a");
            store.write("pantry.json", List.of("apples"));

            NomiDataContext.setUserId("user-b");
            assertTrue(store.read("pantry.json", new TypeReference<List<String>>() { }).isEmpty());
            store.write("pantry.json", List.of("rice"));

            NomiDataContext.setUserId("user-a");
            assertEquals(List.of("apples"), store.read("pantry.json", new TypeReference<List<String>>() { }).orElseThrow());
        } finally {
            NomiDataContext.clear();
        }
    }
}
