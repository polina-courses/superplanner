package ru.polina.blizko;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Обновление интерфейса без переустановки.
 * Свежий index.html берётся из репозитория на GitHub и хранится во внутренней памяти.
 * Данные пользователя этот класс не трогает и никуда не отправляет.
 */
final class Updater {
    static final String UI_URL =
            "https://raw.githubusercontent.com/polina-courses/superplanner/main/app/src/main/assets/www/index.html";
    static final String APK_URL =
            "https://github.com/polina-courses/superplanner/releases/latest/download/Blizko.apk";
    static final String ASSET_URL = "file:///android_asset/www/index.html";

    static final int UPDATED = 1, SAME = 0, ERROR = -1;

    private Updater() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("updater", Context.MODE_PRIVATE);
    }

    private static File cached(Context c) {
        return new File(new File(c.getFilesDir(), "www"), "index.html");
    }

    static long appVersion(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pi.getLongVersionCode();
        } catch (Exception e) {
            return 0;
        }
    }

    static String appVersionName(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    /** Какую страницу открыть: скачанную версию интерфейса или встроенную в APK. */
    static synchronized String startUrl(Context c) {
        SharedPreferences sp = prefs(c);
        long vc = appVersion(c);
        if (sp.getLong("vc", -1) != vc) {
            // Установлен новый APK: в нём уже свежий интерфейс, старую скачанную копию убираем.
            //noinspection ResultOfMethodCallIgnored
            cached(c).delete();
            sp.edit().putLong("vc", vc).remove("updated").apply();
        }
        File f = cached(c);
        return f.exists() ? "file://" + f.getAbsolutePath() : ASSET_URL;
    }

    /** Скачанная версия не запустилась — удалить её и больше не принимать. */
    static synchronized void rejectCached(Context c) {
        File f = cached(c);
        if (f.exists()) {
            try {
                prefs(c).edit().putString("bad", sha(readFile(f))).remove("updated").apply();
            } catch (Exception ignored) { }
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    /** Когда интерфейс последний раз обновлялся из интернета (0 — используется встроенный). */
    static long updatedAt(Context c) {
        return cached(c).exists() ? prefs(c).getLong("updated", 0) : 0;
    }

    /** Проверяет репозиторий. Вызывать не из UI-потока. */
    static synchronized int check(Context c) {
        prefs(c).edit().putLong("lastCheck", System.currentTimeMillis()).apply();
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(UI_URL + "?t=" + System.currentTimeMillis()).openConnection();
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setUseCaches(false);
            if (con.getResponseCode() != 200) return ERROR;
            byte[] data = readAll(con.getInputStream());
            String s = new String(data, StandardCharsets.UTF_8);
            // Защита от обрывков и чужих страниц.
            if (data.length < 20000 || !s.contains("<title>Близко</title>") || !s.trim().endsWith("</html>")) {
                return ERROR;
            }
            if (sha(data).equals(prefs(c).getString("bad", ""))) return SAME;
            if (Arrays.equals(data, current(c))) return SAME;
            File f = cached(c);
            //noinspection ResultOfMethodCallIgnored
            f.getParentFile().mkdirs();
            File tmp = new File(f.getParentFile(), "index.html.tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(data);
                out.getFD().sync();
            }
            if (!tmp.renameTo(f)) return ERROR;
            prefs(c).edit().putLong("updated", System.currentTimeMillis()).apply();
            return UPDATED;
        } catch (Exception e) {
            return ERROR;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    /** Проверять не чаще раза в 20 минут при открытии приложения. */
    static boolean due(Context c) {
        return System.currentTimeMillis() - prefs(c).getLong("lastCheck", 0) > 20 * 60 * 1000L;
    }

    private static byte[] current(Context c) throws Exception {
        File f = cached(c);
        if (f.exists()) return readFile(f);
        try (InputStream in = c.getAssets().open("www/index.html")) {
            return readAll(in);
        }
    }

    private static byte[] readFile(File f) throws Exception {
        try (InputStream in = new FileInputStream(f)) {
            return readAll(in);
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    private static String sha(byte[] data) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
