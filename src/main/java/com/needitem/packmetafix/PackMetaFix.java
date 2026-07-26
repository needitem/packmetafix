package com.needitem.packmetafix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared state for the pack metadata repair pass.
 *
 * <p>The repair runs inside a mixin on {@code ResourceMetadata.fromJsonStream}, which re-invokes the
 * very method it is injected into. The re-entrancy flag lets the second (inner) call fall straight
 * through to vanilla instead of looping.
 */
public final class PackMetaFix {

    public static final Logger LOGGER = LoggerFactory.getLogger("PackMetaFix");

    private static final ThreadLocal<Boolean> REENTRANT = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private PackMetaFix() {
    }

    public static boolean isReentrant() {
        return REENTRANT.get();
    }

    public static void enterReentrant() {
        REENTRANT.set(Boolean.TRUE);
    }

    public static void exitReentrant() {
        REENTRANT.remove();
    }
}
