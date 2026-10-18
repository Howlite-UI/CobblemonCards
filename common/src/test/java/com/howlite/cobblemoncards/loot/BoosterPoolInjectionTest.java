package com.howlite.cobblemoncards.loot;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BoosterPoolInjectionTest {
    @Test
    void overlappingEligibilityAndRepeatedCallbacksInjectOnlyOnePoolIntoTheSameTarget() {
        BoosterPoolInjection guard = new BoosterPoolInjection();
        Object table = new Object();
        AtomicInteger pools = new AtomicInteger();
        assertTrue(guard.injectOnce(table, pools::incrementAndGet));
        assertFalse(guard.injectOnce(table, pools::incrementAndGet));
        assertFalse(guard.injectOnce(table, pools::incrementAndGet));
        assertEquals(1, pools.get());
    }

    @Test
    void freshTableAfterReloadReceivesItsPool() {
        BoosterPoolInjection guard = new BoosterPoolInjection();
        AtomicInteger pools = new AtomicInteger();
        assertTrue(guard.injectOnce(new Object(), pools::incrementAndGet));
        assertTrue(guard.injectOnce(new Object(), pools::incrementAndGet));
        assertEquals(2, pools.get());
    }

    @Test
    void unsuccessfulInjectionDoesNotSuppressARetry() {
        BoosterPoolInjection guard = new BoosterPoolInjection();
        Object table = new Object();
        assertThrows(IllegalStateException.class, () -> guard.injectOnce(table, () -> {
            throw new IllegalStateException("failed before adding the pool");
        }));
        AtomicInteger pools = new AtomicInteger();
        assertTrue(guard.injectOnce(table, pools::incrementAndGet));
        assertFalse(guard.injectOnce(table, pools::incrementAndGet));
        assertEquals(1, pools.get());
    }
}
