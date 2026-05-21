/* ============================================================
   SUNILIES – admin.js
   ============================================================ */

// ── Toast ─────────────────────────────────────────────────────
function showToast(msg, type) {
    let t = document.getElementById('_adminToast');
    if (!t) {
        t = document.createElement('div');
        t.id = '_adminToast';
        t.className = 'adm-toast';
        const main = document.querySelector('.adm-grid > div:last-child') || document.body;
        main.insertBefore(t, main.firstChild);
    }
    t.textContent = msg;
    t.className = 'adm-toast ' + (type === 'error' ? 'adm-toast--err' : 'adm-toast--ok');
    t.style.display = 'block';
    clearTimeout(t._timer);
    t._timer = setTimeout(() => t.style.display = 'none', 4000);
}

// ── Tab switching ─────────────────────────────────────────────
function switchTab(name, btn) {
    ['stats','products','orders','hero','blogs'].forEach(t => {
        const el = document.getElementById('tab-' + t);
        if (el) el.style.display = t === name ? '' : 'none';
    });
    document.querySelectorAll('.adm-nav__item').forEach(b => b.classList.remove('is-active'));
    if (btn) btn.classList.add('is-active');
    history.replaceState(null, '', '/admin?tab=' + name);
}

// ── Toggle form thêm sản phẩm ─────────────────────────────────
function toggleAddForm() {
    const w = document.getElementById('add-form-wrap');
    if (!w) return;
    const open = w.style.display !== 'none' && w.style.display !== '';
    w.style.display = open ? 'none' : 'block';
    if (!open) w.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ── Tự sinh handle từ tên ─────────────────────────────────────
(function() {
    const n = document.getElementById('add-name');
    const h = document.getElementById('add-handle');
    if (!n || !h) return;
    n.addEventListener('input', function() {
        if (!h._t) h.value = this.value
            .toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'')
            .replace(/đ/g,'d').replace(/[^a-z0-9\s-]/g,'')
            .trim().replace(/\s+/g,'-');
    });
    h.addEventListener('input', () => h._t = true);
})();

// ── Lưu giá AJAX ─────────────────────────────────────────────
async function savePrice(id) {
    const price        = parseFloat(document.getElementById('price-' + id)?.value);
    const comparePrice = parseFloat(document.getElementById('cmp-'   + id)?.value) || null;
    if (isNaN(price) || price < 0) { showToast('Giá không hợp lệ!', 'error'); return; }
    try {
        const r = await fetch('/admin/products/' + id + '/price', {
            method:'POST', headers:{'Content-Type':'application/json'},
            body: JSON.stringify({ price, comparePrice })
        });
        const d = await r.json();
        d.success ? showToast('✅ Đã cập nhật giá!') : showToast('❌ ' + (d.error||'Lỗi'), 'error');
    } catch(e) { showToast('❌ ' + e.message, 'error'); }
}

// ── Lưu tồn kho AJAX ─────────────────────────────────────────
async function saveStock(id) {
    const stock = parseInt(document.getElementById('stock-' + id)?.value);
    if (isNaN(stock) || stock < 0) { showToast('Số lượng không hợp lệ!', 'error'); return; }
    try {
        const r = await fetch('/admin/products/' + id + '/stock', {
            method:'POST', headers:{'Content-Type':'application/json'},
            body: JSON.stringify({ stock })
        });
        const d = await r.json();
        if (d.success) {
            showToast('✅ Đã cập nhật tồn kho!');
            // Cập nhật màu dòng
            const row = document.getElementById('row-' + id);
            if (row) {
                row.classList.remove('adm-row--out','adm-row--low');
                if (d.stock <= 0) row.classList.add('adm-row--out');
                else if (d.stock <= 5) row.classList.add('adm-row--low');
            }
        } else { showToast('❌ ' + (d.error||'Lỗi'), 'error'); }
    } catch(e) { showToast('❌ ' + e.message, 'error'); }
}

// ── Toggle active AJAX ────────────────────────────────────────
async function toggleActive(id, checkbox) {
    try {
        const r = await fetch('/admin/products/' + id + '/toggle', { method:'POST' });
        const d = await r.json();
        if (d.success) {
            checkbox.checked = d.active;
            showToast(d.active ? '✅ Đã hiển thị!' : '⚪ Đã ẩn!');
        } else { checkbox.checked = !checkbox.checked; showToast('❌ ' + (d.error||''), 'error'); }
    } catch(e) { checkbox.checked = !checkbox.checked; showToast('❌ Lỗi kết nối', 'error'); }
}

