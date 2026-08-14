package henplus.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import henplus.Command;
import henplus.HenPlusHarness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Exercises henplus's real {@code dump-out}/{@code dump-in} commands (via {@link HenPlusHarness}, not raw JDBC) in a
 * full create -&gt; index -&gt; view -&gt; insert -&gt; dump-out -&gt; delete -&gt; dump-in -&gt; count -&gt; drop round
 * trip - unlike {@link E2EConnectionsIT}/{@link E2EDiscoveryIT}, this one is genuinely destructive.
 *
 * <p>Two independent safety gates must both pass before anything destructive runs against a target: the config must set
 * "&lt;name&gt;.destructive=true", <em>and</em> the connection's actual {@code getCatalog()} - not the URL string, the
 * driver's own live answer - must equal literally "e2e-destructive". Either check failing skips cleanly rather than
 * failing the build, same as the rest of this suite. Only runs via "mvn verify".
 */
class E2EDestructiveIT {

    private static final String TABLE = "e2e_destructive_probe";
    private static final String INDEX = "e2e_destructive_probe_name_idx";
    private static final String VIEW = "e2e_destructive_probe_view";

    @TestFactory
    List<DynamicTest> dumpAndReloadRoundTrip() throws IOException {
        return E2EConfig.dynamicTestsPerTarget(target -> () -> {
            Assumptions.assumeTrue(target.destructive,
                    "'" + target.name + "' is not flagged '" + target.name + ".destructive=true' - skipping.");

            try (Connection probe = DriverManager.getConnection(target.url, target.username, target.password)) {
                final String catalog = probe.getCatalog();
                Assumptions.assumeTrue("e2e-destructive".equals(catalog), "'" + target.name
                        + "' is flagged destructive, but is connected to database '" + catalog + "', not "
                        + "'e2e-destructive' - refusing to run destructive operations against it. Point this target's "
                        + ".url at a database literally named 'e2e-destructive' to enable.");
            }

            runDestructiveWorkflow(target);
        });
    }

    private static void runDestructiveWorkflow(final E2EConfig.Target target) throws Exception {
        final Path dumpFile = Files.createTempFile("henplus-e2e-destructive-", ".dump");
        try (Connection connection = DriverManager.getConnection(target.url, target.username, target.password)) {
            try {
                final int insertedRows = createAndPopulate(connection);

                try (HenPlusHarness harness = new HenPlusHarness(target.url, target.username, target.password)) {
                    // No trailing ";" here: that's only needed in interactive use, where the statement
                    // separator/dispatcher strips it before Command.execute() ever sees it. Calling
                    // execute() directly (as HenPlusHarness does) skips that layer entirely, so a literal
                    // ";" would end up glued onto the table name token instead.
                    final int dumpResult = harness.run("dump-out", dumpFile.toAbsolutePath() + " " + TABLE);
                    assertEquals(Command.SUCCESS, dumpResult, "dump-out should succeed");

                    try (Statement st = connection.createStatement()) {
                        // not TRUNCATE: SQLite has no TRUNCATE statement, DELETE FROM with no WHERE is
                        // universally supported and sufficient for this tiny dataset.
                        st.execute("DELETE FROM " + TABLE);
                    }

                    // The trailing "1000" is a required commit-interval, not a row-count cap: dump-in only
                    // calls conn.commit() at all - even its one final commit after all rows are read - when
                    // this parameter is present and >= 0. Omit it and the insert genuinely never commits:
                    // it reports success/"N rows total" and then silently rolls back when the connection
                    // closes, which is exactly what happened here before this fix.
                    final int reloadResult = harness.run("dump-in", dumpFile.toAbsolutePath() + " 1000");
                    assertEquals(Command.SUCCESS, reloadResult, "dump-in should succeed");
                }

                try (Statement st = connection.createStatement();
                        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + TABLE)) {
                    rs.next();
                    assertEquals(insertedRows, rs.getInt(1), "row count after dump-out/delete/dump-in round trip");
                }
            } finally {
                cleanup(connection);
                Files.deleteIfExists(dumpFile);
            }
        }
    }

    private static int createAndPopulate(final Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE " + TABLE + " (id INTEGER PRIMARY KEY, name VARCHAR(100))");
            st.execute("CREATE INDEX " + INDEX + " ON " + TABLE + "(name)");
            st.execute("CREATE VIEW " + VIEW + " AS SELECT * FROM " + TABLE);
            st.execute("INSERT INTO " + TABLE + " (id, name) VALUES (1, 'alice')");
            st.execute("INSERT INTO " + TABLE + " (id, name) VALUES (2, 'bob')");
            st.execute("INSERT INTO " + TABLE + " (id, name) VALUES (3, 'carol')");
        }
        return 3;
    }

    /**
     * Best-effort: each DROP gets its own try/catch that logs and swallows failures, rather than requiring
     * vendor-specific "IF EXISTS" syntax (Oracle notably has none on DROP) or a single universal DROP INDEX form
     * (MySQL/MariaDB require "DROP INDEX x ON table" - the plain form used here is what Postgres/SQLite/H2/Oracle/DB2/
     * SQL Server expect). DROP TABLE removes its indexes regardless, so this never leaves orphaned objects behind even
     * if the DROP INDEX step itself is skipped on a vendor that rejects its syntax.
     */
    private static void cleanup(final Connection connection) {
        for (final String ddl : new String[] { "DROP VIEW " + VIEW, "DROP INDEX " + INDEX, "DROP TABLE " + TABLE }) {
            try (Statement st = connection.createStatement()) {
                st.execute(ddl);
            } catch (final SQLException e) {
                System.err.println("cleanup: '" + ddl + "' failed (" + e.getMessage() + "), continuing");
            }
        }
    }
}
