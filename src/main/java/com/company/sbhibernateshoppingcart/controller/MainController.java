package com.company.sbhibernateshoppingcart.controller;


import com.company.sbhibernateshoppingcart.dao.OrderDAO;
import com.company.sbhibernateshoppingcart.dao.ProductDAO;
import com.company.sbhibernateshoppingcart.entity.Product;
import com.company.sbhibernateshoppingcart.form.CustomerForm;
import com.company.sbhibernateshoppingcart.model.CartInfo;
import com.company.sbhibernateshoppingcart.model.CustomerInfo;
import com.company.sbhibernateshoppingcart.model.ProductInfo;
import com.company.sbhibernateshoppingcart.pagination.PaginationResult;
import com.company.sbhibernateshoppingcart.utils.Utils;
import com.company.sbhibernateshoppingcart.validator.CustomerFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@Transactional
public class MainController {

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private CustomerFormValidator customerFormValidator;

    @InitBinder
    public void myInitBinder(WebDataBinder dataBinder) {
        Object target = dataBinder.getTarget();
        if (target == null) {
            return;
        }

        System.out.println("Target=" + target);

        //case update quantity in cart
        //(@ModelAttribute("cartForm) @Validated CartInfo cartForm)
        if (target.getClass() == CartInfo.class) {

        }

        //case save customer information
        //(@ModelAttribute @Validated CustomerInfo customerInfo)
        else if (target.getClass() == CustomerForm.class) {
            dataBinder.setValidator(customerFormValidator);
        }
    }

    @RequestMapping("/403")
    public String accessDenied() {
        return "/403";
    }

    @RequestMapping("/")
    public String home() {
        return "index";
    }

    //product list
    @RequestMapping({"/productList"})
    public String listProductHandler(Model model,
                                     @RequestParam(value = "name", defaultValue = "") String likeName,
                                     @RequestParam(value = "page", defaultValue = "1") int page) {
        final int maxResult = 5;
        final int maxNavigationPage = 10;

        PaginationResult<ProductInfo> result = productDAO.queryProducts(page,
                maxResult, maxNavigationPage, likeName);

        model.addAttribute("paginationProducts", result);
        return "productList";
    }

    @RequestMapping({"/buyProduct"})
    public String listProductHandler(HttpServletRequest request, Model model,
                                     @RequestParam(value = "code", defaultValue = "") String code) {
        Product product = null;
        if (code != null && code.length() > 0) {
            product = productDAO.findProduct(code);
        }
        if (product != null) {
            CartInfo cartInfo = Utils.getCartInSession(request);

            ProductInfo productInfo = new ProductInfo(product);

            cartInfo.addProduct(productInfo, 1);
        }

        return "redirect:/shoppingCart";
    }

    @RequestMapping({"/shoppingCartRemoveProduct"})
    public String removeProductHandler(HttpServletRequest request, Model model,
                                       @RequestParam(value = "code", defaultValue = "") String code) {
        Product product = null;
        if (code != null && code.length() > 0) {
            product = productDAO.findProduct(code);
        }

        if (product != null) {
            CartInfo cartInfo = Utils.getCartInSession(request);

            ProductInfo productInfo = new ProductInfo(product);
            cartInfo.removeProduct(productInfo);
        }

        return "redirect:/shoppingCart";
    }

    //post: update quantity for product in cart
    @RequestMapping(value = {"/shoppingCart"}, method = RequestMethod.POST)
    public String shoppingCartUpdateQty(HttpServletRequest request,
                                        Model model,
                                        @ModelAttribute("cartForm") CartInfo cartForm) {
        CartInfo cartInfo = Utils.getCartInSession(request);
        cartInfo.updateQuantity(cartForm);

        return "redirect:/shoppingCart";
    }

