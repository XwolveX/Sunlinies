/* ============================================================
   MAIN JS - Converted from main.js.liquid (Haravan theme)
   Removed: Liquid conditionals, Haravan API calls
   ============================================================ */

window.awe = window.awe || {};

/* ---- Init ---- */
$(document).ready(function ($) {
    "use strict";
    awe_backtotop();
    awe_category();
    wolf_swatch();
    initScrollMenu();
    initMobileMenu();
    initSearchToggle();
    initQtyButtons();
    initFilters();
});

/* ---- Scroll nav desktop ---- */
function initScrollMenu() {
    if ($('.wolf-main-nav').length === 0) return;
    if ($('.wolf-main-nav').width() > $('.scroll-menu-desktop').width()) {
        $('.control-menu').addClass('active');
        $('.scroll-menu-desktop').addClass('has-control');
    }
    var margin_left = 0;
    $('#prev').on('click', function(e) {
        e.preventDefault();
        animateMargin(190);
    });
    $('#next').on('click', function(e) {
        e.preventDefault();
        animateMargin(-190);
    });
    function animateMargin(amount) {
        margin_left = Math.min(0, Math.max(getMaxMargin(), margin_left + amount));
        $('ul.wolf-main-nav').animate({'margin-left': margin_left}, 300);
    }
    function getMaxMargin() {
        return $('ul.wolf-main-nav').parent().width() - $('ul.wolf-main-nav')[0].scrollWidth;
    }
}

/* ---- Search toggle ---- */
function initSearchToggle() {
    $(document).on('click', '.wofl-search-link, .wolf-main-search-close', function(e) {
        if ($('body').toggleClass('show-search').hasClass('show-search')) {
            setTimeout(function() { $('.search-auto:first').focus(); }, 500);
        } else {
            $('.wofl-search-link:first').focus();
        }
        return false;
    });
}

/* ---- Mobile menu ---- */
function initMobileMenu() {
    var wDWs = $(window).width();
    if (wDWs <= 991) {
        $('.menu-bar').on('click', function() {
            $('body').toggleClass('menu-is-expanded');
            $('.opacity_menu').toggleClass('current');
        });
        $('.wolf-main-nav .nav-item-toggle').click(function(e) {
            $(this).closest('li').toggleClass('is-expanded');
            return false;
        });
        $('.wolf-main-nav .mobile-subnav-item-toggle').click(function(e) {
            $(this).closest('li').toggleClass('subnav-is-expanded');
            return false;
        });
        $('.opacity_menu').on('click', function() {
            $('.opacity_menu').removeClass('current');
            $('body').removeClass('menu-is-expanded');
        });
        $('.mobile-nav-toggle').on('click', function() {
            $('body').toggleClass('menu-is-expanded');
        });
    }
}

/* ---- Dropdown toggle ---- */
$('.dropdown-toggle').click(function() {
    $(this).parent().toggleClass('open');
});
$('.btn-close').click(function() {
    $(this).parents('.dropdown').toggleClass('open');
});

/* ---- Close popup/overlay ---- */
$(document).on('click', '.overlay, .close-popup, .btn-continue, .fancybox-close, .close-window', function() {
    awe.hidePopup('.awe-popup');
    setTimeout(function() { $('.loading').removeClass('loaded-content'); }, 500);
    return false;
});

/* ---- Close popup cart ---- */
$('.close-pop').click(function() {
    $('#popup-cart').removeClass('opencart');
    $('body').removeClass('opacitycart');
});

/* ---- Popup helpers ---- */
function awe_showPopup(selector) {
    $(selector).addClass('active');
}
window.awe_showPopup = awe_showPopup;

function awe_hidePopup(selector) {
    $(selector).removeClass('active');
}
window.awe_hidePopup = awe_hidePopup;

awe.hidePopup = function(selector) {
    $(selector).removeClass('active');
};

