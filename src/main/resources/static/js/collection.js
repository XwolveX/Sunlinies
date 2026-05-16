(function () {
    'use strict';

    const ITEMS_PER_PAGE = 12;
    let allCards        = [];
    let filteredCards   = [];
    let currentPage     = 1;
    let activeCategory  = 'all';
    let activeStatuses  = new Set();
    let priceMin        = null;
    let priceMax        = null;
    let currentSort     = 'default';
    let currentView     = 4;

    /* ── Init ──────────────────────────────────────────── */
    document.addEventListener('DOMContentLoaded', function () {
        allCards = Array.from(document.querySelectorAll('.cl-card'));
        updateCategoryCounts();
        applyFilters();
        initCategoryChips();
        initSidebarCategories();
        initSidebarStatuses();
        initSort();
        initViewToggle();
        initPriceFilter();
        initFilterDrawer();
        initScrollAnimation();
        syncWishlistIcons(); // ✅ FIX: khởi tạo trạng thái tim khi load trang
    });

    /* ════════════════════════════════════════════════════
       CATEGORY CHIPS (toolbar)
       ════════════════════════════════════════════════════ */
    function initCategoryChips() {
        document.querySelectorAll('#clCategoryChips .cl-chip').forEach(chip => {
            chip.addEventListener('click', function () {
                document.querySelectorAll('#clCategoryChips .cl-chip').forEach(c => c.classList.remove('is-active'));
                this.classList.add('is-active');
                const filter = this.dataset.filter;

                if (filter === 'sale') {
                    activeCategory = 'all';
                    activeStatuses = new Set(['sale']);
                } else if (filter === 'new') {
                    activeCategory = 'all';
                    activeStatuses = new Set(['new']);
                } else {
                    activeCategory = filter;
                    activeStatuses = new Set();
                }

                syncSidebarCategory(activeCategory);
                currentPage = 1;
                applyFilters();
            });
        });
    }

    /* ════════════════════════════════════════════════════
       SIDEBAR CATEGORIES
       ════════════════════════════════════════════════════ */
    function initSidebarCategories() {
        document.querySelectorAll('[data-category]').forEach(item => {
            item.addEventListener('click', function () {
                document.querySelectorAll('[data-category]').forEach(i => i.classList.remove('is-checked'));
                this.classList.add('is-checked');
                activeCategory = this.dataset.category;
                syncChipCategory(activeCategory);
                currentPage = 1;
                applyFilters();
            });
        });

        // Collapsible sections
        document.querySelectorAll('.cl-sidebar__title').forEach(title => {
            title.addEventListener('click', function () {
                this.closest('.cl-sidebar__section').classList.toggle('is-collapsed');
            });
        });
    }

    function syncSidebarCategory(cat) {
        document.querySelectorAll('[data-category]').forEach(item => {
            item.classList.toggle('is-checked', item.dataset.category === cat);
        });
    }
    function syncChipCategory(cat) {
        document.querySelectorAll('#clCategoryChips .cl-chip').forEach(chip => {
            chip.classList.toggle('is-active', chip.dataset.filter === cat);
        });
    }

    /* ════════════════════════════════════════════════════
       SIDEBAR STATUS
       ════════════════════════════════════════════════════ */
    function initSidebarStatuses() {
        document.querySelectorAll('[data-status]').forEach(item => {
            item.addEventListener('click', function () {
                this.classList.toggle('is-checked');
                const status = this.dataset.status;
                if (activeStatuses.has(status)) {
                    activeStatuses.delete(status);
                } else {
                    activeStatuses.add(status);
                }
                currentPage = 1;
                applyFilters();
            });
        });
    }

    /* ════════════════════════════════════════════════════
       PRICE FILTER
       ════════════════════════════════════════════════════ */
    function initPriceFilter() {
        const btn = document.getElementById('clBtnApplyPrice');
        if (btn) {
            btn.addEventListener('click', function () {
                const minVal = document.getElementById('clPriceMin').value;
                const maxVal = document.getElementById('clPriceMax').value;
                priceMin = minVal ? parseFloat(minVal) : null;
                priceMax = maxVal ? parseFloat(maxVal) : null;
                currentPage = 1;
                applyFilters();
            });
        }
    }

    /* ════════════════════════════════════════════════════
       SORT
       ════════════════════════════════════════════════════ */
    function initSort() {
        const sel = document.getElementById('clSortSelect');
        if (sel) {
            sel.addEventListener('change', function () {
                currentSort = this.value;
                currentPage = 1;
                applyFilters();
            });
        }
    }

    /* ════════════════════════════════════════════════════
       VIEW TOGGLE
       ════════════════════════════════════════════════════ */
    function initViewToggle() {
        const grid = document.getElementById('clGrid');
        [['clView2', 2], ['clView3', 3], ['clView4', 4]].forEach(([id, cols]) => {
            const btn = document.getElementById(id);
            if (!btn) return;
            btn.addEventListener('click', function () {
                document.querySelectorAll('.cl-view-btn').forEach(b => b.classList.remove('is-active'));
                this.classList.add('is-active');
                currentView = cols;
                grid.className = 'cl-grid';
                if (cols === 2) grid.classList.add('view-2');
                if (cols === 4) grid.classList.add('view-4');
            });
        });
    }

    /* ════════════════════════════════════════════════════
       FILTER DRAWER (mobile)
       ════════════════════════════════════════════════════ */
    function initFilterDrawer() {
        const overlay  = document.getElementById('clFilterOverlay');
        const drawer   = document.getElementById('clFilterDrawer');
        const btnOpen  = document.getElementById('clBtnFilterOpen');
        const btnClose = document.getElementById('clBtnFilterClose');

        btnOpen?.addEventListener('click', () => {
            overlay.classList.add('is-open');
            drawer.classList.add('is-open');
        });
        btnClose?.addEventListener('click', closeDrawer);
        overlay?.addEventListener('click', closeDrawer);

        function closeDrawer() {
            overlay.classList.remove('is-open');
            drawer.classList.remove('is-open');
        }

        // Mobile category items
        document.querySelectorAll('[data-category-m]').forEach(item => {
            item.addEventListener('click', function () {
                document.querySelectorAll('[data-category-m]').forEach(i => i.classList.remove('is-checked'));
                this.classList.add('is-checked');
                activeCategory = this.dataset.categoryM;
                syncSidebarCategory(activeCategory);
                syncChipCategory(activeCategory);
                currentPage = 1;
                applyFilters();
            });
        });
    }

    /* ════════════════════════════════════════════════════
       MAIN FILTER + SORT + PAGINATE
       ════════════════════════════════════════════════════ */
    function applyFilters() {
        // 1. Filter
        filteredCards = allCards.filter(card => {
            const cat    = card.dataset.category || '';
            const price  = parseFloat(card.dataset.price) || 0;
            const isSale = card.dataset.sale === 'true';
            const isNew  = card.dataset.new  === 'true';
            const isHot  = card.dataset.hot  === 'true';

            // Category
            if (activeCategory !== 'all' && cat !== activeCategory) return false;

            // Status
            if (activeStatuses.size > 0) {
                let pass = false;
                if (activeStatuses.has('sale') && isSale) pass = true;
                if (activeStatuses.has('new')  && isNew)  pass = true;
                if (activeStatuses.has('hot')  && isHot)  pass = true;
                if (!pass) return false;
            }

            // Price
            if (priceMin !== null && price < priceMin) return false;
            if (priceMax !== null && price > priceMax) return false;

            return true;
        });

        // 2. Sort
        filteredCards = [...filteredCards].sort((a, b) => {
            const pa = parseFloat(a.dataset.price) || 0;
            const pb = parseFloat(b.dataset.price) || 0;
            const na = (a.dataset.name || '').toLowerCase();
            const nb = (b.dataset.name || '').toLowerCase();

            if (currentSort === 'price-asc')  return pa - pb;
            if (currentSort === 'price-desc') return pb - pa;
            if (currentSort === 'name-asc')   return na.localeCompare(nb, 'vi');
            if (currentSort === 'newest') {
                const newA = a.dataset.new === 'true' ? 1 : 0;
                const newB = b.dataset.new === 'true' ? 1 : 0;
                return newB - newA;
            }
            return 0;
        });

        // 3. Update count
        document.getElementById('clVisibleCount').textContent = filteredCards.length;

        // 4. Paginate
        renderPage(currentPage);
        renderPagination();
    }

    function renderPage(page) {
        currentPage = page;
        const start     = (page - 1) * ITEMS_PER_PAGE;
        const end       = start + ITEMS_PER_PAGE;
        const pageCards = filteredCards.slice(start, end);

        // Hide all
        allCards.forEach(c => {
            c.style.display = 'none';
            c.classList.remove('is-visible');
        });

        // Show page cards
        pageCards.forEach((card, i) => {
            card.style.display = '';
            setTimeout(() => card.classList.add('is-visible'), i * 60);
        });

        // Empty state
        const emptyEl = document.getElementById('clEmpty');
        if (emptyEl) emptyEl.style.display = filteredCards.length === 0 ? 'block' : 'none';
    }

    function renderPagination() {
        const container = document.getElementById('clPagination');
        if (!container) return;

        const totalPages = Math.ceil(filteredCards.length / ITEMS_PER_PAGE);
        if (totalPages <= 1) { container.innerHTML = ''; return; }

        let html = '';
        html += `<button class="cl-page-btn" ${currentPage === 1 ? 'disabled' : ''} onclick="goPage(${currentPage - 1})">‹</button>`;

        for (let i = 1; i <= totalPages; i++) {
            if (totalPages > 7 && i > 2 && i < totalPages - 1 && Math.abs(i - currentPage) > 1) {
                if (i === 3 || i === totalPages - 2) html += `<span style="padding:0 4px;color:#bbb;">…</span>`;
                continue;
            }
            html += `<button class="cl-page-btn ${i === currentPage ? 'is-active' : ''}" onclick="goPage(${i})">${i}</button>`;
        }

        html += `<button class="cl-page-btn" ${currentPage === totalPages ? 'disabled' : ''} onclick="goPage(${currentPage + 1})">›</button>`;
        container.innerHTML = html;
    }

    window.goPage = function (page) {
        renderPage(page);
        renderPagination();
        document.querySelector('.cl-main').scrollIntoView({ behavior: 'smooth', block: 'start' });
    };

    /* ════════════════════════════════════════════════════
       CATEGORY COUNTS
       ════════════════════════════════════════════════════ */
    function updateCategoryCounts() {
        const counts = {};
        allCards.forEach(c => {
            const cat = c.dataset.category || '';
            counts[cat] = (counts[cat] || 0) + 1;
        });

        const map = {
            countAll:       allCards.length,
            countBag:       counts['bag']       || 0,
            countHat:       counts['hat']       || 0,
            countAccessory: counts['accessory'] || 0,
            countAo:        counts['ao']        || 0,
            countVay:       counts['vay']       || 0,
        };
        Object.entries(map).forEach(([id, val]) => {
            const el = document.getElementById(id);
            if (el) el.textContent = val;
        });
    }

    /* ════════════════════════════════════════════════════
       WISHLIST TOGGLE
       ✅ FIX: Lưu vào localStorage + cập nhật badge
          Trước đây chỉ đổi màu SVG mà không lưu gì cả
       ════════════════════════════════════════════════════ */
    function parsePriceText(text) {
        if (!text) return 0;
        return parseInt(text.replace(/[^\d]/g, '')) || 0;
    }

    window.toggleWish = function (btn) {
        const card = btn.closest('.cl-card');
        if (!card) return;

        // Đọc data từ card — ưu tiên data attributes, fallback đọc DOM text
        const id           = card.dataset.productId || card.dataset.id;
        const handle       = card.dataset.handle || id;
        const name         = card.querySelector('.cl-card__name a')?.textContent?.trim()
            || card.querySelector('[class*="name"] a')?.textContent?.trim();
        const image        = card.querySelector('img:not(.img-hover)')?.src;
        const priceFromAttr = parseFloat(card.dataset.price) || 0;
        const priceFromDOM  = parsePriceText(card.querySelector('.cl-card__price--main, .price')?.textContent);
        const price         = priceFromAttr || priceFromDOM;
        const comparePrice  = parseFloat(card.dataset.comparePrice) || 0;
        const onSale        = card.dataset.sale === 'true';
        const isNew         = card.dataset.new  === 'true';

        const item = { id, handle, name, image, price, comparePrice, onSale, isNew };

        const list = JSON.parse(localStorage.getItem('sunilies_wishlist') || '[]');
        const idx  = list.findIndex(p => p.id === id);

        if (idx > -1) {
            // ── Đã wish → bỏ wish
            list.splice(idx, 1);
            btn.classList.remove('is-wished');
            btn.querySelector('svg')?.setAttribute('fill', 'none');
        } else {
            // ── Chưa wish → thêm vào
            list.push(item);
            btn.classList.add('is-wished');
            btn.querySelector('svg')?.setAttribute('fill', 'currentColor');
        }

        localStorage.setItem('sunilies_wishlist', JSON.stringify(list));

        // Cập nhật badge số lượng trên header
        document.querySelectorAll('.js-wishlist-count')
            .forEach(el => el.textContent = list.length || '');
    };

    /**
     * Đồng bộ trạng thái tim khi load trang
     * (sản phẩm đã wish từ trước sẽ hiện tim đỏ ngay)
     */
    function syncWishlistIcons() {
        const list = JSON.parse(localStorage.getItem('sunilies_wishlist') || '[]');
        const ids  = new Set(list.map(p => p.id));

        allCards.forEach(card => {
            const id  = card.dataset.productId || card.dataset.id;
            const btn = card.querySelector('.cl-card__btn-wish');
            if (btn && ids.has(id)) {
                btn.classList.add('is-wished');
                btn.querySelector('svg')?.setAttribute('fill', 'currentColor');
            }
        });

        document.querySelectorAll('.js-wishlist-count')
            .forEach(el => el.textContent = list.length || '');
    }

    /* ════════════════════════════════════════════════════
       SCROLL ANIMATION
       ════════════════════════════════════════════════════ */
    function initScrollAnimation() {
        if (!window.IntersectionObserver) return;

        // Set hidden state qua JS thay vì CSS
        // → trang không dùng collection.js (search, wishlist...) sẽ hiển thị cards bình thường
        allCards.forEach(card => {
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';
        });

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('is-visible');
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1 });

        allCards.forEach(card => observer.observe(card));
    }


})();