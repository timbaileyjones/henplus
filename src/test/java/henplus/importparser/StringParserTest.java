package henplus.importparser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringParserTest {

    @Test
    void parsesTheGivenRangeOfTheBuffer() throws Exception {
        final RecordingValueRecipient recipient = new RecordingValueRecipient();
        final char[] buffer = "xxhelloxx".toCharArray();

        new StringParser(3).parse(buffer, 2, 5, recipient);

        assertEquals("hello", recipient.stringValue(3));
    }

    @Test
    void parsesAnEmptyRangeAsEmptyString() throws Exception {
        final RecordingValueRecipient recipient = new RecordingValueRecipient();
        final char[] buffer = "hello".toCharArray();

        new StringParser(0).parse(buffer, 2, 0, recipient);

        assertEquals("", recipient.stringValue(0));
    }
}
