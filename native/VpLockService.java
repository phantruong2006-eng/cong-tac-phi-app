package vn.truong.congtacphi;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Lớp 1 – Khoá khi mở app (bản 2.11).
 * Khi một app trong Kho riêng hiện lên màn hình (mở bằng biểu tượng, ô tìm kiếm, thông báo…),
 * VP che lại bằng màn hình khoá: phải có vân tay hoặc mã PIN mới dùng được.
 * Mở khoá xong thì dùng thoải mái; về màn hình chính hoặc tắt màn hình là khoá lại.
 */
public class VpLockService extends AccessibilityService {
    private final Set<String> ignore = new HashSet<>();
    private final Set<String> home = new HashSet<>();
    private BroadcastReceiver screenOff;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        ignore.add(getPackageName());
        ignore.add("com.android.systemui");
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) for (InputMethodInfo i : imm.getEnabledInputMethodList()) ignore.add(i.getPackageName());
        } catch (Exception ignored) {}
        home.add("com.sec.android.app.launcher");
        try {
            Intent h = new Intent(Intent.ACTION_MAIN);
            h.addCategory(Intent.CATEGORY_HOME);
            for (ResolveInfo r : getPackageManager().queryIntentActivities(h, 0)) home.add(r.activityInfo.packageName);
        } catch (Exception ignored) {}
        screenOff = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) { VpLockStore.unlockedPkg = null; }
        };
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    // ===== Lớp 2 – chặn tìm kiếm (bản 2.13) =====
    // Gõ chữ đầu tên app trong kho (vd "z", "za", "zalo") vào ô tìm kiếm của Samsung → VP xoá chữ ngay.
    private static final Set<String> SEARCH = new HashSet<>(java.util.Arrays.asList(
            "com.sec.android.app.launcher", "com.samsung.android.app.galaxyfinder", "com.samsung.android.smartsuggestions"));
    private Set<String> namesFor = null;
    private final List<String[]> names = new ArrayList<>();

    static String norm(CharSequence cs) {
        String s = Normalizer.normalize(cs.toString(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
        return s.replaceAll("[^a-z0-9 ]", " ").trim().replaceAll(" +", " ");
    }

    private void loadNames() {
        Set<String> locked = VpLockStore.locked(this);
        if (locked.equals(namesFor)) return;
        names.clear();
        PackageManager pm = getPackageManager();
        for (String p : locked) {
            try { names.add(norm(pm.getApplicationLabel(pm.getApplicationInfo(p, 0))).split(" ")); }
            catch (Exception ignored) {}
        }
        namesFor = locked;
    }

    private boolean blocked(String q) {
        if (q.isEmpty()) return false;
        String qs = q.replace(" ", "");
        for (String[] w : names) {
            String full = String.join("", w);
            if (full.startsWith(qs)) return true;
            if (qs.length() >= 3 && full.contains(qs)) return true;
            if (q.length() >= 2) for (String x : w) if (!x.isEmpty() && x.startsWith(q)) return true;
        }
        return false;
    }

    private void checkSearch(AccessibilityEvent e) {
        if (!VpLockStore.searchBlock(this)) return;
        StringBuilder sb = new StringBuilder();
        for (CharSequence t : e.getText()) if (t != null) sb.append(t);
        if (sb.length() == 0) return;
        loadNames();
        if (names.isEmpty() || !blocked(norm(sb))) return;
        AccessibilityNodeInfo n = e.getSource();
        if (n == null) return;
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
        n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null) return;
        CharSequence cs = e.getPackageName();
        if (cs == null) return;
        String pkg = cs.toString();
        if (e.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            if (SEARCH.contains(pkg) || home.contains(pkg)) { try { checkSearch(e); } catch (Exception ignored) {} }
            return;
        }
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (ignore.contains(pkg)) return;
        if (home.contains(pkg)) { VpLockStore.unlockedPkg = null; return; }
        if (pkg.equals(VpLockStore.unlockedPkg)) return;
        if (!VpLockStore.locked(this).contains(pkg)) return;
        Intent i = new Intent(this, LockActivity.class);
        i.putExtra("pkg", pkg);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try { startActivity(i); } catch (Exception ignored) {}
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        try { if (screenOff != null) unregisterReceiver(screenOff); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
