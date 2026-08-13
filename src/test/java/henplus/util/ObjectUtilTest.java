package henplus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ObjectUtilTest {

    @Test
    void nullSafeHashCodeReturnsZeroForNull() {
        assertEquals(0, ObjectUtil.nullSafeHashCode(null));
    }

    @Test
    void nullSafeHashCodeDelegatesToHashCode() {
        assertEquals("foo".hashCode(), ObjectUtil.nullSafeHashCode("foo"));
    }

    @Test
    void nullSafeEqualsTreatsBothNullAsEqual() {
        assertTrue(ObjectUtil.nullSafeEquals(null, null));
    }

    @Test
    void nullSafeEqualsTreatsOneNullAsUnequal() {
        assertFalse(ObjectUtil.nullSafeEquals(null, "foo"));
        assertFalse(ObjectUtil.nullSafeEquals("foo", null));
    }

    @Test
    void nullSafeEqualsDelegatesToEquals() {
        assertTrue(ObjectUtil.nullSafeEquals("foo", "foo"));
        assertFalse(ObjectUtil.nullSafeEquals("foo", "bar"));
    }
}
