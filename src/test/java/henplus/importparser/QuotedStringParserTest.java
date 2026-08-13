package henplus.importparser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QuotedStringParserTest {

    @Test
    void stripsMatchingSingleQuotes() throws Exception {
        final RecordingValueRecipient recipient = new RecordingValueRecipient();
        final char[] buffer = "'hello'".toCharArray();

        new QuotedStringParser(0).parse(buffer, 0, buffer.length, recipient);

        assertEquals("hello", recipient.stringValue(0));
    }

    @Test
    void stripsMatchingDoubleQuotes() throws Exception {
        final RecordingValueRecipient recipient = new RecordingValueRecipient();
        final char[] buffer = "\"hello\"".toCharArray();

        new QuotedStringParser(0).parse(buffer, 0, buffer.length, recipient);

        assertEquals("hello", recipient.stringValue(0));
    }

    @Test
    void leavesUnquotedValueAlone() throws Exception {
        final RecordingValueRecipient recipient = new RecordingValueRecipient();
        final char[] buffer = "hello".toCharArray();

        new QuotedStringParser(0).parse(buffer, 0, buffer.length, recipient);

        assertEquals("hello", recipient.stringValue(0));
    }

    @Test
    void leavesMismatchedQuotesAlone() throws Exception {
        final RecordingValueRecipient recipient = new RecordingValueRecipient();
        final char[] buffer = "'hello\"".toCharArray();

        new QuotedStringParser(0).parse(buffer, 0, buffer.length, recipient);

        assertEquals("'hello\"", recipient.stringValue(0));
    }

    @Test
    void treatsASingleQuoteCharacterAsUnquoted() throws Exception {
        final RecordingValueRecipient recipient = new RecordingValueRecipient();
        final char[] buffer = "'".toCharArray();

        new QuotedStringParser(0).parse(buffer, 0, buffer.length, recipient);

        assertEquals("'", recipient.stringValue(0));
    }
}
