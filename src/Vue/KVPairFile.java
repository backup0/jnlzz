package Vue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class KVPairFile {

    private final Path file;
    private final HashMap<String, String> map = new HashMap<>();

    private final Charset CHAR_SET = StandardCharsets.UTF_8;

    public KVPairFile(Path p) {
        file = p;
    }

    public void readContent() throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(file.toString()))) {

            final ArrayList<String> al = new ArrayList<>();

            while (true) {
                final String s = br.readLine();
                if (s == null) {
                    break;
                }

                if ("".equalsIgnoreCase(s)) {
                    continue;
                }
                al.add(s);
            }

            if (al.size() % 2 != 0) {
                throw new Exception("File is bad");
            }

            for (int i = 0; i < al.size(); i = i + 2) {

                final String key = al.get(i).substring(1);
                final String rawValue = al.get(i + 1).substring(1);
                final String value = new String(Base64.getDecoder().decode(rawValue), CHAR_SET);
                map.putIfAbsent(key, value);
            }
        }
    }

    public void writeContent() throws Exception {

        final StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> es: map.entrySet()) {
            sb.append("?").append(es.getKey()).append(System.lineSeparator());
            final String value = Base64.getEncoder().encodeToString(es.getValue().getBytes(CHAR_SET));
            sb.append("%").append(value).append(System.lineSeparator());
        }
        Files.writeString(file, sb);
    }

    public HashMap<String, String> getMap() {
        return map;
    }
}
