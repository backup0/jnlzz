package Vue;

import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;

import java.util.ArrayList;

public class CrossSearch
{
    private final TextArea ta;
    private String[] keyWords = null;
    private int searchIndex = 0;
    private final ArrayList<String> als = new ArrayList<>();

    public CrossSearch(TextArea ta)
    {
        this.ta = ta;
    }

    public void setKeyWords(String kw)
    {
        keyWords = kw.split("\\h+");
        als.clear();
        searchIndex = 0;
    }

    public void search()
    {
        if (search2()) {
            search2();
        }
    }

    public boolean search2()
    {

        if (searchIndex >= keyWords.length) {
            searchIndex = 0;
        }

        GlobalX.searchy(ta, keyWords[searchIndex]);

        final IndexRange ir = ta.getSelection();
        final String irs = ir.getStart() + "_" + ir.getEnd();

        if (als.contains(irs)) {
            als.clear();
            searchIndex++;
            return true;
        }
        else {
            als.add(irs);
            return false;
        }
    }
}
