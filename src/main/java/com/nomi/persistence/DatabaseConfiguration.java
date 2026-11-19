package com.nomi.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@Conditional(DatabaseConfiguration.DatabaseUrlConfigured.class)
public class DatabaseConfiguration {
    @Bean
    public DataSource nomiDataSource(Environment environment) {
        String value = first(environment.getProperty("DATABASE_URL"), environment.getProperty("POSTGRES_URL"), environment.getProperty("SPRING_DATASOURCE_URL"));
        ConnectionDetails connection = parse(value, environment);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connection.jdbcUrl());
        if (connection.username() != null) config.setUsername(connection.username());
        if (connection.password() != null) config.setPassword(connection.password());
        config.setMaximumPoolSize(5); config.setMinimumIdle(0); config.setConnectionTimeout(10000); config.setPoolName("nomi-postgres");
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate nomiJdbcTemplate(DataSource nomiDataSource) { return new JdbcTemplate(nomiDataSource); }

    @Bean
    public PlatformTransactionManager nomiTransactionManager(DataSource nomiDataSource) {
        return new DataSourceTransactionManager(nomiDataSource);
    }

    static ConnectionDetails parse(String value, Environment environment) {
        String username = first(environment.getProperty("SPRING_DATASOURCE_USERNAME"), environment.getProperty("POSTGRES_USER"), environment.getProperty("PGUSER"));
        String password = first(environment.getProperty("SPRING_DATASOURCE_PASSWORD"), environment.getProperty("POSTGRES_PASSWORD"), environment.getProperty("PGPASSWORD"));
        if (value.startsWith("jdbc:")) return new ConnectionDetails(value, username, password);
        URI uri = URI.create(value);
        if (!"postgres".equals(uri.getScheme()) && !"postgresql".equals(uri.getScheme())) throw new IllegalStateException("DATABASE_URL must be PostgreSQL");
        String raw = uri.getRawUserInfo();
        if (raw != null) {
            int separator = raw.indexOf(':');
            if (username == null) username = decode(separator < 0 ? raw : raw.substring(0, separator));
            if (password == null && separator >= 0) password = decode(raw.substring(separator + 1));
        }
        String authority = uri.getRawAuthority();
        String host = raw == null ? authority : authority.substring(raw.length() + 1);
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return new ConnectionDetails("jdbc:postgresql://" + host + path + query, username, password);
    }

    private static String decode(String value) { return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8); }
    private static String first(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value; return null; }
    record ConnectionDetails(String jdbcUrl, String username, String password) { }

    public static class DatabaseUrlConfigured implements Condition {
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return first(environment.getProperty("DATABASE_URL"), environment.getProperty("POSTGRES_URL"), environment.getProperty("SPRING_DATASOURCE_URL")) != null;
        }
    }
}
