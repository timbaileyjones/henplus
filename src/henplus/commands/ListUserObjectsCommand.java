/*
 * This is free software, licensed under the Gnu Public License (GPL) get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.AbstractCommand;
import henplus.HenPlus;
import henplus.Interruptable;
import henplus.SQLSession;
import henplus.SigIntHandler;
import henplus.view.Column;
import henplus.view.util.NameCompleter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * FIXME: use SQLMetaData stuff instead.
 */
public class ListUserObjectsCommand extends AbstractCommand implements Interruptable {

    private static final String[] LIST_TABLES_VIEWS = {"TABLE", "VIEW"};
    private static final String[] LIST_TABLES = {"TABLE"};
    private static final String[] LIST_VIEWS = {"VIEW"};
    private static final int[] TABLE_DISP_COLS = {2, 3, 4, 5};
    private static final int[] PROC_DISP_COLS = {2, 3, 8};

    /**
     * all tables in one session.
     */
    private final Map<SQLSession, NameCompleter> _sessionTables;
    private final Map<SQLSession, NameCompleter> _sessionColumns;
    private final HenPlus _henplus;

    private boolean _interrupted;

    public ListUserObjectsCommand(final HenPlus hp) {
        _sessionTables = new HashMap<SQLSession, NameCompleter>();
        _sessionColumns = new HashMap<SQLSession, NameCompleter>();
        _henplus = hp;
        _interrupted = false;
    }

    /**
     * returns the command-strings this command can handle.
     */
    @Override
    public String[] getCommandList() {
        return new String[]{"tables", "views", "procedures", "rehash"};
    }

    /**
     * execute the command given.
     */
    @Override
    public int execute(final SQLSession session, final String cmd, final String param) {
        if (cmd.equals("rehash")) {
            rehash(session);
        } else {
            try {
                final Connection conn = session.getConnection(); // use createStmt
                final DatabaseMetaData meta = conn.getMetaData();
                final String catalog = conn.getCatalog();
                /*
                 * HenPlus.msg().println("catalog: " + catalog);
                 * ResultSetRenderer catalogrenderer = new
                 * ResultSetRenderer(meta.getSchemas(), "|", true, true, 2000,
                 * HenPlus.out()); catalogrenderer.execute();
                 */
                ResultSetRenderer renderer;
                ResultSet rset;
                String objectType;
                int[] columnDef;
                if ("procedures".equals(cmd)) {
                    objectType = "Procecdures";
                    HenPlus.msg().println(objectType);
                    rset = meta.getProcedures(catalog, null, null);
                    columnDef = PROC_DISP_COLS;
                } else {
                    final boolean showViews = "views".equals(cmd);
                    objectType = showViews ? "Views" : "Tables";
                    HenPlus.msg().println(objectType);
                    rset = meta.getTables(catalog, null, null, showViews ? LIST_VIEWS : LIST_TABLES);
                    columnDef = TABLE_DISP_COLS;
                }

                renderer = new ResultSetRenderer(rset, "|", true, true, false, 10000, HenPlus.out(), columnDef);
                renderer.getDisplayMetaData()[2].setAutoWrap(78);

                if (param != null && !param.trim().isEmpty()) {
                    MultiGlobFilter multiFilter = new MultiGlobFilter();
                    // foo.bar  = len 2
                    // .bar = len 2
                    // foo. = len 2
                    String[] parts = param.trim().split("\\.", 2);
                    if (parts.length == 2) {
                        // we have a <schema>.<table> glob, either part could be empty
                        if (!parts[0].isEmpty()) {
                            multiFilter.addFilter(new GlobFilter(0, parts[0]));
                        }
                        if (!parts[1].isEmpty()) {
                            multiFilter.addFilter(new GlobFilter(1, parts[1]));
                        }
                    } else {
                        // only have a <table> glob
                        multiFilter.addFilter(new GlobFilter(1, parts[0]));
                    }

                    renderer.setFilter(multiFilter);
                }

                final int tables = renderer.execute();
                if (tables > 0) {
                    HenPlus.msg().println(tables + " " + objectType + " found.");
                    if (renderer.limitReached()) {
                        HenPlus.msg().println("..and probably more; reached display limit");
                    }
                }
            } catch (final Exception e) {
                HenPlus.msg().println(e.getMessage());
                return EXEC_FAILED;
            }
        }
        return SUCCESS;
    }