// ── Modal sản phẩm ────────────────────────────────────────────
function openEditModal(id) {
    const p = PRODUCTS_DATA[id];
    if (!p) { showToast('❌ Không tìm thấy dữ liệu!', 'error'); return; }
    document.getElementById('editForm').action = '/admin/products/' + id + '/update';
    const s = (f, v) => { const el = document.getElementById(f); if(el) el.value = v ?? ''; };
    const c = (f, v) => { const el = document.getElementById(f); if(el) el.checked = !!v; };
    s('e-name',   p.name);    s('e-handle', p.handle);
    s('e-price',  p.price);   s('e-cmp',    p.comparePrice||'');
    s('e-stock',  p.stock);   s('e-cat',    p.category);
    s('e-col',    p.collection); s('e-img', p.imageUrl);
    const currentImgsStr = Array.isArray(p.images) ? p.images.join(', ') : (p.images||'');
    s('e-imgs',   currentImgsStr);
    if (currentImgsStr.trim()) {
        currentEditSubUrls = currentImgsStr.split(',').map(url => url.trim()).filter(url => url.length > 0);
    } else {
        currentEditSubUrls = [];
    }
    renderCurrentSubImagesPreview();
    s('e-desc',   p.description); s('e-fdesc', p.fullDescription);
    s('e-sizes',  Array.isArray(p.sizes)  ? p.sizes.join(', ')  : (p.sizes||''));
    s('e-colors', Array.isArray(p.colors) ? p.colors.join(', ') : (p.colors||''));
    s('e-tags',   Array.isArray(p.tags)   ? p.tags.join(', ')   : (p.tags||''));
    s('e-mt', p.metaTitle); s('e-md', p.metaDescription);
    c('e-active', p.active); c('e-onSale', p.onSale);
    c('e-isNew',  p['new'] !== undefined ? p['new'] : p.isNew);
    c('e-hot',    p.hot);

    // Reset file input và hiển thị ảnh hiện tại
    const fileInput = document.getElementById('e-imageFile');
    if (fileInput) fileInput.value = '';
    const imgEl    = document.getElementById('e-img-preview');
    const noneEl   = document.getElementById('e-img-none');
    const newPreviewWrap = document.getElementById('e-img-preview-new');
    if (newPreviewWrap) newPreviewWrap.style.display = 'none';
    if (p.imageUrl) {
        if (imgEl)  { imgEl.src = p.imageUrl; imgEl.style.display = 'block'; }
        if (noneEl) noneEl.style.display = 'none';
    } else {
        if (imgEl)  imgEl.style.display = 'none';
        if (noneEl) noneEl.style.display = 'inline';
    }

    // Reset danh sách ảnh phụ mới tải lên từ máy và preview của chúng
    selectedEditSubFiles = [];
    const filesInput = document.getElementById('e-imageFiles');
    if (filesInput) filesInput.value = '';
    const newSubPreviewWrap = document.getElementById('e-subimgs-preview-new');
    if (newSubPreviewWrap) newSubPreviewWrap.innerHTML = '';

    document.getElementById('editOverlay').classList.add('open');
    document.body.style.overflow = 'hidden';
}
function closeEditModal() {
    document.getElementById('editOverlay').classList.remove('open');
    document.body.style.overflow = '';
}

// ── Preview ảnh sản phẩm (modal edit) ────────────────────────
function previewProductImageEdit(input) {
    let wrap = document.getElementById('e-img-preview-new');
    if (!wrap) {
        wrap = document.createElement('div');
        wrap.id = 'e-img-preview-new';
        wrap.style.marginTop = '8px';
        wrap.innerHTML = '<img style="max-height:120px;border-radius:6px;object-fit:cover;" alt="Ảnh mới"/>';
        input.parentNode.insertBefore(wrap, input.nextSibling);
    }
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = e => {
            wrap.style.display = 'block';
            wrap.querySelector('img').src = e.target.result;
        };
        reader.readAsDataURL(input.files[0]);
    } else {
        wrap.style.display = 'none';
    }
}

// ── Khai báo các mảng trạng thái lưu ảnh phụ xem trước ──
let selectedAddSubFiles = [];
let selectedEditSubFiles = [];
let currentEditSubUrls = [];

