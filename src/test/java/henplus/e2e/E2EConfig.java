package henplus.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Function;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Loads end-to-end database targets from a properties file that deliberately lives outside this repository, so real
 * credentials never get committed. Each target is a group of "&lt;name&gt;.url" (required), "&lt;name&gt;.username" and
 * "&lt;name&gt;.password" (both optional - e.g. SQLite needs neither) keys, e.g.:
 *
 * <pre>
 * postgres.url=jdbc:postgresql://localhost:5432/postgres
 * postgres.username=postgres
 * postgres.password=secret
 *
 * sqlite.url=jdbc:sqlite:/tmp/henplus-e2e-test.db
 * </pre>
 */
public final class E2EConfig {

    public static final Path DEFAULT_PATH = Paths.get(System.getProperty("user.home"), ".config", "henplus",
            "e2e-connectstrings.properties");

    public static final class Target {
        public final String name;
        public final String url;
        public final String username;
        public final String password;

        Target(final String name, final String url, final String username, final String password) {
            this.name = name;
            this.url = url;
            this.username = username;
            this.password = password;
        }
    }

    private final List<Target> targets;

    private E2EConfig(final List<Target> targets) {
        this.targets = targets;
    }

    public static E2EConfig load() throws IOException {
        return load(DEFAULT_PATH);
    }

    public static E2EConfig load(final Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new E2EConfig(Collections.emptyList());
        }

        final Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        }

        final TreeSet<String> names = new TreeSet<>();
        for (final String key : props.stringPropertyNames()) {
            if (key.endsWith(".url")) {
                names.add(key.substring(0, key.length() - ".url".length()));
            }
        }

        final List<Target> result = new ArrayList<>();
        for (final String name : names) {
            result.add(new Target(name, props.getProperty(name + ".url"), props.getProperty(name + ".username"),
                    props.getProperty(name + ".password")));
        }
        return new E2EConfig(result);
    }

    public List<Target> targets() {
        return targets;
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    /**
     * Builds one {@link DynamicTest} per configured target, via the given executable factory. If no targets are
     * configured, this doesn't fail: it prints a warning to stderr and returns a single skipped test inviting you to add
     * some, rather than an empty list (a {@code @TestFactory} returning zero tests reads as "nothing to check", not "check
     * skipped" - this makes the skip visible in the test report).
     */
    public static List<DynamicTest> dynamicTestsPerTarget(final Function<Target, Executable> testBuilder) throws IOException {
        final E2EConfig config = load();

        if (config.isEmpty()) {
            final String message = "No e2e database targets configured. Create " + DEFAULT_PATH
                    + " (copy src/test/resources/e2e-connectstrings.properties.example and fill in what you want to test"
                    + " against) to enable end-to-end tests here.";
            System.err.println("WARNING: " + message);
            return Collections.singletonList(dynamicTest("no e2e targets configured", () -> Assumptions.assumeTrue(false, message)));
        }

        final List<DynamicTest> tests = new ArrayList<>();
        for (final Target target : config.targets()) {
            tests.add(dynamicTest(target.name, testBuilder.apply(target)));
        }
        return tests;
    }
}
