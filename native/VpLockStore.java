package vn.truong.congtacphi;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

/** Kho riêng (bản 2.11): lưu mã PIN (đã mã hoá), danh sách app bị khoá, app đang được mở khoá. */
final class VpLockStore {
    private VpLockStore() {}

    /** App vừa mở khoá xong (không hỏi lại cho tới khi về màn hình chính / tắt màn hình). */
    static volatile String unlockedPkg = null;

    static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("vp_lock", Context.MODE_PRIVATE);
    }

    private static String hash(String salt, String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((salt + ":" + pin).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static boolean hasPin(Context c) {
        return p(c).getString("pin", null) != null;
    }

    static void setPin(Context c, String pin) {
        byte[] r = new byte[16];
        new SecureRandom().nextBytes(r);
        StringBuilder sb = new StringBuilder();
        for (byte b : r) sb.append(String.format("%02x", b));
        String salt = sb.toString();
        p(c).edit().putString("salt", salt).putString("pin", hash(salt, pin)).putInt("fails", 0).apply();
    }

    /** Sai 5 lần liền thì phải chờ 30 giây. */
    static long waitMs(Context c) {
        long until = p(c).getLong("waitUntil", 0);
        long now = System.currentTimeMillis();
        return until > now ? until - now : 0;
    }

    static boolean checkPin(Context c, String pin) {
        SharedPreferences sp = p(c);
        String h = sp.getString("pin", null);
        if (h == null) return false;
        boolean ok = h.equals(hash(sp.getString("salt", ""), pin));
        if (ok) {
            sp.edit().putInt("fails", 0).apply();
        } else {
            int f = sp.getInt("fails", 0) + 1;
            SharedPreferences.Editor e = sp.edit().putInt("fails", f);
            if (f >= 5) e.putInt("fails", 0).putLong("waitUntil", System.currentTimeMillis() + 30000);
            e.apply();
        }
        return ok;
    }

    /** Lớp 2 – chặn tìm kiếm (mặc định bật). */
    static boolean searchBlock(Context c) { return p(c).getBoolean("searchBlock", true); }
    static void setSearchBlock(Context c, boolean on) { p(c).edit().putBoolean("searchBlock", on).apply(); }

    static Set<String> locked(Context c) {
        return new HashSet<>(p(c).getStringSet("locked", new HashSet<>()));
    }

    static void setLocked(Context c, Set<String> pkgs) {
        p(c).edit().putStringSet("locked", new HashSet<>(pkgs)).apply();
    }
}
