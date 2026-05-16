/* ==============================================================
   about.js – Script trang Giới thiệu SUNILIES
   Đặt tại: src/main/resources/static/js/about.js
   ============================================================== */

(function () {
    'use strict';

    // ─────────────────────────────────────────────────────────────
    // 1. SCROLL REVEAL – fade-in khi cuộn tới
    // ─────────────────────────────────────────────────────────────
    function initScrollReveal() {
        const items = document.querySelectorAll('.reveal');
        if (!items.length) return;

        // Trình duyệt cũ không hỗ trợ IntersectionObserver → show luôn
        if (!('IntersectionObserver' in window)) {
            items.forEach(el => el.classList.add('is-visible'));
            return;
        }

        const io = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('is-visible');
                    io.unobserve(entry.target);
                }
            });
        }, {
            threshold: 0.12,
            rootMargin: '0px 0px -40px 0px'
        });

        items.forEach(el => io.observe(el));
    }

    // ─────────────────────────────────────────────────────────────
    // 2. MOBILE MENU – đồng bộ với fragments/header
    // ─────────────────────────────────────────────────────────────
    function initMobileMenu() {
        const overlay  = document.getElementById('mobileOverlay');
        const menu     = document.getElementById('mobileMenu');
        const closeBtn = document.getElementById('mobileMenuClose');

        if (!menu) return;

        const open  = () => {
            menu.classList.add('is-open');
            overlay && overlay.classList.add('is-open');
        };
        const close = () => {
            menu.classList.remove('is-open');
            overlay && overlay.classList.remove('is-open');
        };

        closeBtn && closeBtn.addEventListener('click', close);
        overlay  && overlay.addEventListener('click', close);

        document.querySelectorAll('[data-mobile-menu-trigger]')
            .forEach(b => b.addEventListener('click', open));
    }

    // ─────────────────────────────────────────────────────────────
    // INIT khi DOM sẵn sàng
    // ─────────────────────────────────────────────────────────────
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            initScrollReveal();
            initMobileMenu();
        });
    } else {
        initScrollReveal();
        initMobileMenu();
    }
})();