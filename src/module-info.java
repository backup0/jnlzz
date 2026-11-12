
module jnlz {

    requires java.sql;
    requires javafx.controls;
    requires com.h2database;
    requires jdk.compiler;
    requires java.desktop;

    exports Vue;
    exports ftc;

    uses java.sql.Driver;
    opens imgs;
    opens skin;
}