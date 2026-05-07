/* ============================================================
   COL.JS - Collection page filter & sort
   Converted from col.js.liquid (removed Haravan Bizweb API)
   Spring Boot version: dùng URL params để filter/sort
   ============================================================ */

var selectedSortby = '';
var selectedView   = 'grid';
var tt = 'Mặc định';

/* ---- Sort ---- */
function sortby(sort) {
    selectedSortby = sort;
    resortby(sort);
    doSearch(1);
}

function resortby(sort) {
    $('.sort-cate-right .btn-quick-sort').removeClass('active');
    switch (sort) {
        case "price_min:asc":
            tt = "Giá tăng dần";
            $('.sort-cate-right .price-asc').addClass("active");
            break;
        case "price_min:desc":
            tt = "Giá giảm dần";
            $('.sort-cate-right .price-desc').addClass("active");
            break;
        case "name:asc":
            tt = "Tên A → Z";
            $('.sort-cate-right .alpha-asc').addClass("active");
            break;
        case "name:desc":
            tt = "Tên Z → A";
            $('.sort-cate-right .alpha-desc').addClass("active");
            break;
        case "created_on:desc":
            tt = "Hàng mới nhất";
            $('.sort-cate-right .position-desc').addClass("active");
            break;
        default:
            tt = "Mặc định";
            $('.sort-cate-right .default').addClass("active");
    }
    $('#sort-by > ul > li > span').html(tt);
}

function _selectSortby(sort) {
    resortby(sort);
    switch (sort) {
        case "price-asc":   selectedSortby = "price_min:asc";  break;
        case "price-desc":  selectedSortby = "price_min:desc"; break;
        case "alpha-asc":   selectedSortby = "name:asc";       break;
        case "alpha-desc":  selectedSortby = "name:desc";      break;
        case "created-desc":selectedSortby = "created_on:desc";break;
        case "created-asc": selectedSortby = "created_on:asc"; break;
        default:            selectedSortby = sort;
    }
}

/* ---- View switch ---- */
function switchView(view) {
    selectedView = view;
    if (view === 'list') {
        $('.button-view-mode').removeClass('active');
        $('.button-view-mode.view-mode-list').addClass('active');
        $('.category-products').addClass('view-list').removeClass('view-grid');
    } else {
        $('.button-view-mode').removeClass('active');
        $('.button-view-mode.view-mode-grid').addClass('active');
        $('.category-products').addClass('view-grid').removeClass('view-list');
    }
    var url = new URL(window.location.href);
    url.searchParams.set('view', view);
    window.history.pushState({}, null, url.toString());
}

/* ---- Search / Filter (gọi Spring Boot endpoint) ---- */
var activeFilters = {};

function doSearch(page) {
    var url = new URL(window.location.href);
    url.searchParams.set('page', page || 1);
    if (selectedSortby) url.searchParams.set('sortby', selectedSortby);
    // Gắn filter params
    Object.keys(activeFilters).forEach(function(key) {
        url.searchParams.delete(key);
        if (activeFilters[key] && activeFilters[key].length > 0) {
            activeFilters[key].forEach(function(v) { url.searchParams.append(key, v); });
        }
    });
    awe_showLoading('.category-products');
    $.get(url.toString(), function(html) {
        var $html = $(html);
        var $products = $html.find('.category-products');
        if ($products.length) {
            $('.category-products').html($products.html());
        } else {
            $('.category-products').html($html);
        }
        window.history.pushState({}, null, url.toString());
        awe_hideLoading('.category-products');
        if (typeof awe_lazyloadImage === 'function') awe_lazyloadImage();
        if (typeof wolf_swatch === 'function') wolf_swatch();
        initWishlistIcons && initWishlistIcons();
        $('html, body').animate({ scrollTop: $('.block-collection').offset().top - 80 }, 300);
    }).fail(function() {
        awe_hideLoading('.category-products');
    });
}

