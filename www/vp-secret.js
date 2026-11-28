// Lối vào ẩn của Kho riêng (bản 2.12): bấm 5 lần liền vào "VP · bản …" → gõ mã 89 → vào Kho riêng.
(function(){
  const h = document.querySelector('h1'); if (!h) return;
  h.style.userSelect = 'none'; h.style.webkitUserSelect = 'none';
  let n = 0, t = 0;
  h.addEventListener('click', () => {
    const now = Date.now();
    n = (now - t < 700) ? n + 1 : 1; t = now;
    if (n >= 5){ n = 0; ask(); }
  });
  function ask(){
    const m = document.createElement('div');
    m.style.cssText = 'position:fixed;inset:0;background:#0007;display:flex;align-items:center;justify-content:center;z-index:99';
    m.innerHTML = '<div style="background:var(--card,#fff);color:var(--ink,#1c2430);border-radius:14px;padding:16px;width:220px">'
      + '<input type="password" inputmode="numeric" maxlength="6" autocomplete="off" style="font:inherit;font-size:26px;letter-spacing:8px;text-align:center;width:100%;padding:8px;border:1px solid #d9dfe7;border-radius:10px;background:transparent;color:inherit">'
      + '<div style="display:flex;gap:8px;margin-top:10px"><button type="button" data-x style="flex:1;font:inherit;font-weight:600;border:1px solid #1f6feb;background:transparent;color:#1f6feb;border-radius:10px;padding:9px">Huỷ</button>'
      + '<button type="button" data-ok style="flex:1;font:inherit;font-weight:600;border:0;background:#1f6feb;color:#fff;border-radius:10px;padding:9px">OK</button></div></div>';
    document.body.appendChild(m);
    const i = m.querySelector('input');
    const close = () => m.remove();
    const ok = () => { if (i.value === '89') location.href = 'kho.html'; else close(); };
    m.querySelector('[data-x]').onclick = close;
    m.querySelector('[data-ok]').onclick = ok;
    i.addEventListener('keydown', e => { if (e.key === 'Enter') ok(); });
    m.addEventListener('click', e => { if (e.target === m) close(); });
    setTimeout(() => i.focus(), 50);
  }
})();
