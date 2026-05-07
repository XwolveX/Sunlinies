/* ======= SWIPER INIT ======= */
// Main slider
const sliderSwiper = new Swiper('.sliderSwiper', {
    loop: true,
    autoplay: { delay: 5000, disableOnInteraction: false },
    pagination: { el: '.section_slider .swiper-pagination', clickable: true },
    navigation: {
        nextEl: '.section_slider .swiper-button-next',
        prevEl: '.section_slider .swiper-button-prev',
    },
    speed: 700,
});

// Product trending slider
const productSwiper = new Swiper('.productSwiper', {
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

/* ======= MOBILE MENU ======= */
const btnHamburger   = document.getElementById('btnHamburger');
const mobileMenu     = document.getElementById('mobileMenu');
const mobileOverlay  = document.getElementById('mobileOverlay');
const mobileMenuClose= document.getElementById('mobileMenuClose');

function openMobileMenu() { mobileMenu.classList.add('active'); mobileOverlay.classList.add('active'); }
function closeMobileMenu() { mobileMenu.classList.remove('active'); mobileOverlay.classList.remove('active'); }

btnHamburger.addEventListener('click', openMobileMenu);
mobileMenuClose.addEventListener('click', closeMobileMenu);
mobileOverlay.addEventListener('click', closeMobileMenu);

/* ======= CART DRAWER ======= */
// Lưu giỏ hàng đơn giản bằng JS (trong Spring Boot sẽ gọi /api/cart)
let cart = JSON.parse(localStorage.getItem('wbCart') || '[]');

const btnCartOpen  = document.getElementById('btnCartOpen');
const btnCartClose = document.getElementById('btnCartClose');
const cartOverlay  = document.getElementById('cartOverlay');
const cartDrawer   = document.getElementById('cartDrawer');

function openCart()  { cartDrawer.classList.add('active'); cartOverlay.classList.add('active'); renderCart(); }
function closeCart() { cartDrawer.classList.remove('active'); cartOverlay.classList.remove('active'); }

btnCartOpen.addEventListener('click', openCart);
btnCartClose.addEventListener('click', closeCart);
cartOverlay.addEventListener('click', closeCart);

function renderCart() {
    const list = document.getElementById('cartItemsList');
    const footer = document.getElementById('cartFooter');
    const countBadge = document.getElementById('cartCount');
    const countHead = document.getElementById('cartItemCount');
    const totalEl = document.getElementById('cartTotal');

    if (cart.length === 0) {
        list.innerHTML = `<div class="cart-empty"><svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/></svg><p>Giỏ hàng của bạn đang trống</p></div>`;
        footer.style.display = 'none';
        countBadge.textContent = 0;
        countHead.textContent = 0;
        return;
    }

    let total = 0;
    list.innerHTML = cart.map((item, i) => {
        total += item.price * item.qty;
        return `
        <div class="cart-item">
          <img src="${item.image}" alt="${item.name}" />
          <div class="cart-item-info">
            <div class="name">${item.name}</div>
            <div class="variant">${item.variant || ''}</div>
            <div class="qty-row">
              <div class="qty-control">
                <button onclick="updateQty(${i}, ${item.qty - 1})">−</button>
                <input type="number" value="${item.qty}" min="1" onchange="updateQty(${i}, this.value)" />
                <button onclick="updateQty(${i}, ${item.qty + 1})">+</button>
              </div>
              <span class="cart-item-price">${formatMoney(item.price * item.qty)}</span>
            </div>
          </div>
        </div>`;
    }).join('');

    footer.style.display = 'block';
    totalEl.textContent = formatMoney(total);
    countBadge.textContent = cart.reduce((s, i) => s + i.qty, 0);
    countHead.textContent = cart.length;
}

function updateQty(index, qty) {
    qty = parseInt(qty);
    if (qty <= 0) { cart.splice(index, 1); }
    else { cart[index].qty = qty; }
    localStorage.setItem('wbCart', JSON.stringify(cart));
    renderCart();
}

function addToCart(product) {
    const existing = cart.find(i => i.id === product.id);
    if (existing) { existing.qty++; }
    else { cart.push({ ...product, qty: 1 }); }
    localStorage.setItem('wbCart', JSON.stringify(cart));
    renderCart();
    openCart();
}

function formatMoney(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// Gắn sự kiện "Xem nhanh" — mẫu demo
document.querySelectorAll('.btn-quickview').forEach(btn => {
    btn.addEventListener('click', function(e) {
        e.preventDefault();
        // TODO: mở quickview modal, gọi /api/products/:id
        alert('Quickview - sẽ gọi API /api/products/{id} trong Spring Boot');
    });
});

/* ======= BACK TO TOP ======= */
const backToTopBtn = document.getElementById('backToTop');
window.addEventListener('scroll', () => {
    backToTopBtn.classList.toggle('visible', window.scrollY > 400);
});
backToTopBtn.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));

/* ======= SUBSCRIBE ======= */
function handleSubscribe(e) {
    e.preventDefault();
    // TODO: gọi POST /api/subscribe
    alert('Đăng ký thành công! Cảm ơn bạn.');
    e.target.reset();
}

/* ======= INIT ======= */
renderCart();