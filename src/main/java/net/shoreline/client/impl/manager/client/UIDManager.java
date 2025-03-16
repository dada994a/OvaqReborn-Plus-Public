package net.shoreline.client.impl.manager.client;

import net.shoreline.client.OvaqRebornPlus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author OvaqRebornPlus
 * @since 1.0
 */

public class UIDManager {
    public static String getUID() {
        String hwid = HwidManager.getHWID();
        if (hwid.equals("Error")) {
            return "-1";
        }
        try {
            URL url = new URL("https://pastebin.com/raw/PnxMiXTB");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(hwid)) {
                    String[] parts = line.split("-");
                    if (parts.length > 1) {
                        return parts[1];
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            OvaqRebornPlus.LOGGER.error("error: {}", e.getMessage());
        }

        return "-1";
    }
}