/* ---- Loading helpers ---- */
function awe_showLoading(selector) {
    var loading = '<div class="loading-icon"><div class="spinner"></div></div>';
    $(selector).addClass("loading").append(loading);
}
window.awe_showLoading = awe_showLoading;

function awe_hideLoading(selector) {
    $(selector).removeClass("loading");
    $(selector + ' .loading-icon').remove();
}
window.awe_hideLoading = awe_hideLoading;

/* ---- Color swatch ---- */
function wolf_swatch() {
    $('.wolf-product-block-item .wolf-product-grswatches .swatch-elemente').click(function(e) {
        $('.wolf-product-block-item .wolf-product-grswatches .swatch-elemente').removeClass('active');
        $(this).addClass('active');
        var img = $(this).attr('data-image');
        $(this).parents('.wolf-product-block-item').find('.wolf-product-grid-img .wolf-product-grid-link img').attr('src', img);
    });
}
window.wolf_swatch = wolf_swatch;

/* ---- Category toggle ---- */
function awe_category() {
    $('.nav-category .fa-angle-right').click(function(e) {
        $(this).toggleClass('fa-angle-down fa-angle-right');
        $(this).parent().toggleClass('active');
    });
    $('.nav-category .fa-angle-down').click(function(e) {
        $(this).toggleClass('fa-angle-right');
        $(this).parent().toggleClass('active');
    });
}
window.awe_category = awe_category;

/* ---- Back to top ---- */
function awe_backtotop() {
    $(window).scroll(function() {
        $(this).scrollTop() > 200 ? $('.backtop').addClass('show') : $('.backtop').removeClass('show');
    });
    $('.backtop').click(function() {
        $("body,html").animate({ scrollTop: 0 }, 800);
        return false;
    });
}
window.awe_backtotop = awe_backtotop;

/* ---- Quantity +/- buttons ---- */
function initQtyButtons() {
    $(document).on('keydown', '#qty, .number-sidebar', function(e) {
        if ($.inArray(e.keyCode, [46, 8, 9, 27, 13, 110, 190]) !== -1 ||
            /65|67|86|88/.test(e.keyCode) && (e.ctrlKey === true || e.metaKey === true) ||
            (e.keyCode >= 35 && e.keyCode <= 40)) return;
        if ((e.shiftKey || e.keyCode < 48 || e.keyCode > 57) &&
            (e.keyCode < 96 || e.keyCode > 105)) { e.preventDefault(); }
    });
    $(document).on('click', '.qtyplus', function(e) {
        e.preventDefault();
        var fieldName = $(this).attr('data-field');
        var currentVal = parseInt($('input[data-field="' + fieldName + '"]').val());
        if (!isNaN(currentVal)) {
            $('input[data-field="' + fieldName + '"]').val(currentVal + 1);
        } else {
            $('input[data-field="' + fieldName + '"]').val(0);
        }
    });
    $(document).on('click', '.qtyminus', function(e) {
        e.preventDefault();
        var fieldName = $(this).attr('data-field');
        var currentVal = parseInt($('input[data-field="' + fieldName + '"]').val());
        if (!isNaN(currentVal) && currentVal > 1) {
            $('input[data-field="' + fieldName + '"]').val(currentVal - 1);
        } else {
            $('input[data-field="' + fieldName + '"]').val(1);
        }
    });
}

