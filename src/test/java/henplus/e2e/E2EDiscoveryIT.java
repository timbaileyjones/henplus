package henplus.e2e;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Exercises the same JDBC DatabaseMetaData calls henplus's own discovery commands are built on -
 * {@code tables}/{@code views} (ListUserObjectsCommand: {@code getTables(...)}) and {@code describe}'s index listing
 * (DescribeCommand: {@code getIndexInfo(...)}) - against whatever real databases are configured in
 * {@link E2EConfig#DEFAULT_PATH}, one dynamic test per target.
 *
 * This is read-only: it lists whatever schemas/tables/views/indexes already exist, asserting only that discovery itself
 * doesn't blow up - not any particular content, since what exists is whatever's actually in that database. Names of
 * everything found are printed to stdout so a run is actually informative to read, not just pass/fail; run without "-q"
 * (or check build/failsafe-reports/) to see them. Only runs via "mvn verify", same as {@link E2EConnectionsIT}.
 */
class E2EDiscoveryIT {

    private static final String[] TABLE_TYPES = { "TABLE" };
    private static final String[] VIEW_TYPES = { "VIEW" };

    @TestFactory
    List<DynamicTest> discoverExistingObjects() throws java.io.IOException {
        return E2EConfig.dynamicTestsPerTarget(target -> () -> {
            try (Connection connection = DriverManager.getConnection(target.url, target.username, target.password)) {
                final DatabaseMetaData meta = connection.getMetaData();
                final String prefix = "[" + target.name + "] ";

                final List<String> schemas = listSchemas(meta, prefix);
                System.out.println(prefix + "schemas: " + describe(schemas));

                final List<String[]> tables = listTablesOrViews(meta, TABLE_TYPES);
                System.out.println(prefix + "tables: " + describe(qualifiedNames(tables)));

                final List<String[]> views = listTablesOrViews(meta, VIEW_TYPES);
                System.out.println(prefix + "views: " + describe(qualifiedNames(views)));

                for (final String[] table : tables) {
                    final List<String> indexNames = listIndexNames(meta, table);
                    System.out.println(prefix + "indexes on " + qualifiedName(table) + ": " + describe(indexNames));
                }
            }
        });
    }

    /**
     * getSchemas() support genuinely varies by vendor/driver (e.g. SQLite has no real schema concept) - unsupported is
     * tolerated, but any other failure is a real problem worth failing the test over.
     */
    private static List<String> listSchemas(final DatabaseMetaData meta, final String logPrefix) throws SQLException {
        final List<String> names = new ArrayList<>();
        try (ResultSet rs = meta.getSchemas()) {
            while (rs.next()) {
                names.add(rs.getString("TABLE_SCHEM"));
            }
        } catch (final SQLFeatureNotSupportedException e) {
            System.out.println(logPrefix + "getSchemas() not supported by this driver, skipping");
        }
        return names;
    }

    private static List<String[]> listTablesOrViews(final DatabaseMetaData meta, final String[] types) throws SQLException {
        final List<String[]> found = new ArrayList<>();
        try (ResultSet rs = meta.getTables(null, null, null, types)) {
            while (rs.next()) {
                found.add(new String[] { rs.getString("TABLE_CAT"), rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME") });
            }
        }
        return found;
    }

    private static List<String> listIndexNames(final DatabaseMetaData meta, final String[] table) {
        final String catalog = table[0];
        final String schema = table[1];
        final String tableName = table[2];
        final Set<String> names = new LinkedHashSet<>();
        assertDoesNotThrow(() -> {
            try (ResultSet rs = meta.getIndexInfo(catalog, schema, tableName, false, true)) {
                while (rs.next()) {
                    final String indexName = rs.getString("INDEX_NAME");
                    if (indexName != null) {
                        names.add(indexName);
                    }
                }
            }
        }, "listing indexes for '" + tableName + "' should not throw");
        return new ArrayList<>(names);
    }

    private static List<String> qualifiedNames(final List<String[]> tables) {
        final List<String> names = new ArrayList<>();
        for (final String[] table : tables) {
            names.add(qualifiedName(table));
        }
        return names;
    }

    private static String qualifiedName(final String[] table) {
        final String schema = table[1];
        final String name = table[2];
        return schema == null ? name : schema + "." + name;
    }

    private static String describe(final List<String> names) {
        return names.isEmpty() ? "(none)" : String.join(", ", names);
    }
}
