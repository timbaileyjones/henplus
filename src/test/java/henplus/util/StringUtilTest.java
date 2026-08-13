package henplus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringUtilTest {

    @Test
    void nullSafeEqualsTreatsBothNullAsEqual() {
        assertTrue(StringUtil.nullSafeEquals(null, null));
    }

    @Test
    void nullSafeEqualsTreatsOneNullAsUnequal() {
        assertFalse(StringUtil.nullSafeEquals(null, "foo"));
        assertFalse(StringUtil.nullSafeEquals("foo", null));
    }

    @Test
    void nullSafeEqualsComparesValues() {
        assertTrue(StringUtil.nullSafeEquals("foo", "foo"));
        assertFalse(StringUtil.nullSafeEquals("foo", "bar"));
    }

    @Test
    void nullSafeEqualsIgnoreCaseIsCaseInsensitive() {
        assertTrue(StringUtil.nullSafeEquals("FOO", "foo", true));
        assertFalse(StringUtil.nullSafeEquals("FOO", "foo", false));
    }

    @Test
    void nullSafeEqualsIgnoreCaseStillTreatsBothNullAsEqual() {
        assertTrue(StringUtil.nullSafeEquals(null, null, true));
        assertFalse(StringUtil.nullSafeEquals(null, "foo", true));
    }

    @Test
    void isEmptyIsTrueForNullAndEmptyString() {
        assertTrue(StringUtil.isEmpty(null));
        assertTrue(StringUtil.isEmpty(""));
    }

    @Test
    void isEmptyIsFalseForNonEmptyString() {
        assertFalse(StringUtil.isEmpty(" "));
        assertFalse(StringUtil.isEmpty("foo"));
    }
}
