# Thêm vào AndroidManifest.xml: quyền xem danh sách app, màn hình khoá, dịch vụ "Hỗ trợ tiếp cận" (bản 2.11)
import sys
p = sys.argv[1]
s = open(p, encoding="utf-8").read()
queries = """
    <uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES" />
    <queries>
        <intent><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.LAUNCHER" /></intent>
        <intent><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.HOME" /></intent>
        <package android:name="com.sec.android.app.launcher" />
        <package android:name="com.android.settings" />
    </queries>
"""
app = """
        <activity android:name=".LockActivity" android:exported="false" android:launchMode="singleTask"
            android:taskAffinity="vn.truong.congtacphi.lock" android:excludeFromRecents="true"
            android:noHistory="false" android:theme="@android:style/Theme.Material.NoActionBar" />
        <activity android:name=".VaultLockActivity" android:exported="false" android:excludeFromRecents="true"
            android:theme="@android:style/Theme.Material.NoActionBar" />
        <service android:name=".VpLockService" android:exported="false" android:label="@string/vp_lock_label"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter><action android:name="android.accessibilityservice.AccessibilityService" /></intent-filter>
            <meta-data android:name="android.accessibilityservice" android:resource="@xml/vp_lock_service" />
        </service>
"""
assert "<application" in s and "</application>" in s
if "VpLockService" not in s:
    s = s.replace("<application", queries + "    <application", 1)
    s = s.replace("</application>", app + "    </application>", 1)
open(p, "w", encoding="utf-8").write(s)
print(s)
