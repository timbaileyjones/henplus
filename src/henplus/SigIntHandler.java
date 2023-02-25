/*
 * This is free software, licensed under the Gnu Public License (GPL) get a copy from <http://www.gnu.org/licenses/gpl.html> $Id:
 * SigIntHandler.java,v 1.10 2008-10-19 08:53:25 hzeller Exp $ author: Henner Zeller <H.Zeller@acm.org>
 * 
 * --- Note, this is not portable. If anyone knows a portable form that works accross different implementations of JVMs, please let
 * me know ---
 */
package henplus;

import java.util.ListIterator;
import java.util.Stack;

import org.jline.terminal.Terminal;

/**
 * Signal handler, that reacts on CTRL-C.
 */
public class SigIntHandler implements Terminal.SignalHandler, InterruptHandler {

    private static InterruptHandler dummyHandler = new InterruptHandler() {

        @Override
        public void popInterruptable() {
        }

        @Override
        public void pushInterruptable(final Interruptable t) {
        }

        @Override
        public void reset() {
        }
    };

    private boolean _once;
    private static SigIntHandler instance = null;
    private final Stack<Interruptable> _toInterruptStack;

    public static InterruptHandler getInstance() {
        return instance;
    }

    public static Terminal.SignalHandler getHandler() {
        return instance;
    }

    private SigIntHandler() {
        _once = false;
        _toInterruptStack = new Stack<>();
        instance = new SigIntHandler();
    }

    @Override
    public void pushInterruptable(final Interruptable t) {
        _toInterruptStack.push(t);
    }

    @Override
    public void popInterruptable() {
        _once = false;
        _toInterruptStack.pop();
    }

    @Override
    public void reset() {
        _once = false;
        _toInterruptStack.clear();
    }

    @Override
    public void handle(Terminal.Signal signal) {
        if (_once) {
            // got the interrupt more than once. May happen if you press
            // Ctrl-C multiple times .. or with broken thread lib on Linux.
            return;
        }

        _once = true;
        if (!_toInterruptStack.empty()) {
            final ListIterator<Interruptable> it = _toInterruptStack.listIterator(_toInterruptStack.size());
            while (it.hasPrevious()) {
                final Interruptable toInterrupt = it.previous();
                toInterrupt.interrupt();
            }
        } else {
            HenPlus.out().println("[Ctrl-C ; interrupted] - ignoring since there's nothing to interrupt");

            //System.exit(1);
        }
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
