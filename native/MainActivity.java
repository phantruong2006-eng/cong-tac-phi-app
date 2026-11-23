package vn.truong.congtacphi;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    /** Số lượng nên đặt gửi từ app BH offline (bản 2.9), chờ trang Đặt hàng Bionote lấy. */
    static volatile String incoming = null;

    private static void grab(Intent i) {
        if (i != null && i.hasExtra("bionote")) {
            incoming = i.getStringExtra("bionote");
            i.removeExtra("bionote");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(VpNativePlugin.class);
        super.onCreate(savedInstanceState);
        grab(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        grab(intent);
        if (incoming != null && getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript("window.dispatchEvent(new Event('vpIncoming'))", null));
        }
    }
}
