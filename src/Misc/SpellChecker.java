package Misc;

import Vue.GlobalX;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

public class SpellChecker
{
    private static final HashSet<String> giss = new HashSet<>(470_000);
    private static boolean ok = false;

    private static void loadDict()
    {
        if (ok)
        {
            return;
        }

        final File f;
        if (Files.exists(Paths.get(System.getProperty("java.home"), "full_eng_words2" )))
        {
            f = new File(System.getProperty("java.home"), "full_eng_words2");
        }
        else
        {
            f = new File("D:\\Disposable\\full_eng_words2");
        }

        try (var io = new BufferedReader(new FileReader(f)))
        {
            while (true)
            {
                String s = io.readLine();
                if (s == null)
                {
                    break;
                }
                giss.add(s);
            }
        }
        catch (Exception ignore)
        {
        }
        loadCustomDicts();
        ok = true;
    }

    private static void loadCustomDicts()
    {
        try (var io = new BufferedReader(new FileReader(GlobalX.customDict.toFile())))
        {
            while (true)
            {
                String s = io.readLine();
                if (s == null)
                {
                    break;
                }
                giss.add(s);
            }
        }
        catch (Exception ignore)
        {
        }
    }

    public static void addWord(String w)
    {
        giss.add(w.toLowerCase());
    }

    public static int checkSpelling(String s, HashSet<String> hs)
    {
        if (s.isEmpty())
        {
            return 0;
        }

        loadDict();

        final String[] words = s.split("[\\s/-]+"); // split by white space/dash?

        for (String w : words)
        {
            if (w.length() <= 1)
            {
                continue;
            }

            if (hs.contains(w)) // word already checked.
            {
                continue;
            }

            String es = w;
            es = es.replaceAll("[^a-zA-Z]+$", ""); // strip trailing? non alphabets

            es = es.replaceAll("^[^a-zA-Z]+", ""); // leading?

            if (es.matches("^[A-Z]+$"))
            {
                // all caps
                continue;
            }

            if (es.matches(".*[A-Z].*"))
            {
                // contains capital letter
                if (!Character.isUpperCase(es.codePointAt(0)))
                {
                    hs.add(es);
                    continue;
                }
            }

            if (es.endsWith("'s") || es.endsWith("'ll"))
            {
                final String gg = es.replace("'s", "");
                if (!giss.contains(gg.toLowerCase()))
                {
                    hs.add(es);
                }
                continue;
            }

            if (es.length() <= 1)
            {
                continue;
            }

            if (!giss.contains(es.toLowerCase()))
            {
                hs.add(es);
            }
        }
        return words.length;
    }
}
