package henplus.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BooleanPropertyHolderTest {

    private static final class RecordingBooleanProperty extends BooleanPropertyHolder {
        private boolean lastValue;

        @Override
        public void booleanPropertyChanged(final boolean newValue) {
            lastValue = newValue;
        }

        @Override
        public String getDefaultValue() {
            return "on";
        }
    }

    @Test
    void defaultsToTheConstructorProvidedValue() {
        final RecordingBooleanProperty property = new RecordingBooleanProperty();
        assertFalse(property.lastValue);
        assertEquals(null, property.getValue());
    }

    @Test
    void acceptsAllTheDocumentedTrueValues() throws Exception {
        for (final String v : new String[] { "1", "on", "true" }) {
            final RecordingBooleanProperty property = new RecordingBooleanProperty();
            property.setValue(v);
            assertTrue(property.lastValue, v + " should mean true");
        }
    }

    @Test
    void acceptsAllTheDocumentedFalseValues() throws Exception {
        for (final String v : new String[] { "0", "off", "false" }) {
            final RecordingBooleanProperty property = new RecordingBooleanProperty();
            property.setValue(v);
            assertFalse(property.lastValue, v + " should mean false");
        }
    }

    @Test
    void canonicalizesAnUnambiguousPrefixToItsFullValue() throws Exception {
        final RecordingBooleanProperty property = new RecordingBooleanProperty();
        property.setValue("of");
        assertEquals("off", property.getValue());
        assertFalse(property.lastValue);
    }

    @Test
    void rejectsValuesOutsideTheEnumeration() {
        final RecordingBooleanProperty property = new RecordingBooleanProperty();
        assertThrows(Exception.class, () -> property.setValue("maybe"));
    }
}
