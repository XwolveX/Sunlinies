/* ============================================================
   SUNILIES – admin.js
   Xử lý: cập nhật giá/kho AJAX, modal chỉnh sửa, lọc bảng
   ============================================================ */

// ══════════════════════════════════════════════════════
// HELPER: hiện / ẩn toast ngắn
// ══════════════════════════════════════════════════════
function showToast(msg, type = 'success') {
    let toast = document.getElementById('adminToast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'adminToast';
        toast.className = 'adm-toast';
        document.querySelector('.adm-main').prepend(toast);
    }
    toast.textContent = msg;
    toast.className = `adm-toast adm-toast--${type}`;
    toast.style.display = 'block';
    clearTimeout(toast._timer);
    toast._timer = setTimeout(() => toast.style.display = 'none', 4000);
}

// ══════════════════════════════════════════════════════
// 1. LƯU GIÁ NHANH (AJAX)
// ══════════════════════════════════════════════════════
async function savePrice(id) {
    const price        = parseFloat(document.getElementById('price-' + id)?.value);
    const comparePrice = parseFloat(document.getElementById('cmp-'   + id)?.value) || null;

    if (isNaN(price) || price < 0) { showToast('❌ Giá không hợp lệ!', 'error'); return; }

    try {
        const res  = await fetch(`/admin/products/${id}/price`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ price, comparePrice })
        });
        const data = await res.json();
        if (data.success) {
            showToast('✅ Cập nhật giá thành công!');
        } else {
            showToast('❌ ' + (data.error || 'Lỗi không xác định'), 'error');
        }
    } catch (e) {
        showToast('❌ Lỗi kết nối: ' + e.message, 'error');
    }
}

// ══════════════════════════════════════════════════════
// 2. LƯU TỒN KHO NHANH (AJAX)
// ══════════════════════════════════════════════════════
async function saveStock(id) {
    const stock = parseInt(document.getElementById('stock-' + id)?.value);
    if (isNaN(stock) || stock < 0) { showToast('❌ Số lượng không hợp lệ!', 'error'); return; }

    try {
        const res  = await fetch(`/admin/products/${id}/stock`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ stock })
        });
        const data = await res.json();
        if (data.success) {
            showToast('✅ Cập nhật tồn kho thành công!');
            // Cập nhật badge cảnh báo
            const row = document.getElementById('row-' + id);
            if (row) {
                row.classList.remove('adm-row--out', 'adm-row--low');
                const badgeWrap = document.getElementById('stock-' + id)?.closest('.adm-inline-edit')?.parentElement;
                if (badgeWrap) {
                    ['adm-badge--out', 'adm-badge--low'].forEach(c => {
                        badgeWrap.querySelectorAll('.' + c).forEach(el => el.remove());
                    });
                }
                if (data.stock <= 0) {
                    row.classList.add('adm-row--out');
                    const badge = document.createElement('span');
                    badge.className = 'adm-badge adm-badge--out';
                    badge.textContent = 'Hết';
                    document.getElementById('stock-' + id)?.closest('.adm-inline-edit')?.parentElement?.appendChild(badge);
                } else if (data.stock <= 5) {
                    row.classList.add('adm-row--low');
                    const badge = document.createElement('span');
                    badge.className = 'adm-badge adm-badge--low';
                    badge.textContent = 'Sắp hết';
                    document.getElementById('stock-' + id)?.closest('.adm-inline-edit')?.parentElement?.appendChild(badge);
                }
            }
        } else {
            showToast('❌ ' + (data.error || 'Lỗi không xác định'), 'error');
        }
    } catch (e) {
        showToast('❌ Lỗi kết nối: ' + e.message, 'error');
    }
}

// ══════════════════════════════════════════════════════
// 3. TOGGLE ACTIVE (AJAX)
// ══════════════════════════════════════════════════════
async function toggleActive(id, checkbox) {
    try {
        const res  = await fetch(`/admin/products/${id}/toggle`, { method: 'POST' });
        const data = await res.json();
        if (data.success) {
            checkbox.checked = data.active;
            showToast(data.active ? '✅ Sản phẩm đã hiển thị!' : '⚪ Sản phẩm đã ẩn!');
        } else {
            checkbox.checked = !checkbox.checked; // revert
            showToast('❌ ' + (data.error || 'Lỗi không xác định'), 'error');
        }
    } catch (e) {
        checkbox.checked = !checkbox.checked;
        showToast('❌ Lỗi kết nối', 'error');
    }
}

