package dinhlam2901.sunilies.service;

import dinhlam2901.sunilies.model.CartItem;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CartService – quản lý giỏ hàng lưu trong HTTP Session
 */
@Service
public class CartService {

    private static final String SESSION_KEY = "SUNILIES_CART";

    // ── Lấy giỏ hàng từ session ───────────────────────────
    @SuppressWarnings("unchecked")
    public List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute(SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(SESSION_KEY, cart);
        }
        return cart;
    }

    // ── Thêm sản phẩm ─────────────────────────────────────
    public List<CartItem> addItem(HttpSession session, CartItem newItem) {
        List<CartItem> cart = getCart(session);
        String key = newItem.getKey();

        Optional<CartItem> existing = cart.stream()
                .filter(i -> i.getKey().equals(key))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQty(existing.get().getQty() + newItem.getQty());
        } else {
            if (newItem.getQty() < 1) newItem.setQty(1);
            cart.add(newItem);
        }

        session.setAttribute(SESSION_KEY, cart);
        return cart;
    }

    // ── Cập nhật số lượng ─────────────────────────────────
    public List<CartItem> updateQty(HttpSession session, String key, int qty) {
        List<CartItem> cart = getCart(session);
        if (qty <= 0) {
            cart.removeIf(i -> i.getKey().equals(key));
        } else {
            cart.stream()
                    .filter(i -> i.getKey().equals(key))
                    .findFirst()
                    .ifPresent(i -> i.setQty(qty));
        }
        session.setAttribute(SESSION_KEY, cart);
        return cart;
    }

    // ── Xoá một item ──────────────────────────────────────
    public List<CartItem> removeItem(HttpSession session, String key) {
        List<CartItem> cart = getCart(session);
        cart.removeIf(i -> i.getKey().equals(key));
        session.setAttribute(SESSION_KEY, cart);
        return cart;
    }

    // ── Xoá toàn bộ ───────────────────────────────────────
    public void clearCart(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    // ── Tổng tiền ─────────────────────────────────────────
    public double getTotalPrice(List<CartItem> cart) {
        return cart.stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    // ── Tổng số lượng ─────────────────────────────────────
    public int getTotalQty(List<CartItem> cart) {
        return cart.stream().mapToInt(CartItem::getQty).sum();
    }
}