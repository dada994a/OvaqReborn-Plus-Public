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
        changeLogs.add(new ChangeLogEntry(ChangeType.ADD, "ircとかにwebhookを追加した"));
        changeLogs.add(new ChangeLogEntry((ChangeType.ADD),"PhaseにJPModeを追加中"));
        changeLogs.add(new ChangeLogEntry(ChangeType.IMPROVE, "座標流出moduleを修正"));
    }

    public static List<ChangeLogEntry> getChangeLogs() {
        return changeLogs;
    }
}