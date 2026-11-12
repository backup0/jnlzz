package Misc;

import Vue.GlobalX;
import Vue.LooseMeta;
import Vue.Xc;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;

public class ExImBackend {

    private static final HashMap<String, Charset> csMap = new HashMap<>();
    private static final ArrayList<String> charsets = new ArrayList<>();

    public static ArrayList<String> getCsMap() {

        if (!charsets.isEmpty()) {
            return charsets;
        }

        charsets.add("UTF-8");
        charsets.add("Cp1252");
        charsets.add("US-ASCII");

        return charsets;
    }

    public static void importDaily(Path loc, String charset) {

        if (loc.toFile().isFile()) {
            final int id = getDateInFileName(loc.getFileName());
            storeDayEntry(id, loc, charset);
            return;
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(loc)) {

            for (Path p : ds) {
                final int id = getDateInFileName(p.getFileName());
                if (id == 0) {
                    continue;
                }
                storeDayEntry(id, p, charset);
            }
        }
        catch (Exception ex) {
            showImportFailDialog(ex.getMessage());
        }
    }

    private static void storeDayEntry(int id, Path p, String charset) {

        try {

            final String s = new String(Files.readAllBytes(p), charset);
            Xc.ploc().createDayEntry(id);
            Xc.ploc().storeDailyText(id, s);
        }
        catch (Exception ignored) {}
    }


    private static int getDateInFileName(Path p) {
        // todo fix this in v124.
        // wrong date format like dd-mm-yyyy, or mm-dd-yyyy will also be valid here, so need to check.
        final String fName = p.toString();

        final StringBuilder sbs = new StringBuilder();
        for (char c : fName.toCharArray()) {

            if (c == '-') {
                continue;
            }

            if (Character.isDigit(c)) {
                sbs.append(c);
            }
            else {
                sbs.append("a");
            }
        }

        final String[] stringFrags = sbs.toString().split("a");

        String result = "";
        for (String s : stringFrags) {
            if (s.length() == 8) {
                result = s;
                break;
            }
        }

        if ("".equalsIgnoreCase(result)) {
            return 0;
        }

        try {
            return Integer.parseInt(result);
        }
        catch (Exception e) {
            return 0;
        }
    }

    public static void importLoose(Path loc, String charset) {

        if (loc.toFile().isFile()) {
            storeLooseEntry(loc, charset);
            return;
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(loc)) {

            for (Path p : ds) {
                storeLooseEntry(p, charset);
            }
        }
        catch (Exception ex) {
            showImportFailDialog(ex.getMessage());
        }
    }

    private static void storeLooseEntry(Path p, String charset) {

        try {

            final String s = new String(Files.readAllBytes(p), charset);
            final LooseMeta lms = Xc.ploc().createLoose(removeExtension(p));
            Xc.ploc().storeLooseText(lms.id,s);
        }
        catch (Exception ignored) {}
    }

    private static String removeExtension(Path p) {

        final String fileName = p.getFileName().toString();
        if (!fileName.contains(".")) {
            return fileName;
        }

        try {
            final int lastIndexOfDot = fileName.lastIndexOf(".");
            final String fnSansExt = fileName.substring(0, lastIndexOfDot);

            if (fnSansExt.length() == 0) {
                return fileName;
            } else {
                return fnSansExt;
            }
        }
        catch (Exception e) {
            return fileName;
        }
    }

    private static void showImportFailDialog(String msg) {

        Platform.runLater(() -> {
            final Alert alt = new Alert(Alert.AlertType.ERROR);
            alt.setHeaderText("Import failed");
            alt.setContentText(msg);
            alt.showAndWait();
        });
    }

    public static void export(String fileName, String text) {

        if (GlobalX.exportLocation == null) {
            final File f = chooseFolder();
            if (f == null) {
                return;
            }
            GlobalX.exportLocation = f.toPath();
        }

        if (!GlobalX.exportLocation.toFile().exists()) {
            final File f = chooseFolder();
            if (f == null) {
                return;
            }
            GlobalX.exportLocation = f.toPath();
        }

        if (GlobalX.exportLocation.toFile().isFile()) {
            final File f = chooseFolder();
            if (f == null) {
                return;
            }
            GlobalX.exportLocation = f.toPath();
        }

        try {
            final Path p = GlobalX.exportLocation.resolve(fileName);
            if (Files.exists(p))
            {
                final Alert alt = new Alert(Alert.AlertType.CONFIRMATION);
                alt.setTitle("(!)");
                alt.setHeaderText("File already exists. \nOverwrite?");
                final var o = alt.showAndWait();

                if (o.isPresent() && o.get() == ButtonType.CANCEL)
                {
                    return;
                }
            }

            Files.write(p, text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (Exception e) {

            final Alert alt = new Alert(Alert.AlertType.ERROR);
            alt.setHeaderText("Export error");
            alt.setContentText(e.getMessage());
            alt.showAndWait();
        }
    }

    private static File chooseFolder() {

        final DirectoryChooser dcf = new DirectoryChooser();
        dcf.setTitle("Export location");
        return dcf.showDialog(GlobalX.scene.getWindow());
    }
}
