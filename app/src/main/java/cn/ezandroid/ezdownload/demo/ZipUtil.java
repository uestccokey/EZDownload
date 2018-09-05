package cn.ezandroid.ezdownload.demo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ZipUtil
 *
 * @author like
 * @date 2018-09-04
 */
public class ZipUtil {

    public static void unZip(String zipName, String outputDirectory) throws IOException {
        // 创建解压目标目录
        File dir = new File(outputDirectory);
        // 如果目标目录不存在，则创建
        if (!dir.exists()) {
            dir.mkdirs();
        }
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zipName);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            ZipEntry zipEntry;
            File tmpFile;
            byte[] buf = new byte[1024];
            int len;
            while (entries.hasMoreElements()) {
                zipEntry = entries.nextElement();
                // 不进行文件夹的处理,些为特殊处理
                tmpFile = new File(dir, zipEntry.getName());
                if (zipEntry.isDirectory()) {//当前文件为目录
                    if (!tmpFile.exists()) {
                        tmpFile.mkdirs();
                    }
                } else {
                    if (!tmpFile.exists()) {
                        tmpFile.createNewFile();
                    }
                    InputStream is = null;
                    BufferedOutputStream bos = null;
                    try {
                        is = zipFile.getInputStream(zipEntry);
                        bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
                        while ((len = is.read(buf)) > 0) {
                            bos.write(buf, 0, len);
                        }
                        bos.flush();
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                        if (bos != null) {
                            bos.close();
                        }
                    }
                }
            }
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }
}