    //get: show cart
    @RequestMapping(value = {"/shoppingCart"}, method = RequestMethod.GET)
    public String shoppingCartHandler(HttpServletRequest request, Model model) {
        CartInfo myCart = Utils.getCartInSession(request);

        model.addAttribute("cartForm", myCart);
        return "shoppingCart";
    }

    //get: enter customer information
    @RequestMapping(value = {"/shoppingCartCustomer"}, method = RequestMethod.GET)
    public String shoppingCartCustomerForm(HttpServletRequest request, Model model) {
        CartInfo cartInfo = Utils.getCartInSession(request);

        if (cartInfo.isEmpty()) {
            return "/redirect:/shoppingCart";
        }

        CustomerInfo customerInfo = cartInfo.getCustomerInfo();

        CustomerForm customerForm = new CustomerForm(customerInfo);

        model.addAttribute("customerForm", customerForm);

        return "shoppingCartCustomer";
    }

    //post: save customer info
    @RequestMapping(value = {"/shoppingCartCustomer"}, method = RequestMethod.POST)
    public String shoppingCartCustomerSave(HttpServletRequest request,
                                           Model model,
                                           @ModelAttribute("customerForm") @Validated CustomerForm customerForm,
                                           BindingResult result,
                                           final RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            customerForm.setValid(false);
            //forward to reenter customer info
            return "shoppingCartCustomer";
        }

        customerForm.setValid(true);
        CartInfo cartInfo = Utils.getCartInSession(request);
        CustomerInfo customerInfo = new CustomerInfo(customerForm);
        cartInfo.setCustomerInfo(customerInfo);

        return "redirect:/shoppingCartConfirmation";
    }

    //get: show info to confirm
    @RequestMapping(value = {"/shoppingCartConfirmation"}, method = RequestMethod.GET)
    public String shoppingCartConfirmationReview(HttpServletRequest request, Model model) {
        CartInfo cartInfo = Utils.getCartInSession(request);
        if (cartInfo == null || cartInfo.isEmpty()) {
            return "redirect:/shoppingCart";
        } else if (!cartInfo.isValidCustomer()) {
            return "redirect:/shoppingCartCustomer";
        }
        model.addAttribute("myCart", cartInfo);

        return "shoppingCartConfirmation";
    }

    //post: submit cart (save)
    @RequestMapping(value = {"/shoppingCartConfirmation"}, method = RequestMethod.POST)
    public String shoppingCartConfirmationSave(HttpServletRequest request, Model model) {
        CartInfo cartInfo = Utils.getCartInSession(request);
        if (cartInfo.isEmpty()) {
            return "redirect:/shoppingCart";
        } else if (!cartInfo.isValidCustomer()) {
            return "redirect:/shoppingCartCustomer";
        }
        try {
            orderDAO.saveOrder(cartInfo);
        } catch (Exception e) {
            return "shoppingCartConfirmation";
        }

        //remove cart from session
        Utils.removeCartInSession(request);

        //store last cart
        Utils.storeLastOrderedCartInSession(request, cartInfo);

        return "redirect:/shoppingCartFinalize";

    }

    @RequestMapping(value = {"/shoppingCartFinalize"}, method = RequestMethod.GET)
    public String shoppingCartFinalize(HttpServletRequest request, Model model) {
        CartInfo lastOrderedCart = Utils.getLastOrderedCartInSession(request);

        if (lastOrderedCart == null) {
            return "redirect:/shoppingCart";
        }
        model.addAttribute("lastOrderedCart", lastOrderedCart);
        return "shoppingCartFinalize";
    }

    @RequestMapping(value = {"/productImage"}, method = RequestMethod.GET)
    public void productImage(HttpServletRequest request, HttpServletResponse response, Model model,
                             @RequestParam("code") String code) throws IOException{
        Product product = null;
        if (code != null){
            product = this.productDAO.findProduct(code);
        }

        if (product != null && product.getImage() != null){
            response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
            response.getOutputStream().write(product.getImage());
        }
        response.getOutputStream().close();
    }
}