// ── Preview & Xử lý nhiều ảnh phụ (form thêm mới) ───────────────────
function previewMultipleImages(input, previewId) {
    if (input.files && input.files.length > 0) {
        Array.from(input.files).forEach(file => {
            selectedAddSubFiles.push(file);
        });
    }
    renderAddSubImagesPreview(input, previewId);
}

function renderAddSubImagesPreview(input, previewId) {
    const wrap = document.getElementById(previewId);
    if (!wrap) return;
    wrap.innerHTML = '';

    // Đồng bộ lại mảng tệp tin thực tế vào input.files
    const dt = new DataTransfer();
    selectedAddSubFiles.forEach(file => dt.items.add(file));
    input.files = dt.files;

    selectedAddSubFiles.forEach((file, index) => {
        const reader = new FileReader();
        reader.onload = e => {
            const item = document.createElement('div');
            item.className = 'preview-item';

            const img = document.createElement('img');
            img.src = e.target.result;

            const btn = document.createElement('div');
            btn.className = 'btn-remove-preview';
            btn.innerHTML = '✕';
            btn.onclick = () => {
                selectedAddSubFiles.splice(index, 1);
                renderAddSubImagesPreview(input, previewId);
            };

            item.appendChild(img);
            item.appendChild(btn);
            wrap.appendChild(item);
        };
        reader.readAsDataURL(file);
    });
}

// ── Preview & Xử lý nhiều ảnh phụ mới (modal edit) ───────────────────────
function previewMultipleImagesEdit(input) {
    if (input.files && input.files.length > 0) {
        Array.from(input.files).forEach(file => {
            selectedEditSubFiles.push(file);
        });
    }
    renderEditSubImagesNewPreview(input);
}

function renderEditSubImagesNewPreview(input) {
    let wrap = document.getElementById('e-subimgs-preview-new');
    if (!wrap) {
        wrap = document.createElement('div');
        wrap.id = 'e-subimgs-preview-new';
        wrap.style.display = 'flex';
        wrap.style.flexWrap = 'wrap';
        wrap.style.gap = '8px';
        wrap.style.marginTop = '8px';
        input.parentNode.insertBefore(wrap, input.nextSibling);
    }
    wrap.innerHTML = '';

    // Đồng bộ lại mảng tệp tin thực tế vào input.files mới
    const dt = new DataTransfer();
    selectedEditSubFiles.forEach(file => dt.items.add(file));
    input.files = dt.files;

    selectedEditSubFiles.forEach((file, index) => {
        const reader = new FileReader();
        reader.onload = e => {
            const item = document.createElement('div');
            item.className = 'preview-item';

            const img = document.createElement('img');
            img.src = e.target.result;

            const btn = document.createElement('div');
            btn.className = 'btn-remove-preview';
            btn.innerHTML = '✕';
            btn.onclick = () => {
                selectedEditSubFiles.splice(index, 1);
                renderEditSubImagesNewPreview(input);
            };

            item.appendChild(img);
            item.appendChild(btn);
            wrap.appendChild(item);
        };
        reader.readAsDataURL(file);
    });
}

// ── Render ảnh phụ hiện tại (cũ từ DB) có nút xóa ──
function renderCurrentSubImagesPreview() {
    const wrap = document.getElementById('e-subimgs-preview-current');
    if (!wrap) return;
    wrap.innerHTML = '';

    // Cập nhật lại giá trị input chứa URL danh sách để gửi đi khi submit form
    const input = document.getElementById('e-imgs');
    if (input) {
        input.value = currentEditSubUrls.join(', ');
    }

    currentEditSubUrls.forEach((url, index) => {
        const item = document.createElement('div');
        item.className = 'preview-item';

        const img = document.createElement('img');
        img.src = url;
        img.onerror = () => { item.style.display = 'none'; }; // Ẩn item nếu URL ảnh lỗi

        const btn = document.createElement('div');
        btn.className = 'btn-remove-preview';
        btn.innerHTML = '✕';
        btn.onclick = () => {
            currentEditSubUrls.splice(index, 1);
            renderCurrentSubImagesPreview();
        };

        item.appendChild(img);
        item.appendChild(btn);
        wrap.appendChild(item);
    });
}

