package vn.truong.congtacphi;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cs = e.getPackageName();
        if (cs == null) return;
        String pkg = cs.toString();
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
