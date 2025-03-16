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
        changeLogs.add(new ChangeLogEntry(ChangeType.ADD, "ちくわモーメント"));
        changeLogs.add(new ChangeLogEntry(ChangeType.IMPROVE, "ちくわモーメント"));
    }

    public static List<ChangeLogEntry> getChangeLogs() {
        return changeLogs;
    }
}