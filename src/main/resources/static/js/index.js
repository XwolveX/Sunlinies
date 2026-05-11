/**
 * index.js – JavaScript trang chủ SUNILIES
 * Đặt tại: src/main/resources/static/js/index.js
 *
 * Phụ thuộc: Swiper (load trước qua base.html hoặc CDN)
 */

document.addEventListener('DOMContentLoaded', function () {

    /* =============================================
       1. SWIPER – HERO SLIDER
    ============================================= */
    if (document.querySelector('.sliderSwiper')) {
        new Swiper('.sliderSwiper', {
            loop: true,
            autoplay: { delay: 5000, disableOnInteraction: false },
            pagination: { el: '.section_slider .swiper-pagination', clickable: true },
            navigation: {
                nextEl: '.section_slider .swiper-button-next',
                prevEl: '.section_slider .swiper-button-prev',
            },
            speed: 700,
        });
    }

    /* =============================================
       2. SWIPER – PRODUCT TRENDING SLIDER
    ============================================= */
    if (document.querySelector('.productSwiper')) {
        new Swiper('.productSwiper', {
            slidesPerView: 2,
            spaceBetween: 16,
            navigation: {
                nextEl: '.slider-prod-next',
                prevEl: '.slider-prod-prev',
            },
            breakpoints: {
                640:  { slidesPerView: 3, spaceBetween: 16 },
                992:  { slidesPerView: 4, spaceBetween: 20 },
                1200: { slidesPerView: 5, spaceBetween: 20 },
            },
        });
    }

    /* =============================================
       3. MOBILE MENU
    ============================================= */
    const btnHamburger    = document.getElementById('btnHamburger');
    const mobileMenu      = document.getElementById('mobileMenu');
    const mobileOverlay   = document.getElementById('mobileOverlay');
    const mobileMenuClose = document.getElementById('mobileMenuClose');

    function openMobileMenu() {
        mobileMenu    && mobileMenu.classList.add('active');
        mobileOverlay && mobileOverlay.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
    function closeMobileMenu() {
        mobileMenu    && mobileMenu.classList.remove('active');
        mobileOverlay && mobileOverlay.classList.remove('active');
        document.body.style.overflow = '';
    }

    btnHamburger    && btnHamburger.addEventListener('click', openMobileMenu);
    mobileMenuClose && mobileMenuClose.addEventListener('click', closeMobileMenu);
    mobileOverlay   && mobileOverlay.addEventListener('click', closeMobileMenu);

    /* =============================================
       4. SEARCH BAR TOGGLE
    ============================================= */
    const searchLink  = document.querySelector('.wofl-search-link');
    const searchBar   = document.querySelector('.wolf-main-search');
    const searchClose = document.querySelector('.wolf-main-search-close');

    searchLink && searchLink.addEventListener('click', function (e) {
        e.preventDefault();
        searchBar && searchBar.classList.toggle('active');
        if (searchBar && searchBar.classList.contains('active')) {
            const input = searchBar.querySelector('input');
            input && input.focus();
        }
    });
    searchClose && searchClose.addEventListener('click', function () {
        searchBar && searchBar.classList.remove('active');
    });

    /* =============================================
       5. CART DRAWER
       (Giao tiếp với Spring Boot qua /api/cart)
    ============================================= */
    const btnCartOpen  = document.getElementById('btnCartOpen');
    const btnCartClose = document.getElementById('btnCartClose');
    const cartOverlay  = document.getElementById('cartOverlay');
    const cartDrawer   = document.getElementById('cartDrawer');

    function openCart()  {
        cartDrawer  && cartDrawer.classList.add('active');
        cartOverlay && cartOverlay.classList.add('active');
        document.body.style.overflow = 'hidden';
        loadCart();
    }
    function closeCart() {
        cartDrawer  && cartDrawer.classList.remove('active');
        cartOverlay && cartOverlay.classList.remove('active');
        document.body.style.overflow = '';
    }

    btnCartOpen  && btnCartOpen.addEventListener('click', openCart);
    btnCartClose && btnCartClose.addEventListener('click', closeCart);
    cartOverlay  && cartOverlay.addEventListener('click', closeCart);

    /**
     * Gọi GET /api/cart để lấy giỏ hàng từ server (Spring Boot session)
     * Fallback: dùng localStorage khi chưa có API
     */
    async function loadCart() {
        try {
            const res  = await fetch('/api/cart');
            if (!res.ok) throw new Error('API not ready');
            const data = await res.json();
            renderCart(data.items, data.totalPrice);
            updateCartBadge(data.items.reduce((s, i) => s + i.qty, 0));
        } catch {
            // Fallback: localStorage
            const localCart = JSON.parse(localStorage.getItem('sunCart') || '[]');
            const total = localCart.reduce((s, i) => s + i.price * i.qty, 0);
            renderCart(localCart, total);
            updateCartBadge(localCart.reduce((s, i) => s + i.qty, 0));
        }
    }

    function renderCart(items, totalPrice) {
        const list   = document.getElementById('cartItemsList');
        const footer = document.getElementById('cartFooter');
        const totalEl = document.getElementById('cartTotal');
        if (!list) return;

        if (!items || items.length === 0) {
            list.innerHTML = `
                <div class="cart-empty">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1"
                              d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/>
                    </svg>
                    <p>Giỏ hàng của bạn đang trống</p>
                </div>`;
            footer && (footer.style.display = 'none');
            document.getElementById('cartItemCount') && (document.getElementById('cartItemCount').textContent = 0);
            return;
        }

        list.innerHTML = items.map((item, i) => `
            <div class="cart-item">
                <img src="${item.image || '/image/no-image.png'}" alt="${item.name}" />
                <div class="cart-item-info">
                    <div class="name">${item.name}</div>
                    <div class="variant">${item.variant || ''}</div>
                    <div class="qty-row">
                        <div class="qty-control">
                            <button onclick="window.cartUpdateQty(${item.variantId || i}, ${item.qty - 1})">−</button>
                            <input type="number" value="${item.qty}" min="1"
                                   onchange="window.cartUpdateQty(${item.variantId || i}, this.value)" />
                            <button onclick="window.cartUpdateQty(${item.variantId || i}, ${item.qty + 1})">+</button>
                        </div>
                        <span class="cart-item-price">${formatMoney(item.price * item.qty)}</span>
                    </div>
                </div>
            </div>`
        ).join('');

        if (footer) {
            footer.style.display = 'block';
            if (totalEl) totalEl.textContent = formatMoney(totalPrice);
        }
        if (document.getElementById('cartItemCount'))
            document.getElementById('cartItemCount').textContent = items.length;
    }

    /** Cập nhật số hiển thị trên icon giỏ hàng */
    function updateCartBadge(count) {
        const badges = document.querySelectorAll('.count_item, #cartCount');
        badges.forEach(b => b.textContent = count);
    }

    /** Gọi PUT /api/cart/:id?qty=N hoặc sửa localStorage */
    window.cartUpdateQty = async function (variantId, qty) {
        qty = parseInt(qty);
        try {
            const method = qty <= 0 ? 'DELETE' : 'PUT';
            const url    = qty <= 0
                ? `/api/cart/${variantId}`
                : `/api/cart/${variantId}?qty=${qty}`;
            const res = await fetch(url, { method });
            if (!res.ok) throw new Error();
            const data = await res.json();
            renderCart(data.items, data.totalPrice);
            updateCartBadge(data.items.reduce((s, i) => s + i.qty, 0));
        } catch {
            // Fallback localStorage
            let localCart = JSON.parse(localStorage.getItem('sunCart') || '[]');
            const idx = localCart.findIndex(i => i.variantId === variantId);
            if (idx > -1) {
                if (qty <= 0) localCart.splice(idx, 1);
                else localCart[idx].qty = qty;
            }
            localStorage.setItem('sunCart', JSON.stringify(localCart));
            const total = localCart.reduce((s, i) => s + i.price * i.qty, 0);
            renderCart(localCart, total);
            updateCartBadge(localCart.reduce((s, i) => s + i.qty, 0));
        }
    };

    /** Thêm sản phẩm vào giỏ – gọi từ nút "Thêm vào giỏ hàng" */
    window.addToCart = async function (product) {
        try {
            const res = await fetch('/api/cart/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(product),
            });
            if (!res.ok) throw new Error();
            const data = await res.json();
            renderCart(data.items, data.totalPrice);
            updateCartBadge(data.items.reduce((s, i) => s + i.qty, 0));
        } catch {
            let localCart = JSON.parse(localStorage.getItem('sunCart') || '[]');
            const existing = localCart.find(i => i.variantId === product.variantId);
            if (existing) existing.qty++;
            else localCart.push({ ...product, qty: 1 });
            localStorage.setItem('sunCart', JSON.stringify(localCart));
            const total = localCart.reduce((s, i) => s + i.price * i.qty, 0);
            renderCart(localCart, total);
            updateCartBadge(localCart.reduce((s, i) => s + i.qty, 0));
        }
        openCart();
    };

    /* =============================================
       6. QUICKVIEW
    ============================================= */
    document.querySelectorAll('.btn-quickview').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const productId = this.closest('[data-product-id]')?.dataset.productId;
            if (productId) {
                // TODO: fetch('/api/products/' + productId) rồi hiển thị modal
                console.log('Quickview product:', productId);
            }
        });
    });

    /* =============================================
       7. BACK TO TOP
    ============================================= */
    const backToTopBtn = document.getElementById('backToTop');
    if (backToTopBtn) {
        window.addEventListener('scroll', () => {
            backToTopBtn.classList.toggle('visible', window.scrollY > 400);
        });
        backToTopBtn.addEventListener('click', () =>
            window.scrollTo({ top: 0, behavior: 'smooth' })
        );
    }

    /* =============================================
       8. SUBSCRIBE FORM
    ============================================= */
    const subscribeForms = document.querySelectorAll('.subscribe-form, [data-subscribe-form]');
    subscribeForms.forEach(form => {
        form.addEventListener('submit', async function (e) {
            e.preventDefault();
            const email = this.querySelector('input[type="email"]')?.value;
            try {
                await fetch('/api/newsletter', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email }),
                });
                alert('Đăng ký thành công! Cảm ơn bạn.');
            } catch {
                alert('Đăng ký thành công! Cảm ơn bạn.');
            }
            this.reset();
        });
    });
    /* =============================================
   10. NAV TRANSPARENT → SOLID ON SCROLL
============================================= */
    const siteNav = document.querySelector('.site-nav');

    if (siteNav) {
        const slider = document.querySelector('.section_slider');

        const onScroll = () => {
            const threshold = slider ? slider.offsetHeight * 0.8 : 400;
            siteNav.classList.toggle('nav--scrolled', window.scrollY > threshold);
        };

        window.addEventListener('scroll', onScroll, { passive: true });
        onScroll();
    }
    /* =============================================
       HELPER: format tiền Việt
    ============================================= */
    function formatMoney(amount) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    }
    /* =============================================
   11. STORY SECTION – SCROLL SNAP + ANIMATION
============================================= */
    const storySection = document.getElementById('storySection');

    if (storySection) {
        let snapped = false;

        const observer = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                if (snapped) return;

                const rect = storySection.getBoundingClientRect();
                const windowH = window.innerHeight;

                // Khi cạnh dưới màn hình chạm tới giữa section
                const sectionMid = rect.top + rect.height / 2;

                if (sectionMid <= windowH && !snapped) {
                    snapped = true;
                    observer.disconnect();

                    // 1. Kéo section về giữa màn hình
                    storySection.scrollIntoView({
                        behavior: 'smooth',
                        block: 'center'
                    });

                    // 2. Sau khi scroll xong (~700ms) mới chạy animation
                    setTimeout(() => {
                        storySection.classList.add('visible');
                    }, 700);
                }
            });
        }, {
            // Kích hoạt liên tục khi section đi vào viewport
            threshold: Array.from({ length: 20 }, (_, i) => i / 20)
        });

        observer.observe(storySection);
    }
}); // end DOMContentLoaded