// ── Modal đơn hàng ────────────────────────────────────────────
function openOrderModal(id) {
    const o = ORDERS_DATA[id];
    if (!o) { showToast('❌ Không tìm thấy đơn!', 'error'); return; }
    const fmt = n => new Intl.NumberFormat('vi-VN').format(n) + '₫';
    const fmtDate = ts => ts ? new Date(ts).toLocaleString('vi-VN') : '—';
    const statusLabel = { PENDING:'Chờ thanh toán', PAID:'Đã thanh toán', FAILED:'Thất bại', CANCELLED:'Đã huỷ' };
    const statusCls   = { PENDING:'ord-status--PENDING', PAID:'ord-status--PAID', FAILED:'ord-status--FAILED', CANCELLED:'ord-status--CANCELLED' };

    const items = (o.items || []).map(it => `
        <div class="ord-item-row">
            <img src="${it.image || ''}" alt="${it.name || ''}" onerror="this.style.display='none'"/>
            <div class="name">${it.name || ''}
                <small>${it.size ? 'Size: '+it.size : ''} ${it.color ? '| Màu: '+it.color : ''}</small>
            </div>
            <div class="price">x${it.qty} = ${fmt((it.price||0)*(it.qty||1))}</div>
        </div>`).join('');

    document.getElementById('orderDetail').innerHTML = `
        <div class="ord-detail-grid">
            <div class="ord-detail-item"><label>Mã đơn</label><span>#${o.id}</span></div>
            <div class="ord-detail-item"><label>Trạng thái</label>
                <span class="ord-status ${statusCls[o.status]||''}">${statusLabel[o.status]||o.status}</span></div>
            <div class="ord-detail-item"><label>Khách hàng</label><span>${o.fullName||'—'}</span></div>
            <div class="ord-detail-item"><label>SĐT</label><span>${o.phone||'—'}</span></div>
            <div class="ord-detail-item" style="grid-column:1/-1"><label>Địa chỉ</label><span>${o.address||'—'}</span></div>
            <div class="ord-detail-item"><label>PT Thanh toán</label><span>${o.paymentMethod||'COD'}</span></div>
            <div class="ord-detail-item"><label>Tổng tiền</label><span style="font-weight:700;color:var(--mc)">${fmt(o.totalAmount||0)}</span></div>
            <div class="ord-detail-item"><label>Ngày đặt</label><span>${fmtDate(o.createdAt)}</span></div>
            <div class="ord-detail-item"><label>MoMo Trans</label><span>${o.momoTransId||'—'}</span></div>
            ${o.note ? `<div class="ord-detail-item" style="grid-column:1/-1"><label>Ghi chú</label><span>${o.note}</span></div>` : ''}
        </div>
        <div class="ord-items-list">${items}</div>`;

    document.getElementById('orderOverlay').classList.add('open');
    document.body.style.overflow = 'hidden';
}
function closeOrderModal() {
    document.getElementById('orderOverlay').classList.remove('open');
    document.body.style.overflow = '';
}

// ── Cập nhật trạng thái đơn hàng ─────────────────────────────
async function changeOrderStatus(id, status) {
    try {
        const r = await fetch('/admin/orders/' + id + '/status', {
            method:'POST', headers:{'Content-Type':'application/json'},
            body: JSON.stringify({ status })
        });
        const d = await r.json();
        if (d.success) {
            showToast('✅ Cập nhật trạng thái thành công!');
            const el = document.getElementById('status-' + id);
            if (el) {
                const labels = { PENDING:'Chờ thanh toán', PAID:'Đã thanh toán', FAILED:'Thất bại', CANCELLED:'Đã huỷ' };
                el.textContent = labels[status] || status;
                el.className = 'ord-status ord-status--' + status;
            }
            // Cập nhật ORDERS_DATA
            if (ORDERS_DATA[id]) ORDERS_DATA[id].status = status;
        } else { showToast('❌ ' + (d.error||'Lỗi'), 'error'); }
    } catch(e) { showToast('❌ ' + e.message, 'error'); }
}

// ── Lọc đơn hàng theo status ──────────────────────────────────
function filterOrders(status, btn) {
    document.querySelectorAll('#ordersTable tbody tr').forEach(row => {
        row.style.display = (status === 'ALL' || row.dataset.status === status) ? '' : 'none';
    });
    document.querySelectorAll('.ord-filter-btn').forEach(b => {
        b.className = b.dataset.filter === status
            ? 'adm-btn adm-btn--sm adm-btn--outline ord-filter-btn is-active'
            : 'adm-btn adm-btn--sm adm-btn--ghost ord-filter-btn';
    });
}

