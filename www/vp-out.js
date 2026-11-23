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
      // Zalo tự gắn mã vào tên file khi nhận "Chia sẻ" → lưu file, hiện hướng dẫn, anh chọn cách gửi
      await VP.saveToDownloads({name, data:b64});
      const how = await window.vpZaloGuide(name);
      if (how === 'cancel') return 'Đã lưu file vào Download/VP (chưa gửi).';
      if (how === 'fast'){
        const r = await VP.shareZaloFile({name, text:text||''});
        return r && r.zalo ? 'Đã gửi nhanh sang Zalo — anh xem tên file có đúng không.' : 'Máy chưa cài Zalo.';
      }
      const r = await VP.openZalo();
      return r && r.zalo ? 'Đã lưu file và mở Zalo — bấm ••• → Tài liệu → Download → VP để gửi.' : 'Đã lưu file vào Download/VP, nhưng máy chưa cài Zalo.';
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

// ===== Bảng hướng dẫn gửi Zalo (giữ đúng tên file) =====
window.vpZaloGuide = function(name){
  return new Promise(res=>{
    const ov=document.createElement('div');
    ov.style.cssText='position:fixed;inset:0;background:#0007;z-index:1001;display:flex;align-items:flex-end;justify-content:center';
    const st='display:flex;gap:10px;align-items:flex-start;margin:8px 0;font-size:15px;line-height:1.35';
    const n=k=>`<span style="flex:none;width:24px;height:24px;border-radius:50%;background:#0068ff;color:#fff;font-weight:700;font-size:13px;display:flex;align-items:center;justify-content:center">${k}</span>`;
    ov.innerHTML=`<div style="background:#fff;width:100%;max-width:560px;border-radius:16px 16px 0 0;padding:18px 16px calc(16px + env(safe-area-inset-bottom));color:#1d2433;font-family:inherit">
      <div style="font-size:13px;color:#2e7d32;font-weight:600">✔ Đã lưu file vào Download/VP</div>
      <div style="font-weight:700;font-size:15px;margin:4px 0 12px;word-break:break-word">${String(name).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]))}</div>
      <div style="font-weight:700;margin-bottom:4px">Gửi trong Zalo để giữ đúng tên file:</div>
      <div style="${st}">${n(1)}<span>Mở khung chat với <b>kế toán</b></span></div>
      <div style="${st}">${n(2)}<span>Bấm <b>•••</b> (cạnh nút micro) → <b>Tài liệu</b></span></div>
      <div style="${st}">${n(3)}<span>Vào <b>Download → VP</b> → chọn file <b>trên cùng</b> → Gửi</span></div>
      <button data-a="open" style="width:100%;margin-top:12px;background:#0068ff;color:#fff;border:0;border-radius:12px;padding:14px;font-size:16px;font-weight:700">Mở Zalo</button>
      <div style="display:flex;gap:10px;margin-top:10px">
        <button data-a="fast" style="flex:1;background:#fff;color:#1f3864;border:2px solid #1f3864;border-radius:12px;padding:10px;font-size:13px">⚡ Gửi nhanh 1 chạm<br><span style="font-size:11px;opacity:.7">(thử nghiệm)</span></button>
        <button data-a="cancel" style="flex:1;background:#eef1f7;color:#1f3864;border:0;border-radius:12px;padding:10px;font-size:14px">Để sau</button>
      </div></div>`;
    document.body.appendChild(ov);
    ov.onclick=e=>{ const b=e.target.closest('button[data-a]'); const a=b?b.dataset.a:(e.target===ov?'cancel':null); if(!a) return; ov.remove(); res(a); };
  });
};


// ===== Nhận số lượng nên đặt từ app BH offline (bản 2.9) =====
window.vpIncoming = async function(take){
  const cap = window.Capacitor;
  if (!(cap && cap.isNativePlatform && cap.isNativePlatform())) return '';
  try{ const VP = cap.registerPlugin ? cap.registerPlugin('VpNative') : cap.Plugins.VpNative; const r = await VP.incoming({take: !!take}); return (r && r.data) || ''; }catch(e){ return ''; }
};
(function(){
  const page = location.pathname.split('/').pop() || 'ctp.html';
  let busy = false;
  async function check(){
    if (busy) return; busy = true;
    try{
      const d = await window.vpIncoming(false);
      if (!d) return;
      if (page !== 'bionote.html'){ location.replace('bionote.html'); return; }
      if (window.vpApplyIncoming){ const t = await window.vpIncoming(true); if (t) window.vpApplyIncoming(t); }
    } finally { busy = false; }
  }
  window.addEventListener('vpIncoming', check);
  setTimeout(check, 400);
})();
