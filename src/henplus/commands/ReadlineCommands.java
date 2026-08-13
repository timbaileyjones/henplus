/*
 * This is free software, licensed under the Gnu Public License (GPL) get a copy from <http://www.gnu.org/licenses/gpl.html> $Id:
 * EchoCommand.java,v 1.7 2004-01-28 09:25:48 hzeller Exp $ author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.AbstractCommand;
import henplus.HenPlus;
import henplus.SQLSession;
import org.gnu.readline.Readline;

/**
 * document me.
 */
public final class ReadlineCommands extends AbstractCommand {

    /**
     * returns the command-strings this command can handle.
     */
    @Override
    public String[] getCommandList() {
        return new String[] { "update-window-size" };
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    /**
     * execute the command given.
     */
    @Override
    public int execute(final SQLSession currentSession, final String cmd, final String param) {
        if (cmd.trim().equals("update-window-size")) {
            Readline.updateWindowSize();
        }
        return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "update-window-size";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd;
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc;
        dsc = "\tUpdate the \"readline\" window size, for line editing.\n";
        return dsc;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
