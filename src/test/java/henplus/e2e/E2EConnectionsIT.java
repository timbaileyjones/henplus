package henplus.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Connects to whatever real databases are configured in {@link E2EConfig#DEFAULT_PATH}, one dynamic test per target. Only
 * runs via "mvn verify" (see the maven-failsafe-plugin binding in pom.xml) - never on a plain "mvn package"/"mvn test",
 * since that would require real, reachable databases and credentials.
 */
class E2EConnectionsIT {

    @TestFactory
    List<DynamicTest> connectToConfiguredDatabases() throws IOException {
        return E2EConfig.dynamicTestsPerTarget(target -> () -> {
            try (Connection connection = DriverManager.getConnection(target.url, target.username, target.password)) {
                assertTrue(connection.isValid(5), "connection to '" + target.name + "' should be valid");
            }
        });
    }
}