/* ---- Filter checkbox ---- */
function toggleFilter(element) {
    var $el    = $(element);
    var field  = $el.attr('data-field') || $el.attr('name');
    var value  = $el.val();
    if (!activeFilters[field]) activeFilters[field] = [];
    if (!$el.is(':checked')) {
        activeFilters[field] = activeFilters[field].filter(function(v) { return v !== value; });
    } else {
        activeFilters[field].push(value);
    }
    renderFilteredItems();
    doSearch(1);
}

function renderFilteredItems() {
    var $container = $(".filter-container__selected-filter-list ul");
    if (!$container.length) return;
    $container.html("");
    $(".aside-filter input[type=checkbox]:checked").each(function() {
        var id   = $(this).attr("id");
        var name = $(this).closest("label").text().trim();
        $container.append(
            '<li class="filter-container__selected-filter-item">' +
            '<a href="javascript:void(0)" onclick="removeFilteredItem(\'' + id + '\')">' +
            '<span>×</span> ' + name + '</a></li>'
        );
    });
    var hasFilter = $(".aside-filter input[type=checkbox]:checked").length > 0;
    $(".filter-container__selected-filter").toggle(hasFilter);
}

function removeFilteredItem(id) {
    $("#" + id).prop('checked', false).trigger('change');
}

function toggleCheckbox(id) {
    $(id).click();
}

/* ---- Price range filter ---- */
function applyPriceFilter(minVal, maxVal) {
    activeFilters['price_min'] = [minVal];
    activeFilters['price_max'] = [maxVal];
    doSearch(1);
}

/* ---- URL params restore on load ---- */
function selectFilterByCurrentQuery() {
    var url   = new URL(window.location.href);
    var sort  = url.searchParams.get('sortby');
    var page  = url.searchParams.get('page') || 1;
    var view  = url.searchParams.get('view') || 'grid';
    if (sort)  _selectSortby(sort);
    if (view)  switchView(view);
}

/* ---- Get URL parameter helper ---- */
function getParameter(url, name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex   = new RegExp("[\\?&]" + name + "=([^&#]*)");
    var results = regex.exec(url);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

/* ---- Push state ---- */
function pushState(url) {
    window.history.pushState({ url: url }, null, url);
}

/* ---- Init on ready ---- */
$(document).ready(function() {
    selectFilterByCurrentQuery();

    $('.filter-group .filter-group-title').click(function(e) {
        $(this).parent().toggleClass('active');
    });

    $('.btn-filter, .filter-mobile').click(function() {
        $(".layout-collection .left-content").toggleClass('active');
        $(".backdrop__body-backdrop___1rvky").toggleClass('active');
    });

    $('.backdrop__body-backdrop___1rvky').click(function() {
        $(".layout-collection .left-content").removeClass('active');
        $(this).removeClass('active');
    });

    $('.close-filters').click(function() {
        $(".layout-collection .left-content").removeClass('active');
        $('.backdrop__body-backdrop___1rvky').removeClass('active');
    });

    // Filter checkbox change
    $('.filter-item--check-box input').on('change', function() {
        toggleFilter($(this));
    });

    // Sort click
    $('.awe_sortby').on('click', function() {
        sortby($(this).attr('data-onclick'));
    });

    // Pagination
    $('.dosearch').click(function() {
        doSearch($(this).attr('data-onclick'));
    });

    // Sort dropdown (mobile)
    if ($(window).width() <= 991) {
        $('.sort-cate-right h3').on('click', function(e) {
            e.preventDefault();
            $(this).parents('.sort-cate-right').find('ul').stop().slideToggle();
            $(this).toggleClass('active');
            return false;
        });
    }

    // Filter items in list
    $('.filter-search-input').on('keyup', function() {
        var q = $(this).val().toLowerCase();
        $(this).closest('.aside-content').find('li').each(function() {
            var text = $(this).text().toLowerCase();
            $(this).toggle(text.indexOf(q) !== -1);
        });
    });
});