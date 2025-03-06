package net.shoreline.client.util.changelogs;

import java.util.ArrayList;
import java.util.List;

/**
 * @author OvaqReborn
 * @since 1.0
 */
public class ChangeLog {
    private static final List<ChangeLogEntry> changeLogs = new ArrayList<>();

    static {
        changeLogs.add(new ChangeLogEntry(ChangeType.ADD, "色々"));
        changeLogs.add(new ChangeLogEntry(ChangeType.IMPROVE, "色々"));
    }

    public static List<ChangeLogEntry> getChangeLogs() {
        return changeLogs;
    }
}