package Vue;

import java.util.Calendar;

public class DateAndTime {

    private static final Calendar calendar = Calendar.getInstance();

    public static YDate getYDate(int encDate) {

        final int[] dds = decodeDate(encDate);

        calendar.clear();
        calendar.set(Calendar.YEAR, dds[0]);
        calendar.set(Calendar.MONTH, dds[1]);
        calendar.set(Calendar.DAY_OF_MONTH, dds[2]);

        return new YDate(encDate, calendar.getTime());
    }

    public static int[] decodeDate(int encDate) {

        final String s = encDate + "";
        final int[] ints = new int[3];

        ints[0] = Integer.parseInt(s.substring(0, 4));
        ints[1] = Integer.parseInt(s.substring(4, 6));
        ints[2] = Integer.parseInt(s.substring(6));

        return ints;
    }

    public static int encodeDate(int yr, int mth, int dy) {

        final StringBuilder sb = new StringBuilder();
        sb.append(yr);

        final String month = mth + "";
        if (month.length() == 1) {
            sb.append("0");
        }
        sb.append(mth);

        final String day = dy + "";
        if (day.length() == 1) {
            sb.append("0");
        }
        sb.append(day);

        return Integer.parseInt(sb.toString());
    }

    public static int[] getDateDetails(int encDate) {

        calendar.clear();
        final int[] date = decodeDate(encDate);
        calendar.set(date[0], date[1] - 1, date[2]);

        return new int[] {date[0], date[1], date[2], calendar.get(Calendar.DAY_OF_WEEK)};
    }

    public static String[] getNamedDay(int day) {

        final String[] s = new String[2];

        switch (day) {
            case 1:
                s[0] = "Sun"; s[1] = "Sunday";
                break;
            case 2:
                s[0] = "Mon"; s[1] = "Monday";
                break;
            case 3:
                s[0] = "Tue"; s[1] = "Tuesday";
                break;
            case 4:
                s[0] = "Wed"; s[1] = "Wednesday";
                break;
            case 5:
                s[0] = "Thu"; s[1] = "Thursday";
                break;
            case 6:
                s[0] = "Fri"; s[1] = "Friday";
                break;
            case 7:
                s[0] = "Sat"; s[1] = "Saturday";
                break;
            default:
                throw new RuntimeException("No a correct day.");

        }
        return s;
    }

    public static int getYearPart(int encDate) {

        final String dd = ("" + encDate).substring(0, 4);
        return Integer.parseInt(dd);
    }

    public static int yearMin(int year) {
        return Integer.parseInt(year + "0101");
    }

    public static int yearMax(int year) {
        return Integer.parseInt(year + "1231");
    }

    public static int[] getTodayDate() {
        calendar.clear();
        calendar.setTimeInMillis(System.currentTimeMillis());

        return new int[]{ calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)};
    }

    public static int getTodayDateInInt() {

        final int[] dInt = getTodayDate();
        final StringBuilder sbs = new StringBuilder();
        sbs.append(dInt[2]);
        if (sbs.length() < 2) {
            sbs.insert(0, "0");
        }
        sbs.insert(0, dInt[1]);
        if (sbs.length() < 4) {
            sbs.insert(0, "0");
        }
        sbs.insert(0, dInt[0]);

        return Integer.parseInt(sbs.toString());
    }
}