// ══════════════════════════════════════════════════════
// 4. TOGGLE FORM THÊM SẢN PHẨM
// ══════════════════════════════════════════════════════
function toggleAddForm() {
    const wrapper = document.getElementById('add-form-wrapper');
    if (!wrapper) return;
    const isHidden = wrapper.style.display === 'none' || wrapper.style.display === '';
    wrapper.style.display = isHidden ? 'block' : 'none';
    if (isHidden) wrapper.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ══════════════════════════════════════════════════════
// 5. MODAL CHỈNH SỬA SẢN PHẨM
// ══════════════════════════════════════════════════════
function openEditModal(id) {
    const p = PRODUCTS_DATA[id];
    if (!p) { showToast('❌ Không tìm thấy dữ liệu sản phẩm!', 'error'); return; }

    const form = document.getElementById('editForm');
    form.action = `/admin/products/${id}/update`;

    // Điền dữ liệu vào form
    const set = (fieldId, val) => {
        const el = document.getElementById(fieldId);
        if (el) el.value = val ?? '';
    };
    const setChk = (fieldId, val) => {
        const el = document.getElementById(fieldId);
        if (el) el.checked = !!val;
    };

    set('edit-name',            p.name);
    set('edit-handle',          p.handle);
    set('edit-description',     p.description);
    set('edit-fullDescription', p.fullDescription);
    set('edit-price',           p.price);
    set('edit-comparePrice',    p.comparePrice || '');
    set('edit-imageUrl',        p.imageUrl);
    set('edit-images',          p.images);
    set('edit-category',        p.category);
    set('edit-collection',      p.collection);
    set('edit-sizes',           p.sizes);
    set('edit-colors',          p.colors);
    set('edit-tags',            p.tags);
    set('edit-stock',           p.stock);
    set('edit-metaTitle',       p.metaTitle);
    set('edit-metaDescription', p.metaDescription);

    setChk('edit-active',  p.active);
    setChk('edit-onSale',  p.onSale);
    setChk('edit-isNew',   p.isNew);
    setChk('edit-hot',     p.hot);

    document.getElementById('editModalOverlay').classList.add('is-open');
    document.body.style.overflow = 'hidden';
}

function closeEditModal() {
    document.getElementById('editModalOverlay').classList.remove('is-open');
    document.body.style.overflow = '';
}

// Đóng modal khi nhấn Escape
document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeEditModal();
});

// ══════════════════════════════════════════════════════
// 6. LỌC BẢNG SẢN PHẨM
// ══════════════════════════════════════════════════════
function filterTable() {
    const q = document.getElementById('searchInput')?.value.toLowerCase().trim() || '';
    document.querySelectorAll('#productTable tbody tr').forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(q) ? '' : 'none';
    });
}

// ══════════════════════════════════════════════════════
// 7. SMOOTH SCROLL khi click nav
// ══════════════════════════════════════════════════════
document.querySelectorAll('.adm-nav__link').forEach(link => {
    link.addEventListener('click', function (e) {
        const href = this.getAttribute('href');
        if (href && href.startsWith('#')) {
            e.preventDefault();
            document.querySelector(href)?.scrollIntoView({ behavior: 'smooth' });
            document.querySelectorAll('.adm-nav__link').forEach(l => l.classList.remove('adm-nav__link--active'));
            this.classList.add('adm-nav__link--active');
        }
    });
});

// ══════════════════════════════════════════════════════
// 8. TỰ SINH HANDLE từ tên sản phẩm
// ══════════════════════════════════════════════════════
const nameInput   = document.querySelector('[name="name"]');
const handleInput = document.querySelector('[name="handle"]');
if (nameInput && handleInput) {
    nameInput.addEventListener('input', function () {
        if (!handleInput._touched) {
            handleInput.value = this.value
                .toLowerCase()
                .normalize('NFD').replace(/[\u0300-\u036f]/g, '') // bỏ dấu
                .replace(/đ/g, 'd').replace(/Đ/g, 'D')
                .replace(/[^a-z0-9\s-]/g, '')
                .trim().replace(/\s+/g, '-');
        }
    });
    handleInput.addEventListener('input', () => { handleInput._touched = true; });
}