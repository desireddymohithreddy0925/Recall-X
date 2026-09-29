package com.recallx.recallx.guard;

import com.recallx.recallx.store.ConfigChange;

/**
 * The result for one changed key. status is one of:
 * <ul>
 *   <li>WARNING: a warning card (warning is set)</li>
 *   <li>PREVIOUSLY_CLEARED: the same change was judged safe before (cleared is set)</li>
 *   <li>NO_HISTORY: nothing relevant in memory; RECALL-X stays quiet</li>
 *   <li>MEMORY_UNAVAILABLE: Hindsight failed, so "no history" can't be claimed</li>
 * </ul>
 */
public record KeyResult(String key, String oldValue, String newValue, String status, WarningView warning,
                        ClearedView cleared) {

    static KeyResult noHistory(ConfigChange c) {
        return new KeyResult(c.keyName(), c.oldValue(), c.newValue(), "NO_HISTORY", null, null);
    }

    static KeyResult memoryUnavailable(ConfigChange c) {
        return new KeyResult(c.keyName(), c.oldValue(), c.newValue(), "MEMORY_UNAVAILABLE", null, null);
    }

    static KeyResult warning(ConfigChange c, WarningView warning) {
        return new KeyResult(c.keyName(), c.oldValue(), c.newValue(), "WARNING", warning, null);
    }

    static KeyResult cleared(ConfigChange c, ClearedView cleared) {
        return new KeyResult(c.keyName(), c.oldValue(), c.newValue(), "PREVIOUSLY_CLEARED", null, cleared);
    }
}
