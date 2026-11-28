package vn.truong.congtacphi;

/** Màn hình khoá khi mở Kho riêng trong VP (trả kết quả về trang Kho riêng). */
public class VaultLockActivity extends LockActivity {
    @Override
    protected boolean isVault() { return true; }
}
