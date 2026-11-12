package Misc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class X {

    public static final String WARNING_TEXT_COLOR = "#FF5555";

    public static void w(Object... o) {

        final StringBuilder sbs = new StringBuilder();
        for (Object i : o) {
            if (i == null) continue;
            sbs.append(i);
        }
        System.out.println(sbs.toString());
    }

    public static String getPathWithoutMvDb(String p) {

        final StringBuilder sb1 = new StringBuilder(p);
        final StringBuilder sb2 = new StringBuilder();
        while (sb1.length() > 6) {
            sb2.append(sb1.charAt(0));
            sb1.deleteCharAt(0);
        }

        if (".mv.db".equalsIgnoreCase(sb1.toString())) {
            return sb2.toString();
        }
        else {
            return p;
        }
    }

    public static Connection getConnection(String dbLoc, String pwd) throws SQLException {

        dbLoc = X.getPathWithoutMvDb(dbLoc);
        final String conUrl;
        if ("".equalsIgnoreCase(pwd)) {
            // not encrypted
            conUrl = "jdbc:h2:" + dbLoc;
        }
        else {
            // yes pass
            conUrl = "jdbc:h2:" + dbLoc + ";CIPHER=XTEA";
            pwd = pwd + " ";
        }
        return DriverManager.getConnection(conUrl, "sa", pwd);
    }
}
