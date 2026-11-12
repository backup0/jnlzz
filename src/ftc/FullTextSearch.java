package ftc;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class FullTextSearch {

    private final String splitterRegex = "[^\\p{Alnum}]";
    private final ArrayList<String> errorLog = new ArrayList<>();
    private final Connection connection;

    public FullTextSearch(Connection conn) {
        connection = conn;
    }

    public boolean update(String docRef, String text) {

        try {
            // unique words from the text
            final ArrayList<String> uniqueWords = getUniqueWordsFromText(text);
            // the ids - after all have been committed.
            final ArrayList<Long> wordIds = updateWordList(uniqueWords);
            Long thisDocId = getDocId(docRef);

            if (thisDocId == null) {
                // new doc.
                thisDocId = addDoc(docRef);
            }
            else {
                // remove words no longer inside the text from the map table
                // this doc is already inside the db, it's updated.
                deleteOldMapping(thisDocId);
            }
            storeAssociation(thisDocId, wordIds);
            return true;
        }
        catch (Exception e) {
            errorLog.add(e.getMessage());
            return false;
        }
    }

    private ArrayList<String> getUniqueWordsFromText(String text) {

        final String[] words = text.split(splitterRegex);
        final HashSet<String> uniqueWordList = new HashSet<>();

        for (String s : words) {

            if (s.length() >= 1) {
                try {
                    uniqueWordList.add(s.toUpperCase());
                }
                catch (Exception ignored) {}
            }
        }

        return new ArrayList<>(uniqueWordList);
    }

    private Long getDocId(String docref) throws SQLException {

        final String q = "select id from docref where doc = ? limit 1";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setString(1, docref);

            try (final ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                else {
                    return null;
                }
            }
        }
    }

    private Long addDoc(String docref) throws SQLException {

        final String q = "insert into docref(doc) values(?)";
        try (final PreparedStatement ps = connection.prepareStatement(q,  Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, docref);
            ps.execute();

            try (final ResultSet rs =  ps.getGeneratedKeys()) {

                if (rs.next()) {
                    return rs.getLong(1);
                }
                else {
                    throw new SQLException("Can't get generated id.");
                }
            }
        }
    }

    private void storeAssociation(long docId, ArrayList<Long> wordIds) throws SQLException {

        final String q = "insert into map(word_id, doc_id) values (?, ?)";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {

            for (Long wid : wordIds) {
                ps.setLong(1, wid);
                ps.setLong(2, docId);
                ps.execute();
            }
        }
    }

    private void deleteOldMapping(long docId) throws SQLException {

        final String q = "delete from map where doc_id = ?";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setLong(1, docId);
            ps.execute();
        }
    }

    private ArrayList<Long> updateWordList(List<String> words) throws SQLException {

        final ArrayList<Long> ids = new ArrayList<>();
        final ArrayList<String> wordsNotStoredYet = new ArrayList<>();

        final String q = "select id from words where name = ? limit 1";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {

            for (String s : words) {

                ps.setString(1, s);
                try (final ResultSet rs = ps.executeQuery()) {

                    if (rs.first()) {
                        ids.add(rs.getLong(1));
                    }
                    else {
                        // store words we have not stored in the db.
                        wordsNotStoredYet.add(s);
                    }
                }
            }
            addNewWords(wordsNotStoredYet, ids);
        }
        return ids;
    }

    private void addNewWords(List<String> words, ArrayList<Long> ids) throws SQLException {

        final String q = "insert into words(name) values(?)";
        try (final PreparedStatement ps = connection.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {

            while (!words.isEmpty()) {

                final String s = words.remove(words.size()-1);
                ps.setString(1, s);
                ps.execute();

                try (final ResultSet rs =  ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        ids.add(rs.getLong(1));
                    }
                }
            }
        }
    }

    long lastDone = -1;

    public void clearSearch()
    {
        lastDone = -1;
    }

    public ArrayList<String> search(String keyWord, boolean strict) {

        try {
            final ArrayList<String> uniqueWords = getUniqueWordsFromText(keyWord);
            // ids, because we don't store the words themselves in the map - duh!
            final ArrayList<Long> wordIds = getWordIds(uniqueWords);

            if (wordIds.size() == 0) {
                return new ArrayList<>();
            }

            // docs associated with the words.
            final ArrayList<Long> docIds = doSearch(wordIds);
            if (docIds.size() == 0) {
                return new ArrayList<>();
            }
            return getDocRef(docIds);
        }
        catch (Exception e) {
            errorLog.add(e.getMessage());
            return new ArrayList<>();
        }
    }

    private ArrayList<Long> getWordIds(List<String> words) throws SQLException {

        final ArrayList<Long> wordIds = new ArrayList<>();

        final String q = "select id from words where name = ? limit 1";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {

            for (String s : words) {
                ps.setString(1, s);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.first()) {
                        wordIds.add(rs.getLong(1));
                    }
                }
            }
        }
        return wordIds;
    }

    boolean done = false;

    private  ArrayList<Long> doSearch(List<Long> wordIds) {

        final Long firstWord = wordIds.remove(0); // todo -- not this.
        final ArrayList<Long> result = new ArrayList<>();
        final HashSet<Long> popo = new HashSet<>();

        final String qGetDocIds = "select id, doc_id from map where word_id = ? and id > ? order by id limit 350 ";
        try (final PreparedStatement psGetDocIds = connection.prepareStatement(qGetDocIds)) {
            psGetDocIds.setLong(1, firstWord);
            psGetDocIds.setLong(2, lastDone);
            try (ResultSet rs = psGetDocIds.executeQuery())
            {
                while (rs.next()) {

                    popo.add(rs.getLong(2));
                    if (rs.isLast())
                    {
                        lastDone = rs.getLong(1);
                    }
                }
            }

            done = popo.isEmpty();

            final ArrayList<Long> docsWithFirstWord = new ArrayList<>(popo);
            if (wordIds.isEmpty())
            {
                return docsWithFirstWord;
            }

            // now find the rest.
            while (!docsWithFirstWord.isEmpty())
            {
               final long fid = docsWithFirstWord.remove(docsWithFirstWord.size() - 1);
               final String q2 = "select id from map where doc_id = ? and word_id = ? limit 1";

               try (PreparedStatement ps2 = connection.prepareStatement(q2))
               {
                   boolean ok = true;
                   for (Long op : wordIds)
                   {
                        ps2.setLong(1, fid);
                        ps2.setLong(2, op);
                        try (ResultSet rs = ps2.executeQuery())
                        {
                            if (!rs.first())
                            {
                                ok = false;
                                break;
                            }
                        }
                   }

                   if (ok)
                   {
                       result.add(fid);
                   }
               }
            }

        }
        catch (SQLException ignore) {ignore.printStackTrace();}
        return result;
    }

    private ArrayList<String> getDocRef(List<Long> docIds) throws SQLException {

        final ArrayList<String> docRef = new ArrayList<>();
        final String q = "select doc from docref where id = ? limit 1";

        try (final PreparedStatement ps = connection.prepareStatement(q)) {

            for (Long id : docIds) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.first()) {
                        docRef.add(rs.getString(1));
                    }
                }
            }
        }

        docRef.sort(Comparator.naturalOrder());
        return docRef;
    }

    public ArrayList<String> getErrorLog() {
        return errorLog;
    }

    public void close() {
        try {
            if (!connection.isClosed()) {
                connection.createStatement().execute("shutdown");
                connection.close();
            }
        } catch (Exception ignored) {}
    }

    public boolean strictOrderSearch(String keyword, String text) {

        try {
            return searchY(keyword,text);
        }
        catch (Exception e) {
            return false;
        }
    }

    private boolean searchY(String keyword, String text) {

        final String[] words = keyword.split(splitterRegex);
        final String[] longText = text.split(splitterRegex);

        int kwIndex = 0;
        int txtIndex = 0;

        // loop though kw
        for (; kwIndex < words.length; kwIndex++) {

            final String kw = words[kwIndex];
            if (kw.length() == 0) {
                continue;
            }

            for (; txtIndex < longText.length; txtIndex++) {
                final String st = longText[txtIndex];
                if (st.length() == 0) {
                    continue;
                }

                if (kw.equalsIgnoreCase(st)) {
                    // found - move to the next one
                    break;
                }
            }
            if (txtIndex >= longText.length) {
                break;
            }
        }
        return kwIndex >= words.length;
    }


    public void dropMapIndex() {

        try {
            connection.createStatement().execute("drop index if exists mdi");
            connection.createStatement().execute("drop index if exists mwi");
            connection.createStatement().execute("drop index if exists iddr");
            connection.createStatement().execute("drop index if exists idname");
        }
        catch (Exception ignored) {}
    }

    public void addMapIndex() {

        try {
            connection.createStatement().execute("create index if not exists mdi on map(doc_id)");
            connection.createStatement().execute("create index if not exists mwi on map(word_id)");
            connection.createStatement().execute("create index if not exists iddr on docref(doc)");
            connection.createStatement().execute("create index if not exists idname on words(name)");
        }
        catch (Exception ignored) {}
    }

    public boolean isDone()
    {
        return done;
    }
}
