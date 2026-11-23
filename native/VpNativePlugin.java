package vn.truong.congtacphi;

import android.content.ActivityNotFoundException;
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
    private Uri writeDownload(String name, byte[] bytes) throws Exception {
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
                v.put(MediaStore.MediaColumns.MIME_TYPE, MIME);
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
}
