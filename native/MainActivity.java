package vn.truong.congtacphi;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(VpNativePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
