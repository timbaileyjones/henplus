package henplus;

import henplus.commands.DumpCommand;
import henplus.commands.ListUserObjectsCommand;
import henplus.commands.LoadCommand;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Test support: drives real henplus {@link Command} implementations (currently just {@link DumpCommand}'s dump-out/
 * dump-in) against a real JDBC connection, the same way the interactive shell does - without the interactive shell's
 * {@code CommandDispatcher}/{@code SetCommand} machinery, which would otherwise pull in {@code ~/.henplus} filesystem
 * access. Lives in package {@code henplus} (not {@code henplus.e2e}, where the test classes that use it live) so it can
 * call the package-private {@link HenPlus#forTesting(OutputDevice, OutputDevice)}.
 */
public final class HenPlusHarness implements AutoCloseable {

    private final SQLSession session;
    private final DumpCommand dumpCommand;

    public HenPlusHarness(final String url, final String user, final String password)
            throws IOException, ClassNotFoundException, SQLException {
        final HenPlus henplus = HenPlus.forTesting(new PrintStreamOutputDevice(System.out), new PrintStreamOutputDevice(System.err));
        session = new SQLSession(url, user, password);
        henplus.setCurrentSession(session);

        final ListUserObjectsCommand tableCompleter = new ListUserObjectsCommand(henplus);
        final LoadCommand loadCommand = new LoadCommand();
        dumpCommand = new DumpCommand(tableCompleter, loadCommand);
    }

    public SQLSession session() {
        return session;
    }

    /** Runs "command parameters" (e.g. run("dump-out", "/tmp/x.dump table;")) and returns the Command.SUCCESS/... code. */
    public int run(final String command, final String parameters) {
        return dumpCommand.execute(session, command, parameters);
    }

    @Override
    public void close() {
        session.close();
    }
}
