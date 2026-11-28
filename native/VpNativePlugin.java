package vn.truong.congtacphi;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSArray;
import com.getcapacitor.annotation.ActivityCallback;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

@CapacitorPlugin(name = "VpNative")
public class VpNativePlugin extends Plugin {
    static final String MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String ZALO = "com.zing.zalo";

    // Ghi file vào Tải về/VP (ghi đè nếu trùng tên, không tạo bản "(1)") và trả về Uri dùng chung
    private Uri writeDownload(String name, byte[] bytes) throws Exception { return writeDownload(name, bytes, MIME); }

    private Uri writeDownload(String name, byte[] bytes, String mime) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver cr = getContext().getContentResolver();
            String rel = Environment.DIRECTORY_DOWNLOADS + "/VP/";
            Uri uri = null;
            Cursor c = cr.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " + MediaStore.MediaColumns.RELATIVE_PATH + "=?",
                    new String[]{name, rel}, null);
            if (c != null) {
                if (c.moveToFirst()) uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0));
                c.close();
            }
            if (uri == null) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, rel);
                uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new Exception("Không tạo được file trong Download");
            }
            OutputStream os = cr.openOutputStream(uri, "wt");
            os.write(bytes);
            os.close();
            return uri;
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VP");
            dir.mkdirs();
            File f = new File(dir, name);
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes);
            fo.close();
            return FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", f);
        }
    }

    // Lấy số lượng nên đặt do app BH offline gửi sang (take=true thì xoá sau khi lấy)
    @PluginMethod
    public void incoming(PluginCall call) {
        JSObject r = new JSObject();
        String d = MainActivity.incoming;
        r.put("data", d == null ? "" : d);
        if (call.getBoolean("take", false)) MainActivity.incoming = null;
        call.resolve(r);
    }

    @PluginMethod
    public void saveToDownloads(PluginCall call) {
        String name = call.getString("name");
        String data = call.getString("data");
        if (name == null || data == null) { call.reject("Thiếu dữ liệu"); return; }
        try {
            writeDownload(name, Base64.decode(data, Base64.DEFAULT));
            JSObject r = new JSObject();
            r.put("path", "Download/VP/" + name);
            call.resolve(r);
        } catch (Exception e) {
            call.reject("Không lưu được: " + e.getMessage());
        }
    }

    // Thử nghiệm: gửi bằng đường dẫn file thật (file://) để Zalo đọc thẳng file như trên máy tính
    @PluginMethod
    public void shareZaloFile(PluginCall call) {
        String name = call.getString("name");
        String text = call.getString("text", "");
        if (name == null) { call.reject("Thiếu dữ liệu"); return; }
        try {
            File f = new File(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VP"), name);
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(MIME);
            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
            if (text != null && !text.isEmpty()) i.putExtra(Intent.EXTRA_TEXT, text);
            i.setPackage(ZALO);
            JSObject r = new JSObject();
            try { getActivity().startActivity(i); r.put("zalo", true); }
            catch (ActivityNotFoundException e) { r.put("zalo", false); }
            call.resolve(r);
        } catch (Exception e) {
            call.reject("Không gửi được: " + e.getMessage());
        }
    }

    @PluginMethod
    public void openZalo(PluginCall call) {
        JSObject r = new JSObject();
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setPackage(ZALO);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { getActivity().startActivity(i); r.put("zalo", true); }
        catch (ActivityNotFoundException e) { r.put("zalo", false); }
        call.resolve(r);
    }

    @PluginMethod
    public void shareZalo(PluginCall call) {
        String name = call.getString("name");
        String data = call.getString("data");
        String text = call.getString("text", "");
        if (name == null || data == null) { call.reject("Thiếu dữ liệu"); return; }
        try {
            // Gửi file thật trong Tải về/VP (giống gửi từ app Tệp) để Zalo giữ đúng tên file tiếng Việt
            Uri uri = writeDownload(name, Base64.decode(data, Base64.DEFAULT));
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(MIME);
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_TITLE, name);
            if (text != null && !text.isEmpty()) i.putExtra(Intent.EXTRA_TEXT, text);
            i.setClipData(ClipData.newRawUri(name, uri));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.setPackage(ZALO);
            JSObject r = new JSObject();
            try {
                getActivity().startActivity(i);
                r.put("zalo", true);
            } catch (ActivityNotFoundException e) {
                i.setPackage(null);
                Intent ch = Intent.createChooser(i, "Gửi file");
                ch.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getActivity().startActivity(ch);
                r.put("zalo", false);
            }
            call.resolve(r);
        } catch (Exception e) {
            call.reject("Không gửi được: " + e.getMessage());
        }
    }

    // ===== Kho riêng (bản 2.11): giấu + khoá app =====

    private boolean lockServiceOn() {
        String s = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return s != null && s.contains(getContext().getPackageName() + "/") && s.contains("VpLockService");
    }

    @PluginMethod
    public void lockStatus(PluginCall call) {
        JSObject r = new JSObject();
        r.put("hasPin", VpLockStore.hasPin(getContext()));
        r.put("serviceOn", lockServiceOn());
        r.put("searchBlock", VpLockStore.searchBlock(getContext()));
        r.put("minChars", VpLockStore.minChars(getContext()));
        r.put("decoy", VpLockStore.decoy(getContext()));
        JSObject kw = new JSObject();
        for (String p : VpLockStore.locked(getContext())) kw.put(p, VpLockStore.keywords(getContext(), p));
        r.put("keywords", kw);
        JSArray a = new JSArray();
        for (String p : VpLockStore.locked(getContext())) a.put(p);
        r.put("locked", a);
        call.resolve(r);
    }

    @PluginMethod
    public void setPin(PluginCall call) {
        String pin = call.getString("pin", "");
        if (pin == null || !pin.matches("\\d{4}")) { call.reject("Mã PIN phải đúng 4 chữ số"); return; }
        VpLockStore.setPin(getContext(), pin);
        call.resolve();
    }

    @PluginMethod
    public void setSearchBlock(PluginCall call) {
        VpLockStore.setSearchBlock(getContext(), call.getBoolean("on", true));
        call.resolve();
    }

    @PluginMethod
    public void setDecoy(PluginCall call) {
        VpLockStore.setDecoy(getContext(), call.getString("d", ""));
        call.resolve();
    }

    @PluginMethod
    public void setMinChars(PluginCall call) {
        VpLockStore.setMinChars(getContext(), call.getInt("n", 2) == 1 ? 1 : 2);
        call.resolve();
    }

    @PluginMethod
    public void setKeywords(PluginCall call) {
        String p = call.getString("pkg");
        if (p == null) { call.reject("Thiếu app"); return; }
        VpLockStore.setKeywords(getContext(), p, call.getString("kw", ""));
        call.resolve();
    }

    @PluginMethod
    public void setLocked(PluginCall call) {
        JSArray a = call.getArray("pkgs");
        Set<String> s = new HashSet<>();
        try { if (a != null) for (int i = 0; i < a.length(); i++) s.add(a.getString(i)); }
        catch (Exception e) { call.reject("Danh sách app sai"); return; }
        VpLockStore.setLocked(getContext(), s);
        call.resolve();
    }

    // Mở màn hình vân tay / mã PIN của VP; trả về ok=true nếu đúng
    @PluginMethod
    public void verify(PluginCall call) {
        if (!VpLockStore.hasPin(getContext())) { JSObject r = new JSObject(); r.put("ok", false); call.resolve(r); return; }
        startActivityForResult(call, new Intent(getContext(), VaultLockActivity.class), "verifyDone");
    }

    @ActivityCallback
    private void verifyDone(PluginCall call, ActivityResult result) {
        if (call == null) return;
        JSObject r = new JSObject();
        r.put("ok", result.getResultCode() == android.app.Activity.RESULT_OK);
        call.resolve(r);
    }

    private String iconB64(Drawable d) {
        try {
            int n = 96;
            Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            d.setBounds(0, 0, n, n);
            d.draw(c);
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.PNG, 100, o);
            return Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    // Danh sách app có biểu tượng trên máy (kể cả app đã giấu biểu tượng)
    @PluginMethod
    public void listApps(PluginCall call) {
        PackageManager pm = getContext().getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
        Set<String> seen = new HashSet<>();
        JSArray a = new JSArray();
        for (ResolveInfo ri : list) {
            String p = ri.activityInfo.packageName;
            if (p.equals(getContext().getPackageName()) || !seen.add(p)) continue;
            JSObject o = new JSObject();
            o.put("pkg", p);
            o.put("label", ri.loadLabel(pm).toString());
            o.put("icon", iconB64(ri.loadIcon(pm)));
            a.put(o);
        }
        JSObject r = new JSObject();
        r.put("apps", a);
        call.resolve(r);
    }

    // Mở app từ Kho riêng (đã xác thực trong Kho riêng nên không hỏi lại)
    @PluginMethod
    public void launchApp(PluginCall call) {
        String p = call.getString("pkg");
        Intent i = p == null ? null : getContext().getPackageManager().getLaunchIntentForPackage(p);
        if (i == null) { call.reject("Không mở được app này"); return; }
        VpLockStore.unlockedPkg = p;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(i);
        call.resolve();
    }

    private boolean tryStart(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Mở trang "Ẩn ứng dụng" của Samsung (One UI Home); không được thì mở Cài đặt màn hình chờ
    @PluginMethod
    public void openHideApps(PluginCall call) {
        JSObject r = new JSObject();
        String L = "com.sec.android.app.launcher";
        List<String> hide = new ArrayList<>(), sett = new ArrayList<>();
        try {
            PackageInfo pi = getContext().getPackageManager().getPackageInfo(L, PackageManager.GET_ACTIVITIES);
            if (pi.activities != null) for (ActivityInfo ai : pi.activities) {
                if (!ai.exported) continue;
                String n = ai.name.toLowerCase();
                if (n.contains("hide") || n.contains("hidden")) hide.add(ai.name);
                else if (n.contains("setting")) sett.add(ai.name);
            }
        } catch (Exception ignored) {}
        for (String n : hide) {
            if (tryStart(new Intent().setComponent(new ComponentName(L, n)))) { r.put("how", "hide"); call.resolve(r); return; }
        }
        for (String n : sett) {
            if (tryStart(new Intent().setComponent(new ComponentName(L, n)))) { r.put("how", "settings"); call.resolve(r); return; }
        }
        r.put("how", "none");
        call.resolve(r);
    }

    // Mở trang bật "Hỗ trợ tiếp cận" (để VP khoá được app khi mở)
    @PluginMethod
    public void openAccessibility(PluginCall call) {
        JSObject r = new JSObject();
        Intent d = new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS");
        d.putExtra(Intent.EXTRA_COMPONENT_NAME, new ComponentName(getContext(), VpLockService.class).flattenToString());
        r.put("ok", (Build.VERSION.SDK_INT >= 30 && tryStart(d)) || tryStart(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        call.resolve(r);
    }

    // Mở trang Thông tin ứng dụng của VP (để "Cho phép cài đặt bị hạn chế")
    @PluginMethod
    public void openAppInfo(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getContext().getPackageName()));
        JSObject r = new JSObject();
        r.put("ok", tryStart(i));
        call.resolve(r);
    }

    // Mở trang "Trình chặn tự động" (Auto Blocker) của Samsung; không được thì mở Bảo mật và quyền riêng tư
    @PluginMethod
    public void openAutoBlocker(PluginCall call) {
        JSObject r = new JSObject();
        String S = "com.android.settings";
        List<String> cands = new ArrayList<>();
        try {
            PackageInfo pi = getContext().getPackageManager().getPackageInfo(S, PackageManager.GET_ACTIVITIES);
            if (pi.activities != null) for (ActivityInfo ai : pi.activities) {
                if (!ai.exported) continue;
                String n = ai.name.toLowerCase();
                if (n.contains("autoblock") || n.contains("auto_block")) cands.add(ai.name);
            }
        } catch (Exception ignored) {}
        for (String n : cands) {
            if (tryStart(new Intent().setComponent(new ComponentName(S, n)))) { r.put("how", "direct"); call.resolve(r); return; }
        }
        if (tryStart(new Intent(Settings.ACTION_SECURITY_SETTINGS))) { r.put("how", "security"); call.resolve(r); return; }
        r.put("how", tryStart(new Intent(Settings.ACTION_SETTINGS)) ? "settings" : "none");
        call.resolve(r);
    }

    // Gỡ 1 app trong kho: máy hiện hộp hỏi, người dùng bấm OK mới gỡ. Trả về gone=true nếu app đã bị gỡ
    @PluginMethod
    public void uninstallApp(PluginCall call) {
        String pkg = call.getString("pkg", "");
        if (pkg.isEmpty() || !isInstalled(pkg)) { JSObject r = new JSObject(); r.put("gone", true); call.resolve(r); return; }
        Intent i = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + pkg));
        i.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        try { startActivityForResult(call, i, "uninstallDone"); }
        catch (Exception e) {
            try { getActivity().startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg))); } catch (Exception ignored) {}
            JSObject r = new JSObject(); r.put("gone", false); r.put("fallback", true); call.resolve(r);
        }
    }

    @ActivityCallback
    private void uninstallDone(PluginCall call, ActivityResult result) {
        if (call == null) return;
        JSObject r = new JSObject();
        r.put("gone", !isInstalled(call.getString("pkg", "")));
        call.resolve(r);
    }

    @PluginMethod
    public void isAppInstalled(PluginCall call) {
        JSObject r = new JSObject();
        r.put("installed", isInstalled(call.getString("pkg", "")));
        call.resolve(r);
    }

    // Gỡ chính VP (máy hỏi xác nhận)
    @PluginMethod
    public void uninstallSelf(PluginCall call) {
        JSObject r = new JSObject();
        try { getActivity().startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getContext().getPackageName()))); r.put("ok", true); }
        catch (Exception e) { r.put("ok", false); }
        call.resolve(r);
    }

    private boolean isInstalled(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        try { getContext().getPackageManager().getPackageInfo(pkg, 0); return true; }
        catch (Exception e) { return false; }
    }

    // ===== Sao lưu Kho riêng (bản 2.17): file Tải về/VP/vp-cai-dat.dat, còn lại sau khi gỡ VP =====
    static final String BK_NAME = "vp-cai-dat.dat";
    static final byte[] BK_KEY = "VP89kho-rieng".getBytes();

    private static byte[] xor(byte[] b) {
        byte[] o = new byte[b.length];
        for (int i = 0; i < b.length; i++) o[i] = (byte) (b[i] ^ BK_KEY[i % BK_KEY.length]);
        return o;
    }

    @PluginMethod
    public void backupKho(PluginCall call) {
        try {
            android.content.SharedPreferences sp = VpLockStore.p(getContext());
            org.json.JSONObject j = new org.json.JSONObject();
            j.put("v", 1);
            j.put("pin", sp.getString("pin", ""));
            j.put("salt", sp.getString("salt", ""));
            j.put("searchBlock", VpLockStore.searchBlock(getContext()));
            j.put("minChars", VpLockStore.minChars(getContext()));
            j.put("decoy", VpLockStore.decoy(getContext()));
            org.json.JSONArray a = new org.json.JSONArray();
            org.json.JSONObject kw = new org.json.JSONObject();
            for (String p : VpLockStore.locked(getContext())) { a.put(p); kw.put(p, VpLockStore.keywords(getContext(), p)); }
            j.put("locked", a);
            j.put("kw", kw);
            JSObject hid = call.getObject("hidden", new JSObject());
            j.put("hidden", hid == null ? new org.json.JSONObject() : hid);
            byte[] out = ("VPK1:" + Base64.encodeToString(xor(j.toString().getBytes("UTF-8")), Base64.NO_WRAP)).getBytes("UTF-8");
            boolean done = false;
            String u = sp.getString("bkUri", null);
            if (u != null) {
                try {
                    OutputStream os = getContext().getContentResolver().openOutputStream(Uri.parse(u), "wt");
                    os.write(out); os.close(); done = true;
                } catch (Exception ignored) {}
            }
            if (!done) {
                Uri nu = writeDownload(BK_NAME, out, "application/octet-stream");
                sp.edit().putString("bkUri", nu.toString()).apply();
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("Không sao lưu được: " + e.getMessage());
        }
    }

    // Mở trình chọn file ở Tải về/VP để chọn file sao lưu cũ
    @PluginMethod
    public void pickBackup(PluginCall call) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= 26) {
            i.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,
                Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FVP"));
        }
        try { startActivityForResult(call, i, "pickBackupDone"); }
        catch (Exception e) { JSObject r = new JSObject(); r.put("ok", false); r.put("msg", "Máy không mở được trình chọn file"); call.resolve(r); }
    }

    @ActivityCallback
    private void pickBackupDone(PluginCall call, ActivityResult result) {
        if (call == null) return;
        JSObject r = new JSObject();
        Intent d = result.getData();
        if (result.getResultCode() != android.app.Activity.RESULT_OK || d == null || d.getData() == null) {
            r.put("ok", false); r.put("cancel", true); call.resolve(r); return;
        }
        Uri uri = d.getData();
        try {
            java.io.InputStream in = getContext().getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            String txt = bo.toString("UTF-8").trim();
            if (!txt.startsWith("VPK1:")) throw new Exception("Đây không phải file sao lưu của VP");
            org.json.JSONObject j = new org.json.JSONObject(new String(xor(Base64.decode(txt.substring(5), Base64.NO_WRAP)), "UTF-8"));
            android.content.SharedPreferences.Editor e = VpLockStore.p(getContext()).edit();
            if (!j.optString("pin").isEmpty()) e.putString("pin", j.optString("pin")).putString("salt", j.optString("salt"));
            e.putBoolean("searchBlock", j.optBoolean("searchBlock", true));
            e.putInt("minChars", j.optInt("minChars", 2) == 1 ? 1 : 2);
            e.putString("decoy", j.optString("decoy", "Máy tính"));
            Set<String> locked = new HashSet<>();
            org.json.JSONArray a = j.optJSONArray("locked");
            org.json.JSONObject kw = j.optJSONObject("kw");
            if (a != null) for (int k = 0; k < a.length(); k++) {
                String p = a.getString(k);
                if (getContext().getPackageManager().getLaunchIntentForPackage(p) == null && !isInstalled(p)) continue;
                locked.add(p);
                if (kw != null) e.putString("kw:" + p, kw.optString(p, ""));
            }
            e.putStringSet("locked", locked).putInt("fails", 0).putLong("ver", System.nanoTime());
            try {
                getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                e.putString("bkUri", uri.toString());
            } catch (Exception ignored) {}
            e.apply();
            r.put("ok", true);
            r.put("count", locked.size());
            org.json.JSONObject h = j.optJSONObject("hidden");
            r.put("hidden", h == null ? "{}" : h.toString());
        } catch (Exception ex) {
            r.put("ok", false); r.put("msg", ex.getMessage());
        }
        call.resolve(r);
    }
}