// ── Tìm kiếm sản phẩm ────────────────────────────────────────
function filterTable() {
    const q = document.getElementById('searchInput')?.value.toLowerCase().trim() || '';
    document.querySelectorAll('#productTable tbody tr').forEach(r => {
        r.style.display = r.textContent.toLowerCase().includes(q) ? '' : 'none';
    });
}

// ── Hero Slider ──────────────────────────────────────────────

function toggleHeroForm() {
    const w = document.getElementById('hero-form-wrap');
    if (!w) return;
    const open = w.style.display !== 'none' && w.style.display !== '';
    w.style.display = open ? 'none' : 'block';
    if (!open) w.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// Preview ảnh hero khi chọn file (dùng chung cho form thêm mới và modal edit)
function previewHeroImage(input, previewId) {
    const wrap = document.getElementById(previewId);
    if (!wrap) return;
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = e => {
            wrap.style.display = 'block';
            wrap.querySelector('img').src = e.target.result;
        };
        reader.readAsDataURL(input.files[0]);
    } else {
        wrap.style.display = 'none';
    }
}

// Toggle active qua AJAX
async function toggleHeroSlide(id, active, checkbox) {
    try {
        const r = await fetch('/admin/hero/' + id + '/toggle', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ active })
        });
        const d = await r.json();
        if (d.success) {
            showToast(active ? '🌟 Slide đang hiển thị!' : '🙈 Slide đã ẩn!');
        } else {
            checkbox.checked = !active;
            showToast('❌ ' + (d.error || 'Lỗi'), 'error');
        }
    } catch(e) {
        checkbox.checked = !active;
        showToast('❌ Lỗi kết nối', 'error');
    }
}

// Mở modal chỉnh sửa slide
function openHeroEditModal(id) {
    const s = HERO_DATA[id];
    if (!s) { showToast('❌ Không tìm thấy slide!', 'error'); return; }
    document.getElementById('heroEditForm').action = '/admin/hero/' + id + '/update';
    const set = (fid, val) => { const el = document.getElementById(fid); if (el) el.value = val ?? ''; };
    set('he-imageUrl',   s.imageUrl);
    set('he-subtitle',   s.subtitle);
    set('he-titleLine1', s.titleLine1);
    set('he-titleLine2', s.titleLine2);
    set('he-buttonText', s.buttonText);
    set('he-buttonLink', s.buttonLink);
    set('he-sortOrder',  s.sortOrder ?? 0);
    const active = document.getElementById('he-active');
    if (active) active.checked = !!s.active;
    // Reset file input và ẩn preview ảnh mới
    const fileInput = document.getElementById('he-imageFile');
    if (fileInput) fileInput.value = '';
    const newPreview = document.getElementById('he-new-preview');
    if (newPreview) newPreview.style.display = 'none';
    // Hiện ảnh hiện tại nếu có
    const wrap = document.getElementById('he-preview-wrap');
    const img  = document.getElementById('he-preview-img');
    if (wrap && img) {
        if (s.imageUrl) { img.src = s.imageUrl; wrap.style.display = 'block'; }
        else              wrap.style.display = 'none';
    }
    document.getElementById('heroEditOverlay').classList.add('open');
    document.body.style.overflow = 'hidden';
}
function closeHeroEditModal() {
    document.getElementById('heroEditOverlay').classList.remove('open');
    document.body.style.overflow = '';
}

