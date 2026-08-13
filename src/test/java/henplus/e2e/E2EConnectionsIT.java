package henplus.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Connects to whatever real databases are configured in {@link E2EConfig#DEFAULT_PATH}, one dynamic test per target. Only
 * runs via "mvn verify" (see the maven-failsafe-plugin binding in pom.xml) - never on a plain "mvn package"/"mvn test",
 * since that would require real, reachable databases and credentials.
 *
 * If no config file exists, this doesn't fail: it prints a warning and produces a single skipped test inviting you to add
 * one.
 */
class E2EConnectionsIT {

    @TestFactory
    List<DynamicTest> connectToConfiguredDatabases() throws IOException {
        final E2EConfig config = E2EConfig.load();

        if (config.isEmpty()) {
            final String message = "No e2e database targets configured. Create " + E2EConfig.DEFAULT_PATH
                    + " with entries like:\n\n" + "  postgres.url=jdbc:postgresql://localhost:5432/postgres\n"
                    + "  postgres.username=postgres\n" + "  postgres.password=secret\n\n"
                    + "(username/password are optional, e.g. for SQLite) to enable end-to-end connectivity tests here.";
            System.err.println("WARNING: " + message);
            return Collections.singletonList(dynamicTest("no e2e targets configured", () -> Assumptions.assumeTrue(false, message)));
        }

        final List<DynamicTest> tests = new ArrayList<>();
        for (final E2EConfig.Target target : config.targets()) {
            tests.add(dynamicTest(target.name, () -> {
                try (Connection connection = DriverManager.getConnection(target.url, target.username, target.password)) {
                    assertTrue(connection.isValid(5), "connection to '" + target.name + "' should be valid");
                }
            }));
        }
        return tests;
    }
}
