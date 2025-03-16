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
        changeLogs.add(new ChangeLogEntry(ChangeType.ADD, "UpdateCheckerを追加"));
        changeLogs.add(new ChangeLogEntry(ChangeType.IMPROVE, "IRCに[VIP]などの表示を追加"));
    }

    public static List<ChangeLogEntry> getChangeLogs() {
        return changeLogs;
    }
}