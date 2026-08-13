package henplus.e2e;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Exercises the same JDBC DatabaseMetaData calls henplus's own discovery commands are built on -
 * {@code tables}/{@code views} (ListUserObjectsCommand: {@code getTables(...)}) and {@code describe}'s index listing
 * (DescribeCommand: {@code getIndexInfo(...)}) - against whatever real databases are configured in
 * {@link E2EConfig#DEFAULT_PATH}, one dynamic test per target.
 *
 * This is read-only: it lists whatever schemas/tables/views/indexes already exist, asserting only that discovery itself
 * doesn't blow up - not any particular content, since what exists is whatever's actually in that database. Only runs via
 * "mvn verify", same as {@link E2EConnectionsIT}.
 */
class E2EDiscoveryIT {

    private static final String[] TABLE_TYPES = { "TABLE" };
    private static final String[] VIEW_TYPES = { "VIEW" };

    @TestFactory
    List<DynamicTest> discoverExistingObjects() throws java.io.IOException {
        return E2EConfig.dynamicTestsPerTarget(target -> () -> {
            try (Connection connection = DriverManager.getConnection(target.url, target.username, target.password)) {
                final DatabaseMetaData meta = connection.getMetaData();

                final int schemaCount = countSchemas(meta, target.name);
                final List<String[]> tables = listTablesOrViews(meta, TABLE_TYPES);
                final List<String[]> views = listTablesOrViews(meta, VIEW_TYPES);

                int indexedTables = 0;
                for (final String[] table : tables) {
                    listIndexes(meta, table);
                    indexedTables++;
                }

                System.out.printf("[%s] discovered: %d schema(s), %d table(s), %d view(s), indexes checked on all %d table(s)%n",
                        target.name, schemaCount, tables.size(), views.size(), indexedTables);
            }
        });
    }

    /**
     * getSchemas() support genuinely varies by vendor/driver (e.g. SQLite has no real schema concept) - unsupported is
     * tolerated, but any other failure is a real problem worth failing the test over.
     */
    private static int countSchemas(final DatabaseMetaData meta, final String targetName) throws SQLException {
        try (ResultSet rs = meta.getSchemas()) {
            int count = 0;
            while (rs.next()) {
                count++;
            }
            return count;
        } catch (final SQLFeatureNotSupportedException e) {
            System.out.printf("[%s] getSchemas() not supported by this driver, skipping%n", targetName);
            return 0;
        }
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

    private static void listIndexes(final DatabaseMetaData meta, final String[] table) {
        final String catalog = table[0];
        final String schema = table[1];
        final String tableName = table[2];
        assertDoesNotThrow(() -> {
            try (ResultSet rs = meta.getIndexInfo(catalog, schema, tableName, false, true)) {
                while (rs.next()) {
                    // just draining the cursor - discovery working without error is the point, not any particular index.
                }
            }
        }, "listing indexes for '" + tableName + "' should not throw");
    }
}