    private NameCompleter getTableCompleter(final SQLSession session) {
        final NameCompleter compl = _sessionTables.get(session);
        return compl == null ? rehash(session) : compl;
    }

    private NameCompleter getAllColumnsCompleter(final SQLSession session) {
        NameCompleter compl = _sessionColumns.get(session);
        if (compl != null) {
            return compl;
        }
        /*
         * This may be a lengthy process..
         */
        _interrupted = false;
        SigIntHandler.getInstance().pushInterruptable(this);
        final NameCompleter tables = getTableCompleter(session);
        if (tables == null) {
            return null;
        }
        final Iterator<String> table = tables.getAllNamesIterator();
        compl = new NameCompleter();
        while (!_interrupted && table.hasNext()) {
            final String tabName = table.next();
            final Collection<String> columns = columnsFor(tabName);
            final Iterator<String> cit = columns.iterator();
            while (cit.hasNext()) {
                final String col = cit.next();
                compl.addName(col);
            }
        }
        if (_interrupted) {
            compl = null;
        } else {
            _sessionColumns.put(session, compl);
        }
        SigIntHandler.getInstance().popInterruptable();
        return compl;
    }

    public void unhash(final SQLSession session) {
        _sessionTables.remove(session);
    }

    /**
     * rehash table names.
     */
    private NameCompleter rehash(final SQLSession session) {
        final NameCompleter result = new NameCompleter();
        final Connection conn = session.getConnection(); // use createStmt
        ResultSet rset = null;
        try {
            final DatabaseMetaData meta = conn.getMetaData();
            rset = meta.getTables(null, null, null, LIST_TABLES_VIEWS);
            while (rset.next()) {
                result.addName(rset.getString(3));
            }
        } catch (final Exception e) {
            // ignore.
        } finally {
            if (rset != null) {
                try {
                    rset.close();
                } catch (final Exception e) {
                }
            }
        }
        _sessionTables.put(session, result);
        _sessionColumns.remove(session);
        return result;
    }

    /**
     * fixme: add this to the cached values determined by rehash.
     */
    public Collection<String> columnsFor(String tabName) {
        final SQLSession session = _henplus.getCurrentSession();
        final Set<String> result = new HashSet<String>();
        final Connection conn = session.getConnection(); // use createStmt
        ResultSet rset = null;

        String schema = null;
        final int schemaDelim = tabName.indexOf('.');
        if (schemaDelim > 0) {
            schema = tabName.substring(0, schemaDelim);
            tabName = tabName.substring(schemaDelim + 1);
        }
        try {
            final DatabaseMetaData meta = conn.getMetaData();
            rset = meta.getColumns(conn.getCatalog(), schema, tabName, null);
            while (rset.next()) {
                result.add(rset.getString(4));
            }
        } catch (final Exception e) {
            // ignore.
        } finally {
            if (rset != null) {
                try {
                    rset.close();
                } catch (final Exception e) {
                }
            }
        }
        return result;
    }

    /**
     * see, if we find exactly one alternative, that is spelled correctly. If we have more than one alternative but one, that has
     * the same length of the requested tablename, return this.
     */
    public String correctTableName(final String tabName) {
        final Iterator<String> it = completeTableName(HenPlus.getInstance().getCurrentSession(), tabName);
        if (it == null) {
            return null;
        }
        boolean foundSameLengthMatch = false;
        int count = 0;
        String correctedName = null;
        if (it.hasNext()) {
            final String alternative = it.next();
            final boolean sameLength = alternative != null && alternative.length() == tabName.length();

            foundSameLengthMatch |= sameLength;
            ++count;
            if (sameLength) {
                correctedName = alternative;
            }
        }
        return count == 1 || foundSameLengthMatch ? correctedName : null;
    }

