package henplus.importparser;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal ValueRecipient test double that just records what was set, keyed by field number.
 */
class RecordingValueRecipient implements ValueRecipient {

    private final Map<Integer, Object> values = new HashMap<>();

    @Override
    public void setLong(final int fieldNumber, final long value) {
        values.put(fieldNumber, value);
    }

    @Override
    public void setString(final int fieldNumber, final String value) {
        values.put(fieldNumber, value);
    }

    @Override
    public void setDate(final int fieldNumber, final Calendar cal) {
        values.put(fieldNumber, cal);
    }

    @Override
    public boolean finishRow() {
        return false;
    }

    String stringValue(final int fieldNumber) {
        return (String) values.get(fieldNumber);
    }

    Long longValue(final int fieldNumber) {
        return (Long) values.get(fieldNumber);
    }

    Calendar dateValue(final int fieldNumber) {
        return (Calendar) values.get(fieldNumber);
    }
}
