package vn.truong.congtacphi;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
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

    @PluginMethod
    public void saveToDownloads(PluginCall call) {
        String name = call.getString("name");
        String data = call.getString("data");
        if (name == null || data == null) { call.reject("Thiếu dữ liệu"); return; }
        try {
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, MIME);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/VP");
                Uri uri = getContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new Exception("Không tạo được file trong Download");
                OutputStream os = getContext().getContentResolver().openOutputStream(uri);
                os.write(bytes);
                os.close();
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VP");
                dir.mkdirs();
                FileOutputStream f = new FileOutputStream(new File(dir, name));
                f.write(bytes);
                f.close();
            }
            JSObject r = new JSObject();
            r.put("path", "Download/VP/" + name);
            call.resolve(r);
        } catch (Exception e) {
            call.reject("Không lưu được: " + e.getMessage());
        }
    }

    @PluginMethod
    public void shareZalo(PluginCall call) {
        String name = call.getString("name");
        String data = call.getString("data");
        String text = call.getString("text", "");
        if (name == null || data == null) { call.reject("Thiếu dữ liệu"); return; }
        try {
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            File dir = new File(getContext().getCacheDir(), "share");
            dir.mkdirs();
            File f = new File(dir, name);
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes);
            fo.close();
            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(MIME);
            i.putExtra(Intent.EXTRA_STREAM, uri);
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
