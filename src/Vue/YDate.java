package Vue;

import java.util.Date;

public class YDate {

    private final int encodedDate;
    private final Date date;

    public YDate(int encDate, Date d) {
        encodedDate = encDate;
        date = d;
    }

    public String toString() {
        return date.toString();
    }
}
