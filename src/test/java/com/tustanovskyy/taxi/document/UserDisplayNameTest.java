package com.tustanovskyy.taxi.document;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UserDisplayNameTest {

    @Test
    void joinsFirstAndLastName() {
        assertEquals("Ivan Petrenko", new User().setFirstName("Ivan").setLastName("Petrenko").displayName());
    }

    @Test
    void omitsMissingLastNameInsteadOfPrintingNull() {
        assertEquals("Ivan", new User().setFirstName("Ivan").displayName());
        assertEquals("Ivan", new User().setFirstName("Ivan").setLastName("  ").displayName());
    }

    @Test
    void trimsParts() {
        assertEquals("Ivan Petrenko", new User().setFirstName(" Ivan ").setLastName(" Petrenko").displayName());
    }

    @Test
    void emptyWhenNoNameAtAll() {
        assertEquals("", new User().displayName());
    }
}
