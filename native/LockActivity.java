package vn.truong.congtacphi;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Màn hình khoá của VP (bản 2.11): vân tay hoặc mã PIN 4 số.
 * - LockActivity: che app bị khoá (Zalo…). Đúng → vào app; Huỷ → về màn hình chính.
 * - VaultLockActivity: mở Kho riêng trong VP. Đúng → trả kết quả OK cho trang Kho riêng.
 */
public class LockActivity extends Activity {
    private final StringBuilder pin = new StringBuilder();
    private final TextView[] dots = new TextView[4];
    private TextView msg;
    private String pkg;
    private boolean bioShown = false;
    private boolean done = false;
    private final Handler ui = new Handler(Looper.getMainLooper());

    protected boolean isVault() { return false; }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        pkg = getIntent().getStringExtra("pkg");
        build();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pkg = intent.getStringExtra("pkg");
        pin.setLength(0);
        bioShown = false;
        done = false;
        build();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#14181F"));
        root.setPadding(dp(24), dp(56), dp(24), dp(32));

        // Biểu tượng + tên app đang bị khoá
        String label = "Kho riêng";
        Drawable icon = null;
        if (!isVault() && pkg != null) {
            try {
                PackageManager pm = getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                label = pm.getApplicationLabel(ai).toString();
                icon = pm.getApplicationIcon(ai);
            } catch (Exception ignored) {}
        }
        ImageView iv = new ImageView(this);
        if (icon != null) iv.setImageDrawable(icon); else iv.setImageResource(getApplicationInfo().icon);
        root.addView(iv, new LinearLayout.LayoutParams(dp(64), dp(64)));

        TextView t = text("🔒 " + label, 22, true, "#E8ECF2");
        t.setPadding(0, dp(14), 0, dp(4));
        root.addView(t);
        root.addView(text(isVault() ? "Đặt ngón tay lên cảm biến hoặc gõ mã PIN 4 số của VP"
                : "App này đang được VP khoá. Dùng vân tay hoặc gõ mã PIN 4 số để mở", 15, false, "#98A2B3"));

        LinearLayout dl = new LinearLayout(this);
        dl.setGravity(Gravity.CENTER);
        dl.setPadding(0, dp(28), 0, dp(8));
        for (int i = 0; i < 4; i++) {
            dots[i] = text("○", 30, false, "#E8ECF2");
            dots[i].setPadding(dp(10), 0, dp(10), 0);
            dl.addView(dots[i]);
        }
        root.addView(dl);
        msg = text(" ", 15, false, "#F5B04A");
        msg.setPadding(0, 0, 0, dp(16));
        root.addView(msg);

        String[][] keys = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"Vân tay", "0", "Xoá"}};
        for (String[] row : keys) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER);
            for (String k : row) {
                TextView key = text(k, k.length() == 1 ? 28 : 15, k.length() == 1, "#E8ECF2");
                key.setGravity(Gravity.CENTER);
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(Color.parseColor(k.length() == 1 ? "#262E3A" : "#14181F"));
                key.setBackground(bg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(76), dp(76));
                lp.setMargins(dp(12), dp(8), dp(12), dp(8));
                key.setOnClickListener(v -> press(k));
                r.addView(key, lp);
            }
            root.addView(r);
        }

        TextView cancel = text(isVault() ? "Huỷ" : "Huỷ – về màn hình chính", 16, true, "#5B9BFF");
        cancel.setPadding(dp(16), dp(20), dp(16), dp(8));
        cancel.setOnClickListener(v -> giveUp());
        root.addView(cancel);
        setContentView(root);
        refresh();
    }

    private TextView text(String s, int sp, boolean bold, String color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.parseColor(color));
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private void refresh() {
        for (int i = 0; i < 4; i++) dots[i].setText(i < pin.length() ? "●" : "○");
    }

    private void press(String k) {
        if (k.equals("Vân tay")) { showBio(); return; }
        if (k.equals("Xoá")) { if (pin.length() > 0) pin.setLength(pin.length() - 1); refresh(); return; }
        long w = VpLockStore.waitMs(this);
        if (w > 0) { msg.setText("Sai nhiều lần. Chờ " + (w / 1000 + 1) + " giây rồi thử lại."); return; }
        if (pin.length() >= 4) return;
        pin.append(k);
        refresh();
        if (pin.length() == 4) {
            if (VpLockStore.checkPin(this, pin.toString())) {
                success();
            } else {
                msg.setText("Sai mã PIN, gõ lại.");
                ui.postDelayed(() -> { pin.setLength(0); refresh(); }, 350);
            }
        }
    }

    private boolean bioAvailable() {
        if (Build.VERSION.SDK_INT < 29) return false;
        try {
            BiometricManager bm = getSystemService(BiometricManager.class);
            return bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    private void showBio() {
        if (!bioAvailable()) { msg.setText("Máy chưa cài vân tay. Gõ mã PIN."); return; }
        try {
            BiometricPrompt bp = new BiometricPrompt.Builder(this)
                    .setTitle("Mở khoá bằng vân tay")
                    .setSubtitle(isVault() ? "Kho riêng của VP" : "App đang được VP khoá")
                    .setNegativeButton("Dùng mã PIN", getMainExecutor(), (d, w) -> {})
                    .build();
            bp.authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) { success(); }
            });
        } catch (Exception e) {
            msg.setText("Không mở được vân tay. Gõ mã PIN.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!bioShown && !done) {
            bioShown = true;
            ui.postDelayed(() -> { if (!done && bioAvailable()) showBio(); }, 250);
        }
    }

    private void success() {
        if (done) return;
        done = true;
        if (isVault()) {
            setResult(RESULT_OK);
        } else {
            VpLockStore.unlockedPkg = pkg;
        }
        finish();
        overridePendingTransition(0, 0);
    }

    private void giveUp() {
        if (done) return;
        done = true;
        if (isVault()) {
            setResult(RESULT_CANCELED);
        } else {
            Intent h = new Intent(Intent.ACTION_MAIN);
            h.addCategory(Intent.CATEGORY_HOME);
            h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(h); } catch (Exception ignored) {}
        }
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        giveUp();
    }
}
