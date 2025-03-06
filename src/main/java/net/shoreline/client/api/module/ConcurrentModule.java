package net.shoreline.client.api.module;

import net.shoreline.client.OvaqReborn;

/**
 * {@link Module} implementation that runs concurrently and cannot be disabled
 * without the use of Commands. Unlike {@link ToggleModule},
 * {@link ConcurrentModule} does not have an enabled state or a keybinding.
 *
 * @author linus
 * @since 1.0
 */
public class ConcurrentModule extends Module {
    /**
     * @param name
     * @param desc
     * @param category
     */
    public ConcurrentModule(String name, String desc, ModuleCategory category) {
        super(name, desc, category);
        OvaqReborn.EVENT_HANDLER.subscribe(this);
    }
}
