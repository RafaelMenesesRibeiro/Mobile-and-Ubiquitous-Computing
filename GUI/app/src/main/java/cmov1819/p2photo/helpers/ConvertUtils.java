package cmov1819.p2photo.helpers;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ConvertUtils {
    public static byte[] imageToByteArray(String imgPath) throws IOException {
        File imgFile = new File(imgPath);
        byte[] bytes = new byte[(int) imgFile.length()];
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(imgFile));
        bufferedInputStream.read(bytes,0, bytes.length);
        bufferedInputStream.close();
        return bytes;
    }
}
