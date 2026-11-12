package Vue;

public class UpDownStuff {

    public final int type; // 0 up, 1 down.
    public final int id;
    public final Integer upDn;

    public UpDownStuff(int typ, int id, Integer ud) {
        type = typ;
        this.id = id;
        upDn = ud;
    }
}