/* ---- Filter sidebar ---- */
function initFilters() {
    $('.open-filters').click(function(e) {
        e.stopPropagation();
        $(this).toggleClass('openf');
        $('.dqdt-sidebar').toggleClass('openf');
        $('.opacity_sidebar').toggleClass('openf');
    });
    $('.opacity_sidebar').click(function(e) {
        $('.opacity_sidebar').removeClass('openf');
        $('.dqdt-sidebar, .open-filters').removeClass('openf');
    });
    $('.bolocs').click(function(e) {
        e.stopPropagation();
        $(this).toggleClass('openf');
        $('.aside-filter').toggleClass('show');
    });
    $('.aside-filter').click(function(e) { e.stopPropagation(); });
    $(document).click(function() { $('.aside-filter').slideUp(); });
    $('.aside-filter .aside-title').on('click', function(e) {
        e.preventDefault();
        $(this).parents('.aside-item').find('.aside-content').stop().slideToggle();
        $(this).toggleClass('active');
        return false;
    });
    if ($(window).width() <= 991) {
        $('.sort-cate-right h3').on('click', function(e) {
            e.preventDefault();
            $(this).parents('.sort-cate-right').find('ul').stop().slideToggle();
            $(this).toggleClass('active');
            return false;
        });
    }
}

/* ---- Sidebar collection toggle ---- */
$('.ul_collections li > svg').click(function() {
    $(this).parent().toggleClass('current');
    $(this).next('ul').slideToggle("fast");
    $(this).next('div').slideToggle("fast");
});

/* ---- Password recovery toggle ---- */
$('.quenmk').on('click', function() {
    $('#login').toggleClass('hidden');
    $('.h_recover').slideToggle();
});

/* ---- Click outside to close ---- */
$('body').click(function(event) {
    if (!$(event.target).closest('.collection-selector').length) {
        $('.list_search').css('display', 'none');
    }
});

/* ---- Alert system ---- */
var theme = window.theme || {};
theme.alert = (function() {
    var $alert    = $('#js-global-alert'),
        $title    = $('#js-global-alert .alert-heading'),
        $content  = $('#js-global-alert .alert-content'),
        close     = '#js-global-alert .close';
    var timeoutID = null;
    $(document).on('click', close, function() {
        $alert.removeClass('active');
    });
    function createAlert(title, mess, time, type) {
        var showTime = time || 3000;
        $alert.removeClass('alert-success alert-danger alert-warning alert-primary');
        $alert.addClass(type || 'alert-success');
        $title.html(title);
        $content.html(mess);
        $alert.addClass('active');
        if (timeoutID) clearTimeout(timeoutID);
        timeoutID = setTimeout(function() {
            $alert.removeClass('active');
        }, showTime);
    }
    return { new: createAlert };
})();

/* ---- Vietnamese text converter (slug) ---- */
function awe_convertVietnamese(str) {
    str = str.toLowerCase();
    str = str.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, "a");
    str = str.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, "e");
    str = str.replace(/ì|í|ị|ỉ|ĩ/g, "i");
    str = str.replace(/ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, "o");
    str = str.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, "u");
    str = str.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, "y");
    str = str.replace(/đ/g, "d");
    str = str.replace(/!|@|%|\^|\*|\(|\)|\+|=|<|>|\?|\/|,|\.|\:|\;|\'| |\"|\&|\#|\[|\]|~|\$|_/g, "-");
    str = str.replace(/-+-/g, "-");
    str = str.replace(/^\-+|\-+$/g, "");
    return str;
}
window.awe_convertVietnamese = awe_convertVietnamese;

/* ---- Lazy load init ---- */
function awe_lazyloadImage() {
    if (typeof LazyLoad !== 'undefined') {
        var ll = new LazyLoad({
            elements_selector: ".lazyload",
            load_delay: 100,
            threshold: 0
        });
    }
}
window.awe_lazyloadImage = awe_lazyloadImage;

/* ---- Cart: Add to cart via AJAX (Spring Boot endpoint) ---- */
$(document).on('click', '.add_to_cart', function(e) {
    e.preventDefault();
    var $this = $(this);
    var form = $this.parents('form');
    $.ajax({
        type: 'POST',
        url: '/cart/add',
        data: form.serialize(),
        dataType: 'json',
        success: function(data) {
            // Cập nhật số lượng giỏ hàng trên icon
            $('.count_item_pr').text(data.cartCount || 0);
            theme.alert.new('Thêm vào giỏ hàng', data.productName + ' đã được thêm vào giỏ hàng!', 3000, 'alert-success');
        },
        error: function() {
            theme.alert.new('Lỗi', 'Không thể thêm vào giỏ hàng. Vui lòng thử lại.', 3000, 'alert-danger');
        }
    });
});

