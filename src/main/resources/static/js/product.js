/**
 * product.js – SUNILIES Product Detail Page
 * src/main/resources/static/js/product.js
 */

(function () {
    'use strict';

    /* ── State ─────────────────────────────────────────── */
    const state = {
        selectedSize: null,
        selectedColor: null,
        quantity: 1,
        wished: false,
        currentImg: null
    };

    /* ── DOM refs ──────────────────────────────────────── */
    const mainImg       = document.getElementById('pdMainImg');
    const thumbItems    = document.querySelectorAll('.pd-gallery__thumb');
    const lightbox      = document.getElementById('pdLightbox');
    const lightboxImg   = document.getElementById('pdLightboxImg');
    const qtyInput      = document.getElementById('pdQtyInput');
    const btnCart       = document.getElementById('pdBtnCart');
    const btnWish       = document.getElementById('pdBtnWish');
    const toast         = document.getElementById('pdToast');
    const tabBtns       = document.querySelectorAll('.pd-tab-btn');
    const tabPanels     = document.querySelectorAll('.pd-tab-panel');

    /* ════════════════════════════════════════════════════
       GALLERY
       ════════════════════════════════════════════════════ */
    function initGallery() {
        thumbItems.forEach(thumb => {
            thumb.addEventListener('click', () => switchImage(thumb));
        });

        // Lightbox open
        if (mainImg) {
            mainImg.parentElement.addEventListener('click', openLightbox);
        }

        // Lightbox close
        if (lightbox) {
            lightbox.addEventListener('click', (e) => {
                if (e.target === lightbox || e.target.classList.contains('pd-lightbox__close')) {
                    closeLightbox();
                }
            });
        }

        // Keyboard
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') closeLightbox();
        });
    }

    function switchImage(thumb) {
        const src = thumb.dataset.src;
        if (!src || !mainImg) return;

        // Fade out → update → fade in
        mainImg.parentElement.classList.add('switching');
        setTimeout(() => {
            mainImg.src = src;
            state.currentImg = src;
            mainImg.parentElement.classList.remove('switching');
        }, 150);

        thumbItems.forEach(t => t.classList.remove('is-active'));
        thumb.classList.add('is-active');
    }

    function openLightbox() {
        if (!lightbox || !mainImg) return;
        lightboxImg.src = mainImg.src;
        lightbox.classList.add('is-open');
        document.body.style.overflow = 'hidden';
    }

    function closeLightbox() {
        if (!lightbox) return;
        lightbox.classList.remove('is-open');
        document.body.style.overflow = '';
    }

    /* ════════════════════════════════════════════════════
       VARIANTS
       ════════════════════════════════════════════════════ */
    function initVariants() {
        // Sizes
        document.querySelectorAll('.pd-size-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.pd-size-btn').forEach(b => b.classList.remove('is-active'));
                btn.classList.add('is-active');
                state.selectedSize = btn.dataset.value;
                const el = document.getElementById('pdSelectedSize');
                if (el) el.textContent = state.selectedSize;
            });
        });

        // Colors
        document.querySelectorAll('.pd-color-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.pd-color-btn').forEach(b => b.classList.remove('is-active'));
                btn.classList.add('is-active');
                state.selectedColor = btn.dataset.value;
                const el = document.getElementById('pdSelectedColor');
                if (el) el.textContent = state.selectedColor;
            });
        });
    }

    /* ════════════════════════════════════════════════════
       QUANTITY
       ════════════════════════════════════════════════════ */
    function initQuantity() {
        const btnMinus = document.getElementById('pdQtyMinus');
        const btnPlus  = document.getElementById('pdQtyPlus');
        const stock = parseInt(qtyInput?.getAttribute('max')) || 999;

        if (btnMinus) {
            btnMinus.addEventListener('click', () => {
                let val = parseInt(qtyInput.value) - 1;
                qtyInput.value = Math.max(1, val);
                state.quantity = parseInt(qtyInput.value);
            });
        }
        if (btnPlus) {
            btnPlus.addEventListener('click', () => {
                let val = parseInt(qtyInput.value) + 1;
                qtyInput.value = Math.min(stock, val);
                state.quantity = parseInt(qtyInput.value);
            });
        }
        if (qtyInput) {
            qtyInput.addEventListener('change', () => {
                let val = parseInt(qtyInput.value) || 1;
                val = Math.max(1, Math.min(stock, val));
                qtyInput.value = val;
                state.quantity = val;
            });
        }
    }

    /* ════════════════════════════════════════════════════
       ADD TO CART
       ════════════════════════════════════════════════════ */
    function initCart() {
        if (!btnCart) return;

        btnCart.addEventListener('click', () => {
            // Loading state
            btnCart.disabled = true;
            btnCart.classList.add('is-loading');
            const textEl = btnCart.querySelector('span');
            const origText = textEl ? textEl.textContent : '';
            if (textEl) textEl.textContent = '...';

            setTimeout(() => {
                // Success state
                btnCart.classList.remove('is-loading');
                btnCart.classList.add('is-success');
                if (textEl) textEl.textContent = '✓ Đã thêm vào giỏ';

                showToast('🛒 Đã thêm sản phẩm vào giỏ hàng!');

                setTimeout(() => {
                    btnCart.classList.remove('is-success');
                    btnCart.disabled = false;
                    if (textEl) textEl.textContent = origText;
                }, 2500);
            }, 800);
        });
    }

    /* ════════════════════════════════════════════════════
       WISHLIST
       ════════════════════════════════════════════════════ */
    function initWishlist() {
        if (!btnWish) return;

        btnWish.addEventListener('click', () => {
            state.wished = !state.wished;
            btnWish.classList.toggle('is-wished', state.wished);
            showToast(state.wished
                ? '❤️ Đã thêm vào danh sách yêu thích'
                : '💔 Đã xoá khỏi danh sách yêu thích');
        });
    }

    /* ════════════════════════════════════════════════════
       TABS
       ════════════════════════════════════════════════════ */
    function initTabs() {
        tabBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const target = btn.dataset.tab;

                tabBtns.forEach(b => b.classList.remove('is-active'));
                tabPanels.forEach(p => p.classList.remove('is-active'));

                btn.classList.add('is-active');
                const panel = document.getElementById('pdTab-' + target);
                if (panel) panel.classList.add('is-active');
            });
        });
    }

    /* ════════════════════════════════════════════════════
       TOAST
       ════════════════════════════════════════════════════ */
    let toastTimer;
    function showToast(msg) {
        if (!toast) return;
        clearTimeout(toastTimer);
        toast.textContent = msg;
        toast.classList.add('is-visible');
        toastTimer = setTimeout(() => {
            toast.classList.remove('is-visible');
        }, 3000);
    }

    /* ════════════════════════════════════════════════════
       SCROLL ANIMATIONS (Intersection Observer)
       ════════════════════════════════════════════════════ */
    function initScrollAnimations() {
        const items = document.querySelectorAll('.animate-up');
        if (!items.length) return;

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry, i) => {
                if (entry.isIntersecting) {
                    setTimeout(() => {
                        entry.target.style.opacity = '1';
                        entry.target.style.transform = 'translateY(0)';
                    }, i * 80);
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1 });

        items.forEach(item => {
            item.style.opacity = '0';
            item.style.transform = 'translateY(24px)';
            item.style.transition = 'opacity .5s ease, transform .5s ease';
            observer.observe(item);
        });
    }

    /* ════════════════════════════════════════════════════
       COPY LINK
       ════════════════════════════════════════════════════ */
    window.copyProductLink = function () {
        navigator.clipboard.writeText(window.location.href).then(() => {
            showToast('🔗 Đã copy link sản phẩm!');
        });
    };

    /* ── INIT ──────────────────────────────────────────── */
    document.addEventListener('DOMContentLoaded', () => {
        initGallery();
        initVariants();
        initQuantity();
        initCart();
        initWishlist();
        initTabs();
        initScrollAnimations();
    });

})();