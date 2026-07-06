/* ================================================================
   校园外卖平台 — 共享 Vue 3 工具模块
   使用方式: const { api, toast, useMap } = App;
   ================================================================ */

const App = (() => {
  const BASE = '';

  // ---- API 封装 ----
  const api = {
    getProducts:       ()           => fetch(BASE+'/api/products').then(r=>r.json()),
    getOrders:         (uid)        => fetch(BASE+'/api/orders?uid='+uid).then(r=>r.json()),
    createOrder:       (data)       => fetch(BASE+'/api/orders',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)}).then(r=>r.json()),
    getStoreOrders:    (sid)        => fetch(BASE+'/api/store/orders?sid='+sid).then(r=>r.json()),
    getStoreStats:     (sid)        => fetch(BASE+'/api/store/stats?sid='+sid).then(r=>r.json()),
    addProduct:        (data)       => fetch(BASE+'/api/products',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)}).then(r=>r.json()),
    updateProduct:     (id,data)    => fetch(BASE+'/api/products/'+id,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)}).then(r=>r.json()),
    deleteProduct:     (id)         => fetch(BASE+'/api/products/'+id,{method:'DELETE'}).then(r=>r.json()),
    getAvailable:      ()           => fetch(BASE+'/api/rider/available').then(r=>r.json()),
    acceptOrder:       (oid,rid)    => fetch(BASE+'/api/rider/accept',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({order_id:oid,rider_id:rid})}).then(r=>r.json()),
    updateStatus:      (oid,st)     => fetch(BASE+'/api/rider/status',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({order_id:oid,status:st})}).then(r=>r.json()),
    manualRefund:      (oid)        => fetch(BASE+'/api/store/orders/'+oid+'/manual-refund',{method:'POST'}).then(r=>r.json()),
    riderHistory:      (rid)        => fetch(BASE+'/api/rider/history?rid='+rid).then(r=>r.json()),
    chat:              (msg,uid)    => fetch(BASE+'/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:msg,user_id:uid})}).then(r=>r.json()),
  };

  // ---- Toast ----
  function toast(msg, ok=true) {
    const el = document.createElement('div');
    el.className = 'toast ' + (ok ? 'toast-success' : 'toast-error');
    el.textContent = msg;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 2600);
  }

  // ---- 状态徽章 ----
  function statusBadge(status) {
    const map = {'已下单':'warning','配送中':'info','已送达':'success','退款中':'danger','已发货':'info'};
    const cls = map[status] || 'info';
    return `<span class="badge badge-${cls}">${status}</span>`;
  }

  // ---- 商品 Emoji ----
  const EMOJI = {'黄焖鸡米饭':'🍛','麻辣烫':'🍲','珍珠奶茶':'🧋','炸鸡排':'🍗','蓝牙耳机':'🎧','机械键盘':'⌨️','可乐':'🥤'};
  function emoji(name) { return EMOJI[name] || '📦'; }

  // ---- Leaflet 地图 ----
  let lmap = null;
  function renderMap(containerId, data) {
    const mc = document.getElementById(containerId);
    if (!mc) return;
    // 先显示再操作，确保 Leaflet 能正确读取容器尺寸
    mc.classList.add('show'); mc.innerHTML = '';
    // 强制浏览器重排后再初始化地图
    requestAnimationFrame(() => {
      try {
        const wps = data.waypoints || [];
        const mlat = wps.length ? (Math.min(...wps.map(w=>w.lat)) + Math.max(...wps.map(w=>w.lat))) / 2 : 34.15;
        const mlng = wps.length ? (Math.min(...wps.map(w=>w.lng)) + Math.max(...wps.map(w=>w.lng))) / 2 : 108.85;
        // 复用已有地图实例或创建新的
        if (lmap) { lmap.remove(); lmap = null; }
        lmap = L.map(mc, { attributionControl: false }).setView([mlat, mlng], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {attribution:'&copy; OSM', maxZoom:19}).addTo(lmap);
        if (wps.length) L.polyline(wps.map(w=>[w.lat,w.lng]), {color:'#4f6ef7',weight:5,opacity:.7}).addTo(lmap);
        if (data.store) L.marker([data.store.lat,data.store.lng], {icon:L.divIcon({html:'🏪',iconSize:[28,28]})}).addTo(lmap).bindPopup(data.store.label);
        if (data.destination) L.marker([data.destination.lat,data.destination.lng], {icon:L.divIcon({html:'📍',iconSize:[28,28]})}).addTo(lmap).bindPopup(data.destination.label);
        if (data.rider?.lat) L.marker([data.rider.lat,data.rider.lng], {icon:L.divIcon({html:'🛵',iconSize:[30,30]})}).addTo(lmap).bindPopup(data.rider.name||'骑手');
        if (wps.length) lmap.fitBounds(wps.map(w=>[w.lat,w.lng]), {padding:[30,30]});
        // 关键：通知 Leaflet 容器尺寸已变化
        setTimeout(() => { if (lmap) lmap.invalidateSize(); }, 100);
      } catch(e) { console.error('地图渲染失败:', e); }
    });
  }

  return { api, toast, statusBadge, emoji, renderMap };
})();
