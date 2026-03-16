package com.kalon;

import org.junit.jupiter.api.Test;

class KalonApplicationTest {

    @Test
    void contextLoadsWithoutSpring() {
        // Basic smoke test - ensures the class can be instantiated
        KalonApplication app = new KalonApplication();
        assert app != null;
    }
}
