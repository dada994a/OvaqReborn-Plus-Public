package net.shoreline.client.util.changelogs;

import java.util.ArrayList;
import java.util.List;

/**
 * @author OvaqRebornPlus
 * @since 1.0
 */
public class ChangeLog {
    private static final List<ChangeLogEntry> changeLogs = new ArrayList<>();

    static {
        changeLogs.add(new ChangeLogEntry(ChangeType.IMPROVE, "RPCにuidを表示するように変更"));
    }

    public static List<ChangeLogEntry> getChangeLogs() {
        return changeLogs;
    }
}
