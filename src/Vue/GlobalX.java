package Vue;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Calendar;

public class GlobalX {
    // 23523 == jnlz23523; because ...
    // location of folder in app data
    // settings file.
    // settings -- that's what we should call it.
    // ft_search_index

    public static Scene scene = null;

    private static final String appDataFolderName = "jnlz23523";
    private static final String settingsFileName = "settings";
    private static final String fullTextSearchDBFolderName = "ftSearchDBs";
    private static final String bookBackupFolderName = "backups";
    // todo simplify -- x?
    private static final Path appDataFolder = Paths.get(System.getenv("LOCALAPPDATA"));
    public static final Path jnlzAppDataFolder = appDataFolder.resolve(appDataFolderName);

    public static final Path settingsFile = jnlzAppDataFolder.resolve(settingsFileName);
    public static final Path fullTextSearchDBFolder = jnlzAppDataFolder.resolve(fullTextSearchDBFolderName);
    public static final Path bookBackupFolder = jnlzAppDataFolder.resolve(bookBackupFolderName);

    public static final Path customDict = jnlzAppDataFolder.resolve("custom_dictionary");

    public static final String settings_notebookLoc = "note_book_location";
    public static final String settings_exportLoc = "export_location";

    public static Path booksLocation = null;
    public static Path exportLocation = null;

    public static final String localSettings_fts_db_name = "fts";

    private static int dotAt = -1;

    public static void implementSpecialStrokes(TextArea ta) {

        ta.setOnKeyPressed(keyEvent ->
        {
            if (keyEvent.getCode() == KeyCode.PERIOD)
            {
                dotAt = ta.getCaretPosition();
                return;
            }

            if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.D) {
                final String date = DateFormat.getDateInstance(DateFormat.SHORT).format(Calendar.getInstance().getTime());
                final String[] namedDay = DateAndTime.getNamedDay(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
                final String s = namedDay[1] + ", " + date + " ";
                ta.insertText(ta.getCaretPosition(), s);
                return;
            }

            if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.T) {
                final String s = DateFormat.getTimeInstance(DateFormat.SHORT).format(Calendar.getInstance().getTime());
                ta.insertText(ta.getCaretPosition(),s + " ");
                return;
            }

            if (keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.Q) {
                ta.insertText(ta.getCaretPosition(), "(??)");
            }

            if (dotAt > -1)
            {
                procThis(ta, keyEvent);
            }
        });

        ta.setOnKeyTyped(keyEvent -> {

            if (!pood.isEmpty())
            {
                ta.replaceText(ta.getCaretPosition() - 1, ta.getCaretPosition(), pood);
                pood ="";
            }
        });
    }

    private static void procThis(TextArea ta, KeyEvent gk)
    {
        if (ta.getCaretPosition() <= dotAt)
        {
            dotAt = -1;
            return;
        }

        // so,
        var s = ta.getText(dotAt, ta.getCaretPosition()).toCharArray();
        for (int i = 1; i < s.length; i++)
        {
            if (!Character.isSpaceChar(s[i]))
            {
                dotAt = -1;
                return;
            }
        }

        var k = gk.getCode();
        if (Character.isAlphabetic(k.getCode()))
        {
            pood = Character.toString(Character.toUpperCase(k.getCode()));
            dotAt = -1;
        }
    }

    private static String pood = "";

    public static void searchy(TextArea ta, String key) {

        final int startIndex = ta.getCaretPosition();
        final String txt = ta.getText().toLowerCase();
        final int indxNextOccurence = txt.indexOf(key.toLowerCase(), startIndex);

        final int loc;
        if (indxNextOccurence == -1) {
            // can't find? Try the start again.
            loc = (txt.toLowerCase()).indexOf(key.toLowerCase());
        }
        else {
            loc = indxNextOccurence;
        }

        int kle = key.length();
        if (loc == -1) {
            ta.selectRange(0, 0);
        } else {
            ta.selectRange(loc, loc + kle);
        }
    }
}
