package ftc;

import Misc.X;

import java.nio.file.Path;
import java.sql.Connection;

public class FTSearchDBIO {

    public static void createDB(Path dbPath, String password) throws Exception {
        createDB(dbPath.toString(), password);
    }

    public static void createDB(String dbPath, String password) throws Exception {

        try (final Connection conn = X.getConnection(dbPath, password)) {

            final String q =  "create table docref(id bigint generated always as identity primary key, doc varchar);" +
                    "create table words(id bigint generated always as identity primary key, name varchar);" +
                    "create table map(id bigint generated always as identity primary key, word_id bigint, doc_id bigint);" +
                    "create index iddr on docref(doc);" +
                    "create index idname on words(name);";
            conn.createStatement().execute(q);
        }
    }
}
