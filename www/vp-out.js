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

// ===== Lịch sử file đã xuất (lưu trong máy, tối đa 15 file gần nhất) =====
(function(){
  const KEY='vp_hist', MAX=15;
  const load=()=>{ try{return JSON.parse(localStorage.getItem(KEY)||'[]')}catch(e){return []} };
  const store=list=>{ while(list.length){ try{ localStorage.setItem(KEY,JSON.stringify(list)); return; }catch(e){ list.pop(); } } try{localStorage.removeItem(KEY)}catch(e){} };
  const raw=window.vpOut;
  window.vpOut=async function(name,b64,action,text,noLog){
    const r=await raw(name,b64,action,text);
    if(!noLog){
      const list=load().filter(x=>x.name!==name);
      list.unshift({name,b64,text:text||'',t:Date.now(),a:action});
      store(list.slice(0,MAX));
    }
    return r;
  };
  const esc=s=>String(s).replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
  window.vpHistory=function(){
    let ov=document.getElementById('vpHist'); if(ov) ov.remove();
    ov=document.createElement('div'); ov.id='vpHist';
    ov.style.cssText='position:fixed;inset:0;background:#0007;z-index:1000;display:flex;align-items:flex-end;justify-content:center';
    const list=load(), p=n=>String(n).padStart(2,'0');
    const rows=list.length? list.map((x,i)=>{ const d=new Date(x.t);
      return `<div style="padding:10px 0;border-bottom:1px solid #e3e7ef"><div style="font-weight:600;font-size:14px;word-break:break-word">${esc(x.name)}</div>
      <div style="font-size:12px;color:#6b7385;margin:2px 0 8px">${p(d.getHours())}:${p(d.getMinutes())} ngày ${p(d.getDate())}/${p(d.getMonth()+1)}/${d.getFullYear()} · ${x.a==='zalo'?'đã gửi Zalo':'đã lưu vào máy'}</div>
      <div style="display:flex;gap:8px"><button data-i="${i}" data-a="zalo" style="flex:1;background:#0068ff;color:#fff;border:0;border-radius:10px;padding:10px;font-size:14px">Gửi lại Zalo</button>
      <button data-i="${i}" data-a="save" style="flex:1;background:#fff;color:#1f3864;border:2px solid #1f3864;border-radius:10px;padding:8px;font-size:14px">💾 Lưu</button>
      <button data-i="${i}" data-a="del" style="background:#fde2e1;color:#9b1c1c;border:0;border-radius:10px;padding:10px 12px;font-size:14px">✕</button></div></div>`; }).join('')
      : '<p style="color:#6b7385;text-align:center;padding:20px 0">Chưa có file nào. File anh Lưu hoặc Gửi Zalo sẽ hiện ở đây.</p>';
    ov.innerHTML=`<div style="background:#fff;width:100%;max-width:560px;max-height:85vh;overflow:auto;border-radius:16px 16px 0 0;padding:16px 16px calc(16px + env(safe-area-inset-bottom));font-family:inherit;color:#1d2433">
      <div style="display:flex;align-items:center;margin-bottom:4px"><b style="flex:1;font-size:17px">🕘 Lịch sử file đã gửi</b><button id="vpHistX" style="background:#eef1f7;color:#1f3864;border:0;border-radius:10px;padding:8px 14px;font-size:14px">Đóng</button></div>
      <div style="font-size:12px;color:#6b7385">Giữ ${MAX} file gần nhất của cả 2 tool.</div>${rows}<p id="vpHistMsg" style="font-size:13px;color:#2e7d32"></p></div>`;
    document.body.appendChild(ov);
    ov.onclick=async e=>{
      if(e.target===ov||e.target.id==='vpHistX'){ ov.remove(); return; }
      const b=e.target.closest('button[data-i]'); if(!b) return;
      const x=load()[+b.dataset.i]; if(!x) return;
      if(b.dataset.a==='del'){ if(!confirm('Xoá "'+x.name+'" khỏi lịch sử?')) return; store(load().filter(y=>y.t!==x.t)); window.vpHistory(); return; }
      try{ const m=await window.vpOut(x.name,x.b64,b.dataset.a,x.text,true); document.getElementById('vpHistMsg').textContent='✔ '+m; }
      catch(err){ alert('Lỗi: '+(err&&(err.message||err))); }
    };
  };
})();
