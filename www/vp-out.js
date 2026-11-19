// Xuất file: "save" = lưu vào máy (Download/VP), "zalo" = mở Zalo kèm file
window.vpOut = async function(name, b64, action, text){
  const cap = window.Capacitor;
  const native = cap && cap.isNativePlatform && cap.isNativePlatform();
  if (native){
    const VP = cap.registerPlugin ? cap.registerPlugin('VpNative') : (cap.Plugins && cap.Plugins.VpNative);
    if (action === 'save'){
      const r = await VP.saveToDownloads({name, data:b64});
      return `Đã lưu vào máy: ${r.path} (mở app "Tệp của tôi" → Tải về → VP).`;
    } else {
      const r = await VP.shareZalo({name, data:b64, text:text||''});
      return r && r.zalo ? 'Đã mở Zalo — chọn người kế toán rồi bấm gửi.' : 'Máy chưa cài Zalo, anh chọn ứng dụng khác để gửi.';
    }
  }
  const bin = atob(b64), u = new Uint8Array(bin.length); for (let i=0;i<bin.length;i++) u[i]=bin.charCodeAt(i);
  const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([u],{type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'})); a.download = name;
  document.body.appendChild(a); a.click(); a.remove();
  return 'Đã tải file: ' + name;
};
