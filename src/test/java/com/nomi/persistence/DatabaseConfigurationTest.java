package com.nomi.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigurationTest {
    @Test
    void convertsMarketplacePostgresUrlToJdbcSettings() {
        MockEnvironment environment = new MockEnvironment().withProperty(
                "DATABASE_URL",
                "postgresql://nomi%40user:p%2Bss@db.example:5432/nomi?sslmode=require");

        DatabaseConfiguration.ConnectionDetails connection = DatabaseConfiguration.parseConnection(
                environment.getProperty("DATABASE_URL"), environment);

        assertEquals("jdbc:postgresql://db.example:5432/nomi?sslmode=require", connection.jdbcUrl());
        assertEquals("nomi@user", connection.username());
        assertEquals("p+ss", connection.password());
    }
}
