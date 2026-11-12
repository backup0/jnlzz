package Misc;

import Vue.GlobalX;

import java.nio.file.Files;

public class GlobalStartCheck {

    public static String doStartCheck() {

        try {

            Files.createDirectories(GlobalX.bookBackupFolder);
            Files.createDirectories(GlobalX.fullTextSearchDBFolder);
            // now teh setting file -- m'kayz?
            if (!GlobalX.settingsFile.toFile().exists()) {
                Files.createFile(GlobalX.settingsFile);
            }
            return "";
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }

}
