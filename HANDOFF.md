# App VP – Tóm tắt để làm tiếp (bản hiện tại: 2.0, ngày 19/11/2026)

## App là gì
- App Android tên **VP** (biểu tượng tờ giấy trắng trên nền xanh), cài trên Samsung S25 Ultra của anh Trường.
- Gồm 2 tool, chuyển qua lại bằng nút ở **góc trên bên phải**. Mở app vào thẳng tool dùng lần trước.
- Mục đích duy nhất: nhập số → **xuất file Excel** → **Lưu vào máy** (Tải về/VP) hoặc **Gửi qua Zalo** cho kế toán. Không dùng Gmail.

## Tool 1 – Công tác phí
- File mẫu có sẵn trong app: "Công tác phí T10.2026 - Phan Đình Trường.xlsx" (bảng bắt đầu ô A11, cột A ngày, C tiền ăn 150.000đ, H = C, I số thứ tự, dòng Tổng cộng tự cộng).
- Mở app tự chọn **tháng trước** (làm tháng 11 → báo cáo tháng 10). Chỉ cần gõ **số ngày** (có nút nhanh 18/20/22/24, gõ tay được).
- Chọn ngày ngẫu nhiên, **bỏ Chủ nhật và ngày lễ/Tết**, thứ Bảy vẫn tính. Số ngày khác số dòng thì tự thêm/bớt dòng, tổng tiền tự cộng.
- Tự đổi: tên file (T10.2026…), ô "Ngày: 31/10/2026" thành ngày cuối tháng báo cáo. Hiện "Tổng tiền" trước khi gửi.
- Lịch nghỉ lễ tự tính đến **2046** (âm lịch thuật toán Hồ Ngọc Đức): Tết 5 ngày, Giỗ Tổ 10/3 âm, 1/1, 30/4, 1/5, 2/9 + 1 ngày liền kề, **24/11 Ngày Văn hóa VN (từ 2026)**, nghỉ bù khi trùng T7/CN. Lịch chính thức đã nhập: 2026, 2027 (Tết 4–10/2/2027, Quốc khánh 2–3/9/2027).
- Mục Cài đặt: xem ngày nghỉ của năm, thêm ngày nghỉ riêng, bỏ ngày (gõ -dd/mm/yyyy), dùng file Excel khác.

## Tool 2 – Đặt hàng Bionote
- Theo file gốc TOOL_DAT_HANG_BIONOTE.html của anh: 64 mã (Vcheck thú cưng, Livestock, Khác), 14 khách (Phạm Bằng, Chợ Lớn, Pet Health, Vet Mart, An Việt, Pet Vet, Pro Care, K9, Forest, JVG, Belweel, Tên Lửa, ThongLao, Home Pet Care).
- Gõ "SL đặt" → tự chia ngẫu nhiên cho khách (mặc định ~50% khách/mã, tổng luôn đúng), xuất Excel từ mẫu gốc (sheet "MOQ 2025" + "Trang_tính1"), tên file ĐỀ_XUẤT_ĐẶT_HÀNG_BIONOTE_dd_mm_yyyy.xlsx.
- Có: Xoá hết số, Số đơn mẫu, 🎲 Chia lại, Chỉ hiện mã đã nhập, tự nhớ số đã nhập, thanh dưới cùng "X mã · Y SL" + nút Lưu / Gửi Zalo. Trên điện thoại ẩn cột Mã ERP.

## Kỹ thuật (để làm tiếp)
- Code: GitHub **phantruong2006-eng/cong-tac-phi-app** (đang Public – nên chuyển Private).
- Cấu trúc: www/ctp.html, www/bionote.html, www/index.html (chuyển vào tool lần trước), www/vp-out.js (Lưu/Zalo), www/holidays.js (lịch lễ), www/template.xlsx; native/ (mã Android: VpNativePlugin.java – lưu MediaStore Download/VP và mở Zalo com.zing.zalo; MainActivity.java; file_paths.xml); icons/ (biểu tượng); keystore/ctp-debug.keystore (mã khóa cố định); VERSION; CHANGELOG.md.
- Capacitor 6, appId vn.truong.congtacphi (KHÔNG đổi, để cài đè được).
- GitHub Actions (.github/workflows/build-apk.yml) tự build khi push lên main → Release tag vX.Y, file CTP-X.Y.apk (mở ra tên app vẫn là VP). Ký bằng mã khóa cố định từ 1.5 → **cài đè, không cần gỡ**.
- Máy Claude không build Android được (bị chặn tải SDK) → luôn build qua GitHub Actions rồi tải APK gửi anh.

## Quy tắc anh dặn
- **Mỗi lần sửa tăng phiên bản: 2.0 → 2.1 → 2.2 → 2.3…** (sửa file VERSION + ghi CHANGELOG.md). Số phiên bản hiện trong app và tên file.
- Luôn làm demo/ảnh giao diện cho anh xem trước khi làm thay đổi giao diện lớn.
- Xong việc báo cáo ngắn gọn những gì đã làm + đề xuất tối ưu.
- Chỉ xuất Excel, chỉ gửi qua Zalo (không Gmail).

## Việc có thể làm tiếp (đề xuất, chưa làm)
1. Lịch sử file đã xuất (gửi lại nhanh khi kế toán hỏi).
2. Nhắc việc ngày 1 hằng tháng "Làm công tác phí tháng trước".
3. Công tác phí: đánh dấu trước ngày chắc chắn đi; thêm chi phí khác (xăng, gửi xe).
4. Cập nhật lịch nghỉ chính thức từ 2028 khi Nhà nước công bố.
5. Khi có tool thứ 3 trở lên: đổi nút góc phải thành thanh tab dưới đáy.
6. Chuyển repo GitHub sang Private.
7. Chờ anh thử thực tế trên điện thoại: nút Lưu vào máy và Gửi qua Zalo (mã Android mới ở bản 2.0).
