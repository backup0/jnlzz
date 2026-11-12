package Misc;

import Vue.GlobalX;
import Vue.Xc;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class Backup {

    public static void backup() {

        final String backupDbName = Xc.ploc().ftsDBName();
        if (backupDbName == null) {
            return;
        }

       final ArrayList<Integer> als = new ArrayList<>();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(GlobalX.bookBackupFolder)) {

            for (Path p : ds) {
                final String fn = p.getFileName().toString();
                if (fn.startsWith(backupDbName + "_")) {
                    final int suffix = getNumber(fn);
                    if (suffix == -1) {
                        continue;
                    }
                    als.add(suffix);
                }
            }
        }
        catch (Exception ignored) {}

        final Integer[] suffice = als.toArray(new Integer[0]);
        Arrays.sort(suffice);

        backup(backupDbName, suffice);
        pruneBackup(backupDbName, suffice);
    }

    private static void backup(String name, Integer[] sfx) {

        final int suffix = sfx.length == 0 ? 0 : sfx[sfx.length - 1] + 1;
        final String backupDbName = name + "_" + suffix + ".mv.db";
        final Path p = GlobalX.bookBackupFolder.resolve(backupDbName);

        try {
            Files.copy(Xc.ploc().getDbPath(), p);
        }
        catch (Exception ignored) {}
    }

    private static void pruneBackup(String name, Integer[] sfx) {

        if (sfx.length < 30) {
            return;
        }

        final String dbName = name + "_" + sfx[0] + ".mv.db";
        final Path pathToDel = GlobalX.bookBackupFolder.resolve(dbName);
        try {
            Files.deleteIfExists(pathToDel);
        }
        catch (Exception ignored) {}
    }

    private static int getNumber(String fn) {

        final int lstIndxOfmvDb = fn.lastIndexOf(".mv.db");
        if (lstIndxOfmvDb == -1) {
            return -1;
        }

        // remove mvdb
        final String nameNoExtension = fn.substring(0, lstIndxOfmvDb);

        final int i = nameNoExtension.lastIndexOf("_");
        if (i == -1) {
            return -1;
        }

        try {
            return Integer.parseInt(nameNoExtension.substring(i + 1));
        }
        catch (Exception e) {
            return -1;
        }
    }
}
