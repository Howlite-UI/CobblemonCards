package com.howlite.cobblemoncards.loot;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Tracks the actual builder/table, not its ID: a fresh table on /reload must get its pool. */
public final class BoosterPoolInjection {
    private final Set<Object> injected = Collections.newSetFromMap(new WeakHashMap<>());

    public synchronized boolean injectOnce(Object target, Runnable injection) {
        if (injected.contains(target)) return false;
        injection.run();
        injected.add(target);
        return true;
    }
}
