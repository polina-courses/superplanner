package ru.polina.blizko;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Хранит данные приложения в файле data.json во внутренней памяти. */
final class Store {
    private Store() { }

    private static File file(Context c) {
        return new File(c.getFilesDir(), "data.json");
    }

    static synchronized String read(Context c) {
        File f = file(c);
        if (!f.exists()) return "";
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    static synchronized void write(Context c, String json) {
        File tmp = new File(c.getFilesDir(), "data.json.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (Exception e) {
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        tmp.renameTo(file(c));
    }
}
