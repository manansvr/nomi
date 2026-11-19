package com.nomi.auth;

import com.nomi.persistence.DatabaseConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DurableSessionConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DatabaseConfiguration.class, DurableSessionConfiguration.class)
            .withPropertyValues("DATABASE_URL=jdbc:h2:mem:nomi-sessions;DB_CLOSE_DELAY=-1");

    @Test
    void storesAuthenticatedSessionsInTheConfiguredDatabase() {
        contextRunner.run(context -> {
            assertNotNull(context.getBean(SessionRepository.class));
            verifySessionRepository(context.getBean(SessionRepository.class));
        });
    }

    private <S extends Session> void verifySessionRepository(SessionRepository<S> repository) {
        S session = repository.createSession();
        AuthUser user = new AuthUser("user-1", "Nomi User", "user@example.com");
        session.setAttribute("currentUser", user);

        repository.save(session);

        S loaded = repository.findById(session.getId());
        assertNotNull(loaded);
        assertEquals(user, loaded.getAttribute("currentUser"));
    }
}
