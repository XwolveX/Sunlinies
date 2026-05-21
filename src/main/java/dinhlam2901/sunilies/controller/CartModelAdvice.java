package dinhlam2901.sunilies.controller;

import dinhlam2901.sunilies.model.CartItem;
import dinhlam2901.sunilies.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class CartModelAdvice {

    @Autowired
    private CartService cartService;

    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        List<CartItem> items = cartService.getCart(session);
        return cartService.getTotalQty(items);
    }
}
