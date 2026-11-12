package Vue;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

public class ImgGetter {

    private final HashMap<String, Image> _map = new HashMap<>();

    public Image getImg(String loc, boolean cache) {

        if (_map.containsKey(loc)) {
            return _map.get(loc);
        }
        else {

            final Image newImg = retrieveImg(loc);
            if (newImg != null) {
                // 7-5-2020 - cache some, don't cache some.
                if (cache){
                    _map.put(loc, newImg);
                }
                return newImg;
            }
            else {
                return null;
            }
        }
    }

    private Image retrieveImg(String loc) {

        final URL url = getClass().getResource(loc);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (InputStream ins = url.openStream()) {
            int i;
            while ((i = ins.read()) != -1) {
                bos.write(i);
            }
            return new Image(new ByteArrayInputStream(bos.toByteArray()));
        }
        catch (Exception ie) // 7-5-2020: use a more general one to catch all errors.
        {
            return null;
        }
    }
}