    /**
     * used from diverse commands that need table name completion.
     */
    public Iterator<String> completeTableName(final SQLSession session, final String partialTable) {
        if (session == null) {
            return null;
        }
        final NameCompleter completer = getTableCompleter(session);
        return completer.getAlternatives(partialTable);
    }

    public Iterator<String> completeAllColumns(final String partialColumn) {
        final SQLSession session = _henplus.getCurrentSession();
        if (session == null) {
            return null;
        }
        final NameCompleter completer = getAllColumnsCompleter(session);
        return completer.getAlternatives(partialColumn);
    }

    public Iterator<String> getTableNamesIteratorForSession(final SQLSession session) {
        return getTableCompleter(session).getAllNamesIterator();
    }

    public SortedSet<String> getTableNamesForSession(final SQLSession session) {
        return getTableCompleter(session).getAllNames();
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "list available user objects";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd;
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc;
        if (cmd.equals("rehash")) {
            dsc = "\trebuild the internal hash for tablename completion.";
        } else {
            dsc = "\tLists all " + cmd + " available in this schema.";
        }
        return dsc;
    }

    @Override
    public void interrupt() {
        _interrupted = true;
    }

    public static class GlobFilter implements ResultSetRenderer.ResultSetFilter {

        private final String glob;
        private final int col;
        private final String regex;

        public GlobFilter(int col, String glob) {
            this.col = col;
            this.glob = glob;
            this.regex = convertGlobToRegEx(this.glob);
        }

        @Override
        public boolean accept(String[] row) {
            return row[col].matches(regex);
        }

        private String convertGlobToRegEx(String line) {
            line = line.trim();
            int strLen = line.length();
            StringBuilder sb = new StringBuilder(strLen);
            // Remove beginning and ending * globs because they're useless
//            if (line.startsWith("*")) {
//                line = line.substring(1);
//                strLen--;
//            }
//            if (line.endsWith("*")) {
//                line = line.substring(0, strLen - 1);
//                strLen--;
//            }
            boolean escaping = false;
            int inCurlies = 0;
            for (char currentChar : line.toCharArray()) {
                switch (currentChar) {
                    case '*':
                        if (escaping)
                            sb.append("\\*");
                        else
                            sb.append(".*");
                        escaping = false;
                        break;
                    case '?':
                        if (escaping)
                            sb.append("\\?");
                        else
                            sb.append('.');
                        escaping = false;
                        break;
                    case '.':
                    case '(':
                    case ')':
                    case '+':
                    case '|':
                    case '^':
                    case '$':
                    case '@':
                    case '%':
                        sb.append('\\');
                        sb.append(currentChar);
                        escaping = false;
                        break;
                    case '\\':
                        if (escaping) {
                            sb.append("\\\\");
                            escaping = false;
                        } else
                            escaping = true;
                        break;
                    case '{':
                        if (escaping) {
                            sb.append("\\{");
                        } else {
                            sb.append('(');
                            inCurlies++;
                        }
                        escaping = false;
                        break;
                    case '}':
                        if (inCurlies > 0 && !escaping) {
                            sb.append(')');
                            inCurlies--;
                        } else if (escaping)
                            sb.append("\\}");
                        else
                            sb.append("}");
                        escaping = false;
                        break;
                    case ',':
                        if (inCurlies > 0 && !escaping) {
                            sb.append('|');
                        } else if (escaping)
                            sb.append("\\,");
                        else
                            sb.append(",");
                        break;
                    default:
                        escaping = false;
                        sb.append(currentChar);
                }
            }
            return sb.toString();
        }
    }

    public static class MultiGlobFilter implements ResultSetRenderer.ResultSetFilter {

        private List<GlobFilter> filters = new ArrayList<GlobFilter>();

        public void addFilter(GlobFilter filter) {
            filters.add(filter);
        }

        @Override
        public boolean accept(String[] row) {
            // if any of the filters return false, return false
            for (GlobFilter filter : filters) {
                if (!filter.accept(row)) {
                    return false;
                }
            }
            return true;
        }
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