// ── Toggle form thêm blog ─────────────────────────────────────
function toggleBlogForm() {
    const w = document.getElementById('blog-form-wrap');
    if (!w) return;
    const open = w.style.display !== 'none' && w.style.display !== '';
    w.style.display = open ? 'none' : 'block';
    if (!open) w.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ── Tự sinh slug từ tiêu đề blog ─────────────────────────────
(function() {
    const t = document.getElementById('blog-add-title');
    const s = document.getElementById('blog-add-slug');
    if (!t || !s) return;
    t.addEventListener('input', function() {
        if (!s._touched) s.value = this.value
            .toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
            .replace(/đ/g, 'd').replace(/[^a-z0-9\s-]/g, '')
            .trim().replace(/\s+/g, '-');
    });
    s.addEventListener('input', () => s._touched = true);
})();

// ── Toggle published blog AJAX ────────────────────────────────
async function toggleBlogPublished(id, checkbox) {
    try {
        const r = await fetch('/admin/blogs/' + id + '/toggle', { method: 'POST' });
        const d = await r.json();
        if (d.success) {
            checkbox.checked = d.published;
            showToast(d.published ? '🌐 Bài viết đã hiển thị!' : '📄 Đã chuyển thành bản nháp!');
        } else { checkbox.checked = !checkbox.checked; showToast('❌ ' + (d.error || ''), 'error'); }
    } catch(e) { checkbox.checked = !checkbox.checked; showToast('❌ Lỗi kết nối', 'error'); }
}

// ── Modal chỉnh sửa blog ──────────────────────────────────────
function openBlogEditModal(id) {
    const b = BLOGS_DATA[id];
    if (!b) { showToast('❌ Không tìm thấy dữ liệu!', 'error'); return; }
    document.getElementById('blogEditForm').action = '/admin/blogs/' + id + '/update';
    const s = (f, v) => { const el = document.getElementById(f); if (el) el.value = v ?? ''; };
    const c = (f, v) => { const el = document.getElementById(f); if (el) el.checked = !!v; };
    s('be-title',           b.title);
    s('be-slug',            b.slug);
    s('be-excerpt',         b.excerpt);
    s('be-content',         b.content);
    s('be-imageUrl',        b.imageUrl);
    s('be-readTime',        b.readTime || 5);
    s('be-metaTitle',       b.metaTitle);
    s('be-metaDescription', b.metaDescription);
    // Select category
    const catEl = document.getElementById('be-category');
    if (catEl) catEl.value = b.category || '';
    c('be-published', b.published);
    c('be-featured',  b.featured);
    // Reset file input và hiển thị ảnh hiện tại
    const fileInput = document.getElementById('be-imageFile');
    if (fileInput) fileInput.value = '';
    const imgEl    = document.getElementById('be-img-preview');
    const noneEl   = document.getElementById('be-img-none');
    const newPreviewWrap = document.getElementById('be-img-preview-new');
    if (newPreviewWrap) newPreviewWrap.style.display = 'none';
    if (b.imageUrl) {
        if (imgEl)  { imgEl.src = b.imageUrl; imgEl.style.display = 'block'; }
        if (noneEl) noneEl.style.display = 'none';
    } else {
        if (imgEl)  imgEl.style.display = 'none';
        if (noneEl) noneEl.style.display = 'inline';
    }
    document.getElementById('blogEditOverlay').classList.add('open');
    document.body.style.overflow = 'hidden';
}
function closeBlogEditModal() {
    document.getElementById('blogEditOverlay').classList.remove('open');
    document.body.style.overflow = '';
}

// ── Preview ảnh blog (form thêm mới) ─────────────────────────
function previewBlogImage(input, previewId) {
    const wrap = document.getElementById(previewId);
    if (!wrap) return;
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = e => {
            wrap.style.display = 'block';
            wrap.querySelector('img').src = e.target.result;
        };
        reader.readAsDataURL(input.files[0]);
    } else {
        wrap.style.display = 'none';
    }
}

// ── Preview ảnh blog (modal edit) ────────────────────────────
function previewBlogImageEdit(input) {
    let wrap = document.getElementById('be-img-preview-new');
    if (!wrap) {
        wrap = document.createElement('div');
        wrap.id = 'be-img-preview-new';
        wrap.style.marginTop = '8px';
        wrap.innerHTML = '<img style="max-height:120px;border-radius:6px;object-fit:cover;" alt="Ảnh mới"/>';
        input.parentNode.insertBefore(wrap, input.nextSibling);
    }
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = e => {
            wrap.style.display = 'block';
            wrap.querySelector('img').src = e.target.result;
        };
        reader.readAsDataURL(input.files[0]);
    } else {
        wrap.style.display = 'none';
    }
}

// ── Tìm kiếm blog ────────────────────────────────────────────
function filterBlogTable() {
    const q = document.getElementById('blogSearchInput')?.value.toLowerCase().trim() || '';
    document.querySelectorAll('#blogTable tbody tr').forEach(r => {
        r.style.display = r.textContent.toLowerCase().includes(q) ? '' : 'none';
    });
}

// ── Đóng modal bằng Escape ────────────────────────────────────
document.addEventListener('keydown', e => {
    if (e.key === 'Escape') { closeEditModal(); closeOrderModal(); closeBlogEditModal(); closeHeroEditModal(); }
});