/* ---- Product image thumbnail switcher ---- */
$(document).on('click', '.product-images-thumb img', function() {
    $('.product-images-thumb img').removeClass('active');
    $(this).addClass('active');
    var src = $(this).attr('src');
    $('.product-images-main img').attr('src', src);
});

/* ---- Sort collection (pure JS, no Haravan API) ---- */
function sortby(sort) {
    var url = new URL(window.location.href);
    url.searchParams.set('sortby', sort);
    window.location.href = url.toString();
}

/* ---- Search auto suggest (Spring Boot endpoint) ---- */
$(document).ready(function() {
    var searchTimeout = null;
    var $resultsBox = $('.list-search');
    var $itemsuggest = $('.item-suggest');

    $('.wolf-main-search .header-search-form input[type="text"]').on('keyup change', function() {
        var term = $(this).val();
        if (term.length > 2) {
            $itemsuggest.addClass('suggest-hide');
            clearTimeout(searchTimeout);
            $resultsBox.html('<div class="evo-loading"><div class="spinner-border" role="status"></div></div>');
            searchTimeout = setTimeout(function() {
                $.ajax({
                    url: '/search/suggest',
                    data: { q: term },
                    dataType: 'json',
                    success: function(data) {
                        $resultsBox.empty();
                        if (!data || data.length === 0) {
                            $resultsBox.html('<div class="note">Không có kết quả tìm kiếm</div>');
                        } else {
                            $.each(data, function(i, item) {
                                var $row = $('<a class="col-lg-3 col-md-3 col-sm-6 col-6 clearfix wolf-search-item-result"></a>')
                                    .attr('href', item.url).attr('title', item.name);
                                $row.append('<div class="img"><img src="' + item.imageUrl + '" /></div>');
                                $row.append('<div class="d-title">' + item.name + '</div>');
                                $row.append('<div class="d-title d-price">' + formatPrice(item.price) + '</div>');
                                $resultsBox.append($row);
                            });
                            $resultsBox.append('<a href="/search?q=' + encodeURIComponent(term) + '" class="note col-12">Xem tất cả kết quả</a>');
                        }
                    }
                });
            }, 400);
        } else {
            $resultsBox.empty();
            $itemsuggest.removeClass('suggest-hide');
        }
    }).bind('focusin', function() {
        $resultsBox.fadeIn(200);
    });

    $('body').bind('click', function() {
        $resultsBox.fadeOut(200);
        $itemsuggest.removeClass('suggest-hide');
    });
});

/* ---- Wishlist (localStorage) ---- */
var wishlistObject = JSON.parse(localStorage.getItem('localWishlist')) || [];

$(document).on('click', '.js-btn-wishlist', function(e) {
    e.preventDefault();
    var handle = $(this).data('handle');
    var idx = wishlistObject.indexOf(handle);
    if (idx !== -1) {
        wishlistObject.splice(idx, 1);
        $(this).removeClass('active');
    } else {
        wishlistObject.push(handle);
        $(this).addClass('active');
    }
    localStorage.setItem('localWishlist', JSON.stringify(wishlistObject));
    $('.js-wishlist-count').text(wishlistObject.length);
});

function initWishlistIcons() {
    wishlistObject = JSON.parse(localStorage.getItem('localWishlist')) || [];
    $('.js-wishlist-count').text(wishlistObject.length);
    $('.js-btn-wishlist').each(function() {
        var handle = $(this).data('handle');
        if (wishlistObject.indexOf(handle) !== -1) {
            $(this).addClass('active');
        }
    });
}
$(document).ready(function() { initWishlistIcons(); });

/* ---- Number format helper ---- */
function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
}
window.formatPrice = formatPrice;