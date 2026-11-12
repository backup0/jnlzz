package Vue;

import Misc.X;
import ftc.FTSearchDBIO;
import ftc.FullTextSearch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Ploc {

    // select * from daily where eid >= 20190000 and eid < 20200000;
    // select * from daily where eid >= 20020201 and eid <= 20020231
    // select min(eid), max(eid) from daily;
    private final Connection conn;
    public final FullTextSearch ftSearch;
    private final Path dbPath;
    private final String ftsDBName;

    public static void createNoteBook(String dbLoc, String pwd) throws Exception {

        final Connection conn = X.getConnection(dbLoc, pwd);
        //
        final String qDaily = "create table daily(id int primary key, deleted boolean default false, entry blob, indexed boolean default false, updated int);";
        final String qLoose = "create table loose(id int generated always as identity primary key, name varchar not null, entry blob, odr int, indexed boolean default false, created integer, updated integer, deleted boolean default false, editable boolean default true);";
        final String qSettings = "create table settings(name varchar primary key, val varchar);";
        // final String q3 = create table(id int generated always as identity primary key, entry_id int, entry blob);
        conn.createStatement().execute(qDaily);
        conn.createStatement().execute(qLoose);
        conn.createStatement().execute(qSettings);

        // name for our full text search index db, because -- we'll be storing them all in one folder.
        // don't want collision.
        final String ftsDBName = "fts" + System.currentTimeMillis() + "_" + (new Random()).nextInt(1650);
        try (PreparedStatement ps = conn.prepareStatement("insert into settings values(?, ?)")) {
            ps.setString(1, GlobalX.localSettings_fts_db_name);
            ps.setString(2, ftsDBName);
            ps.execute();
        }

        try {
            conn.close();
        }
        catch (Exception ignored) {}
    }

    public Ploc(Path p, String pwd) throws SQLException {
        /*
        "XTEA" - the 32 round version.
        String url = "jdbc:h2:~/test;CIPHER=AES";
        String user = "sa";
        String pwds = "filepwd userpwd";
        conn = DriverManager.
        getConnection(url, user, pwds);
        */
        conn = X.getConnection(p.toString(), pwd);
        ftsDBName = getFTSdbName();
        ftSearch = openFullTextSearchDB(pwd);
        dbPath = p;
        pwd = "fakarakash-wh00t?-xxxXX?";
        createDeleteDailyTable();
    }

    private void createDeleteDailyTable() {

        // because I forgot to create this before -- duhs!
        try {
            final String q;
            q = "create table if not exists deleted_entries(id int generated always as identity primary key, " +
                    "entry_id int, daily boolean, entry blob, dt varchar)";
            conn.createStatement().execute(q);
        }
        catch (Exception ignored) {
            ignored.printStackTrace();
        }

        //deleted_daily
    }

    public String ftsDBName() {
        return ftsDBName;
    }

    public Path getDbPath() {
        return dbPath;
    }

    private FullTextSearch openFullTextSearchDB(String pwd) {

        if (ftsDBName == null) {
            return null;
        }

        final Path p = GlobalX.fullTextSearchDBFolder.resolve(ftsDBName + ".mv.db");

        try {
            if (!p.toFile().exists()) {
                FTSearchDBIO.createDB(p, pwd);
                resetIndexedFlag();
            }
            return new FullTextSearch(X.getConnection(p.toString(), pwd));
        } catch (Exception ignored) {}
        return null;
    }

    private String getFTSdbName() {

        final String q = "select val from settings where name = ? limit 1";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, GlobalX.localSettings_fts_db_name);
            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.first()) {
                    return null;
                }

                return rs.getString(1);
            }
        } catch (Exception ignored) {}
        return null;

    }

    public void createDayEntry(int id) throws Exception {

        if (dayEntryExists(id)) {
            reviveDayEntry(id);
            return;
        }

        synchronized (conn) {

            final String qInsert = "insert into daily(id) values(?)";
            try (PreparedStatement ps = conn.prepareStatement(qInsert)){
                ps.setInt(1, id);
                ps.execute();
            }
            catch (SQLException e) {
                // duplicate primary key.
                if (e.getErrorCode() != 23505) {
                    throw e;
                }
            }
        }
    }

    private void reviveDayEntry(int id) throws SQLException {

        synchronized (conn) {
            final String q = "update daily set deleted = false, updated = ? where id = ?";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, id);
                ps.setInt(2, id);
                ps.execute();
            }
        }
    }

    public void deleteTodayEntry(int id, String text) throws SQLException, IOException {

        // move first ...
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DeflaterOutputStream dos = new DeflaterOutputStream(bos);
        dos.write(text.getBytes(StandardCharsets.UTF_8));
        dos.close();

        final String qInsert = "insert into deleted_daily(entry_id, entry) values(?, ?)";
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(qInsert)) {
                ps.setInt(1, id);
                ps.setBlob(2, new ByteArrayInputStream(bos.toByteArray()));
                ps.execute();
            }
        }
        storeDailyText(id, "");
    }

    public void deleteDailyEntry(int id, String text) throws IOException, SQLException {

        final String qUpdate = "update daily set deleted = true, entry = null, indexed = false where id = ?";
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(qUpdate)) {
                ps.setInt(1, id);
                ps.execute();
            }
        }

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DeflaterOutputStream dos = new DeflaterOutputStream(bos);
        dos.write(text.getBytes(StandardCharsets.UTF_8));
        dos.close();

        final String qInsert = "insert into deleted_entries(entry_id, daily, entry, dt) values(?, true, ?, ?)";
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(qInsert)) {
                ps.setInt(1, id);
                ps.setBlob(2, new ByteArrayInputStream(bos.toByteArray()));
                ps.setString(3, "??"); // dta e...
                ps.execute();
            }
        }
    }

    public void storeDailyText(int id, String text) throws IOException, SQLException {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DeflaterOutputStream dos = new DeflaterOutputStream(bos);
        dos.write(text.getBytes(StandardCharsets.UTF_8));
        dos.close();

        final String psQ = "update daily set entry = ?, updated = ?, indexed = false where id = ?";
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(psQ)) {
                ps.setBlob(1, new ByteArrayInputStream(bos.toByteArray()));
                ps.setInt(2, DateAndTime.getTodayDateInInt());
                ps.setInt(3, id);
                ps.execute();
            }
        }
    }

    public String getDailyText(int id) throws SQLException, IOException {

        synchronized (conn) {

            final String psQ = "select entry from daily where id = ? limit 1";
            try (PreparedStatement ps = conn.prepareStatement(psQ)) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.first()) {
                        throw new SQLException("No data.");
                    }

                    final Blob blob = rs.getBlob(1);
                    if (rs.wasNull()) {
                        return "";
                    }

                    final InflaterInputStream iins = new InflaterInputStream(blob.getBinaryStream());
                    final byte[] content = iins.readAllBytes();

                    return new String(content, StandardCharsets.UTF_8);
                }
            }
        }
    }

    public ArrayList<Integer> getEntriesForYear(int year) throws SQLException {

        final ArrayList<Integer> ids = new ArrayList<>();

        final String q = "select id from daily where id >= ? and id <= ? and deleted = false;";
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(q)) {

                ps.setInt(1, DateAndTime.yearMin(year));
                ps.setInt(2, DateAndTime.yearMax(year));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getInt(1));
                    }
                }
                return ids;
            }
        }
    }

    public boolean hasEntryForYear(int year) throws SQLException {

        synchronized (conn) {

            final String q = "select id from daily where (id >= ? and id <= ?) and deleted = false limit 1";
            try (PreparedStatement ps =
                         conn.prepareStatement(q)) {

                ps.setInt(1, DateAndTime.yearMin(year));
                ps.setInt(2, DateAndTime.yearMax(year));

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.first();
                }
            }
        }
    }

    public int[] getMinMaxDates() {

        synchronized (conn) {
            try (ResultSet rs =
                         conn.createStatement().executeQuery("select min(id), max(id) from daily")) {
                if (!rs.next()) {
                    return new int[0];
                }
                return new int[]{rs.getInt(1), rs.getInt(2)};
            }
            catch (Exception ex) {
                return new int[0];
            }
        }
    }

    public boolean dayEntryExists(int id) throws SQLException {

        synchronized (conn) {
            final String s = "select id from daily where id = ? and deleted = false limit 1;";
            try (PreparedStatement ps = conn.prepareStatement(s)) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.first();
                }
            }
        }
    }

    public int getSomeDayEntry() throws SQLException {

        synchronized (conn) {

            final String s = "select max(id) from daily where deleted = false;";
            try (PreparedStatement ps = conn.prepareStatement(s)) {

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.first()) {
                        return rs.getInt(1);
                    }
                    else {
                        return -1;
                    }
                }
            }
        }
    }

    public ArrayList<LooseMeta> getLooseEntries() throws SQLException {

        final ArrayList<LooseMeta> lms = new ArrayList<>();

        synchronized (conn) {

            final String s = "select id, name from loose where deleted is false order by odr asc";
            try (ResultSet rs = conn.createStatement().executeQuery(s)) {

                while (rs.next()) {
                    lms.add(new LooseMeta(rs.getInt(1), rs.getString(2)));
                }
            }
        }
        return lms;
    }

    public String getLooseName(int id) throws SQLException {

        synchronized (conn) {

            final String s = "select name from loose where id = ? and deleted is false limit 1";
            try (PreparedStatement ps = conn.prepareStatement(s)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.first()) {
                        return rs.getString(1);
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    public LooseMeta createLoose(String name) throws SQLException {

        final Integer id;
        synchronized (conn) {
            id = addLooseEntry(name);
        }

        if (id == null) {
            throw new SQLException("Cannot create new entry.");
        }

        return new LooseMeta(id, name);
    }

    private Integer addLooseEntry(String name) throws SQLException {

        final int order;
        try (ResultSet rs = conn.createStatement().executeQuery( "select max(odr) from loose")) {
            if (rs.first()) {
                order = rs.getInt(1);
            }
            else {
                order = 0;
            }
        }


        final String q = "insert into loose(name, odr, created, updated) values(?, ?, ?, ?)";
        final Integer newId;
        try (PreparedStatement ps = conn.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setInt(2, order + 1);
            final int d = DateAndTime.getTodayDateInInt();
            ps.setInt(3, d);
            ps.setInt(4, d);
            ps.execute();

            try (ResultSet rs = ps.getGeneratedKeys()) {

                if (rs.first()) {
                    newId = rs.getInt(1);
                }
                else {
                    newId = null;
                }
            }
        }

        return newId;
    }

    public void deleteLooseEntry(int id) throws SQLException {

        synchronized (conn) {
            final String q = "update loose set deleted = true, indexed = false  where id = ?";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setInt(1, id);
                ps.execute();
            }
        }
    }

    public String getLooseText(int id) throws SQLException, IOException {

        synchronized (conn) {
            final String psQ = "select entry from loose where id = ? limit 1";

            try (PreparedStatement ps = conn.prepareStatement(psQ)) {
                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.first()) {
                        throw new SQLException("No data.");
                    }

                    final Blob blob = rs.getBlob(1);
                    if (rs.wasNull()) {
                        return "";
                    }

                    final InflaterInputStream iins = new InflaterInputStream(blob.getBinaryStream());
                    final byte[] content = iins.readAllBytes();

                    return new String(content, StandardCharsets.UTF_8);
                }
            }
        }
    }

    public void storeLooseText(int id, String text) throws IOException, SQLException {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DeflaterOutputStream dos = new DeflaterOutputStream(bos);
        dos.write(text.getBytes(StandardCharsets.UTF_8));
        dos.close();

        final String psQ = "update loose set entry = ?, updated = ?, indexed = false where id = ?";
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(psQ)) {
                ps.setBlob(1, new ByteArrayInputStream(bos.toByteArray()));
                ps.setInt(2, DateAndTime.getTodayDateInInt());
                ps.setInt(3, id);
                ps.execute();
            }
        }
    }

    public void rename(int id, String name) throws SQLException {

        synchronized (conn) {
            final String qs = "update loose set name = ? where id = ?";
            try (PreparedStatement ps = conn.prepareStatement(qs)) {
                ps.setString(1, name);
                ps.setInt(2, id);
                ps.execute();
            }
        }
    }

    public void reOrder(ArrayList<LooseMeta> lms) throws SQLException {

        final String q = "update loose set odr = ? where id = ?";

        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(q)) {

                for (int i = 0; i < lms.size(); i++) {
                    final LooseMeta lm = lms.get(i);
                    ps.setInt(1, i);
                    ps.setInt(2, lm.id);
                    ps.execute();
                }
            }
        }
    }

    public ArrayList<Integer> getAllDays() {

        final ArrayList<Integer> ids = new ArrayList<>();

        final String q = "select id from daily;";
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(q)) {

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                // todo looks like not used -- so why here?
            }
        }
        return ids;
    }

    public ArrayList<Integer> getUnIndexedDaily() {

        final ArrayList<Integer> ids = new ArrayList<>();

        final String q = "select id from daily where indexed is false;";
        try (PreparedStatement ps = conn.prepareStatement(q)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (Exception ignored) {}
        return ids;
    }

    public ArrayList<Integer> getUnIndexedLoose() {

        final ArrayList<Integer> ids = new ArrayList<>();

        final String q = "select id from loose where indexed is false;";
        try (PreparedStatement ps = conn.prepareStatement(q)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }

        } catch (Exception ignored) {}
        return ids;
    }

    public void dailySetIndexed(int id) {

        try (PreparedStatement ps = conn.prepareStatement("update daily set indexed = true where id = ?")){
            ps.setInt(1, id);
            ps.execute();
        } catch (Exception ignored) {
            // no need to handle.
            // so we can't flag it --- so what?
        }
    }

    public void dailySetNotIndexed(int id) {

        try (PreparedStatement ps = conn.prepareStatement("update daily set indexed = false where id = ?")){
            ps.setInt(1, id);
            ps.execute();
        } catch (Exception ignored) {
            // no need to handle.
            // so we can't flag it --- so what?
        }
    }

    public void looseSetIndexed(int id) {

        try (PreparedStatement ps = conn.prepareStatement("update loose set indexed = true where id = ?")){
            ps.setInt(1, id);
            ps.execute();
        } catch (Exception ignored) {
            // no need to handle.
            // so we can't flag it --- so what?
        }
    }

    public void looseSetNotIndexed(int id) {

        try (PreparedStatement ps = conn.prepareStatement("update loose set indexed = false where id = ?")){
            ps.setInt(1, id);
            ps.execute();
        } catch (Exception ignored) {
            // no need to handle.
            // so we can't flag it --- so what?
        }
    }

    public void updateFT(String docRef, String txt) {
        if (ftSearch == null) {
            return;
        }
        ftSearch.update(docRef, txt);
    }

    public ArrayList<String> searchEntries(String keyWord) {

        if (ftSearch == null) {
            return new ArrayList<>();
        }
        // todo: need to exclude deleted entries
        return ftSearch.search(keyWord, true);
    }

    private void resetIndexedFlag() {

        try {
            conn.createStatement().execute("update daily set indexed = false where deleted = false");
            conn.createStatement().execute("update loose set indexed = false where deleted = false");
        }
        catch (Exception ignored) {}
    }

    public boolean isDeleted_daily(int id) {

        synchronized (conn) {
            try (final PreparedStatement ps = conn.prepareStatement(
                    "select deleted from daily where id = ? limit 1")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.first()) {
                        return rs.getBoolean(1);
                    }
                    else {
                        return true;
                    }
                }
            }
            catch (Exception e) {
                return true;
            }
        }
    }

    public boolean getEditability(int id) {

        synchronized (conn) {
            try (final PreparedStatement ps = conn.prepareStatement(
                    "select editable from loose where id = ? limit 1")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.first()) {
                        return rs.getBoolean(1);
                    }
                    else {
                        return true;
                    }
                }
            }
            catch (Exception e) {
                return false;
            }
        }

    }

    public void setEditabilityForLoose(int id, boolean editable) {

        synchronized (conn) {
            try (final PreparedStatement ps =
                         conn.prepareStatement("update loose set editable = ? where id = ?")) {

                ps.setBoolean(1, editable);
                ps.setInt(2, id);
                ps.execute();
            }
            catch (Exception ignored) {}
        }


    }

    public boolean isDeleted_loose(int id) {

        synchronized (conn) {
            try (final PreparedStatement ps = conn.prepareStatement(
                    "select deleted from loose where id = ? limit 1")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()){
                    if (rs.first()) {
                        return rs.getBoolean(1);
                    }
                    else {
                        return true;
                    }
                }
            }
            catch (Exception e) {
                return true;
            }
        }
    }

    public void dropFTSIndice()
    {
        ftSearch.dropMapIndex();
    }

    public void addFTSMapIndex() {
        ftSearch.addMapIndex();
    }

    public int[] countEntries() {

        final int[] eCounts = new int[4];
        synchronized (conn) {

            final String q1 = "select count(*) from daily where deleted is false;";
            try (PreparedStatement ps = conn.prepareStatement(q1)) {

                try (ResultSet rs = ps.executeQuery()){
                    if (rs.first()) {
                        eCounts[0] = rs.getInt(1);
                    }
                    else {
                        eCounts[0] = 0;
                    }
                }
            }
            catch (Exception ignored) {}

            final String q2 = "select count(*) from daily where deleted is true;";
            try (PreparedStatement ps = conn.prepareStatement(q2)) {

                try (ResultSet rs = ps.executeQuery()){
                    if (rs.first()) {
                        eCounts[1] = rs.getInt(1);
                    }
                    else {
                        eCounts[1] = 0;
                    }
                }
            }
            catch (Exception ignored) {}

            final String q3 = "select count(*) from loose where deleted is false;";
            try (PreparedStatement ps = conn.prepareStatement(q3)) {

                try (ResultSet rs = ps.executeQuery()){
                    if (rs.first()) {
                        eCounts[2] = rs.getInt(1);
                    }
                    else {
                        eCounts[2] = 0;
                    }
                }
            }
            catch (Exception ignored) {}

            final String q4 = "select count(*) from loose where deleted is true;";
            try (PreparedStatement ps = conn.prepareStatement(q4)) {

                try (ResultSet rs = ps.executeQuery()){
                    if (rs.first()) {
                        eCounts[3] = rs.getInt(1);
                    }
                    else {
                        eCounts[3] = 0;
                    }
                }
            }
            catch (Exception ignored) {}
        }
        return eCounts;
    }

    public void close() {

        synchronized (conn) {
            try {
                if (!conn.isClosed()) {
                    conn.createStatement().execute("shutdown");
                    conn.close();
                }
                ftSearch.close();
            } catch (Exception ignored) {
            }
        }
    }
}