package henplus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SQLStatementSeparatorTest {

    @Test
    void splitsASemicolonTerminatedStatement() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("select * from foo;");

        assertTrue(sep.hasNext());
        assertEquals("select * from foo;", sep.next());
        sep.consumed();
        assertFalse(sep.hasNext());
    }

    @Test
    void splitsMultipleSemicolonTerminatedStatements() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("select 1;select 2;");

        assertTrue(sep.hasNext());
        assertEquals("select 1;", sep.next());
        sep.consumed();

        assertTrue(sep.hasNext());
        assertEquals("select 2;", sep.next());
        sep.consumed();

        assertFalse(sep.hasNext());
    }

    @Test
    void treatsANewlineAsAStatementEndWhenNoSemicolonSeen() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("echo hello\n");

        assertTrue(sep.hasNext());
        final String stmt = sep.next();
        assertTrue(stmt.startsWith("echo hello"), stmt);
        sep.consumed();
    }

    @Test
    void stripsCStyleComments() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("select /* comment */ 1;");

        assertTrue(sep.hasNext());
        assertEquals("select  1;", sep.next());
        sep.consumed();
    }

    @Test
    void stripsAnsiDashDashComments() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("select 1; -- trailing comment\n");

        assertTrue(sep.hasNext());
        assertEquals("select 1;", sep.next());
        sep.consumed();
    }

    @Test
    void canBeToldNotToRemoveComments() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.removeComments(false);
        sep.append("select /* keep me */ 1;");

        assertTrue(sep.hasNext());
        assertEquals("select /* keep me */ 1;", sep.next());
        sep.consumed();
    }

    @Test
    void doesNotTreatASemicolonInsideAQuotedStringAsAnEnd() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("select ';' from foo;");

        assertTrue(sep.hasNext());
        assertEquals("select ';' from foo;", sep.next());
        sep.consumed();
    }

    @Test
    void doesNotTreatASemicolonInsideADoubleQuotedStringAsAnEnd() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("select \";\" from foo;");

        assertTrue(sep.hasNext());
        assertEquals("select \";\" from foo;", sep.next());
        sep.consumed();
    }

    @Test
    void hasNextReturnsFalseOnEmptyInput() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        assertFalse(sep.hasNext());
    }

    @Test
    void contKeepsAccumulatingUntilConsumed() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("select 1\n");
        assertTrue(sep.hasNext());
        sep.next();
        sep.cont();

        sep.append("+ 2;");
        assertTrue(sep.hasNext());
        final String stmt = sep.next();
        assertTrue(stmt.contains("select 1"), stmt);
        assertTrue(stmt.endsWith("+ 2;"), stmt);
        sep.consumed();
    }

    @Test
    void pushAndPopIsolateNestedParsingState() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("select 1\n");
        assertTrue(sep.hasNext());
        sep.next();
        sep.cont();

        sep.push();
        sep.append("select 2;");
        assertTrue(sep.hasNext());
        assertEquals("select 2;", sep.next());
        sep.consumed();
        sep.pop();

        sep.append("+ 3;");
        assertTrue(sep.hasNext());
        final String stmt = sep.next();
        assertTrue(stmt.contains("select 1"), stmt);
        assertTrue(stmt.endsWith("+ 3;"), stmt);
        sep.consumed();
    }

    @Test
    void discardClearsAnyBufferedInput() {
        final SQLStatementSeparator sep = new SQLStatementSeparator();
        sep.append("select 1");
        sep.discard();
        assertFalse(sep.hasNext());
    }
}
