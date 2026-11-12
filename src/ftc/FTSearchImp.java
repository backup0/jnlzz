package ftc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;

public class FTSearchImp {

    private final Connection connection;

    public FTSearchImp(Connection conn) {
        connection = conn;
    }

    public FTSearchImp() throws Exception {
        final String dbName = "ft_search3";
        final Path dbDir = Paths.get("D:\\Download");
        final String dbUrl = "jdbc:h2:" + dbDir.resolve(dbName) + ";TRACE_LEVEL_FILE=0";
        connection = DriverManager.getConnection(dbUrl, "sa", "");
    }

    public void update(String docref, String text) throws SQLException {
        
        final ArrayList<Long> wordIds = getWordIds(getUniqueWordsFromText(text));
        Long docId = getDocId(docref);
        
        if (docId == null) {
            docId = addDoc(docref);
        }
        else {
            removeWordsAlreadyAdded(docId, wordIds); //<<-- chnage to linked list prob faster.
        }
        storeAssociation(docId, wordIds);
    }

    private HashSet<String> getUniqueWordsFromText(String text) {

        final String[] words = text.split("[^\\p{Alnum}]");
        final HashSet<String> uniqueWordList = new HashSet<>();

        for (String s : words) {

            if (s.length() >= 1) {
                try {
                    uniqueWordList.add(s.toUpperCase());
                }
                catch (Exception ignored) {}
            }
        }
        return uniqueWordList;
    }

    private Long getDocId(String docref) throws SQLException{

        final String q = "select id from docs where docref = ? limit 1";
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

        final String q = "insert into docs(docref) values(?)";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {
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

        // add into the mapz
        final String q = "insert into map(wordid, docid) values (?, ?)";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {

            for (long id : wordIds) {

                ps.setLong(1, id);
                ps.setLong(2, docId);
                ps.execute();
            }

        }
    }

    private ArrayList<Long> getWordIds(HashSet<String> wordList) throws SQLException {

        final ArrayList<Long> wordIds = new ArrayList<>();

        final String q = "select id from words where name = ? limit 1";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {

            for (String word : wordList) {
                ps.setString(1, word);
                try (final ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        wordIds.add(rs.getLong(1));
                    }
                    else {
                        wordIds.add(addWord(word));
                    }
                }
            }
        }
        return wordIds;
    }

    private long addWord(String word) throws SQLException {

        final String q = "insert into words(name) values(?)";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {

            ps.setString(1, word);
            ps.execute();
            try (final ResultSet rs = ps.getGeneratedKeys()) {

                if (rs.next()) {
                    return rs.getLong(1);
                }
                else {
                    throw new SQLException("Can't get generated id.");
                }
            }
        }
    }

    private void removeWordsAlreadyAdded(long docId, ArrayList<Long> ids) throws SQLException {

        final ArrayList<Long> deletedWords = new ArrayList<>();

        final String q = "select wordid from map where docid = ?";
        try (final PreparedStatement ps = connection.prepareStatement(q)) {

            try (final ResultSet rs = ps.executeQuery()) {

                ps.setLong(1, docId);
                while (rs.next()) {
                    final Long wordId = rs.getLong(1);
                    final boolean b = ids.remove(wordId); //<<<---- this should use liked list or something.
                    if (!b) {
                        // we try to remove it but fail,
                        // that means it's not in the db anymore
                        deletedWords.add(wordId);
                    }
                }
            }
        }

        // remove words no longer in the text
        final String q2 = "delete from map where wordid = ? and docid = ?";
        try (final PreparedStatement ps = connection.prepareStatement(q2)) {

            for (Long wordId : deletedWords) {
                ps.setLong(1, wordId);
                ps.setLong(2, docId);
                ps.execute();
            }
        }
        // ok, we remove the words from the list, the caller will use the list - ya?
    }

    public void close() {
        try {
            connection.close();
        } catch (Exception ignored) {}
    }
}
