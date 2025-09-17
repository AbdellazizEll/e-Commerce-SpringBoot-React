package com.ecommerce.project.service;

import com.ecommerce.project.config.util.AuthUtil;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.repositories.cartItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CartServiceImpl  implements CartService{

    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final cartItemRepository cartItemRepository;

    private final ModelMapper modelMapper;

    private final AuthUtil authUtil;
    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {

       Cart cart = createCart();

        Product product = productRepository.findById(productId).orElseThrow( () -> new ResourceNotFoundException("Product","ProductId",productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(
                cart.getCartId(),
                productId
        );
        if(cartItem != null){
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }

        if(product.getQuantity() == 0)
        {
            throw new APIException(product.getProductName() + " is not available ");

        }
        if(product.getQuantity() < quantity)
        {
            throw new APIException("Please , make an order of the " + product.getProductName() + " less than or equal to the quantity" + product.getQuantity() + ".");

        }

        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());
        cartItemRepository.save(newCartItem);
        // Save Cart Item

        product.setQuantity(product.getQuantity());
        cart.setTotalPrice(cart.getTotalPrice() + product.getSpecialPrice() * quantity);

        cartRepository.save(cart);

        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);

        List<CartItem> cartItemList = cart.getItems();
        Stream<ProductDTO> productDTOStream = cartItemList.stream().map(
                item ->
                {
                    ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
                    map.setQuantity(item.getQuantity());
                    return map;
                }
        );

        cartDTO.setProducts(productDTOStream.collect(Collectors.toList()));
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {

        List<Cart> carts = cartRepository.findAll();
        if(carts.size() == 0)
        {
            throw new APIException("No cart exist");
        }
        List<CartDTO> cartDTOS = carts.stream().map(
                cart -> {
                   CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
                   List<ProductDTO> productDTOS = cart.getItems().stream().map(
                           p -> modelMapper.map(p.getProduct(), ProductDTO.class))
                           .toList();

                   cartDTO.setProducts(productDTOS);
                   return cartDTO;
                }
        ).toList();
        return cartDTOS;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {

        Cart cart = cartRepository.findCartByEmailAndCartId(emailId,cartId);
        if(cart == null ){
            throw new ResourceNotFoundException("Cart","CartId",cartId);
        }

        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
        cart.getItems().forEach(c -> c.getProduct().setQuantity(c.getQuantity()));
        List<ProductDTO> products = cart.getItems().stream()
                .map( p -> modelMapper.map(p.getProduct(),ProductDTO.class))
                .toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {

        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart", "CartId", cartId));
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available ");

        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please , make an order of the " + product.getProductName() + " less than or equal to the quantity" + product.getQuantity() + ".");

        }
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId, cartId);
        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " is not available in the cart");
        }

        // Calculate new Quantity
        int newQuantity = cartItem.getQuantity() + quantity;

        if (newQuantity < 0) {
            throw new APIException("The resulting quantity cnanot be negative.");
        }
        // Validation to prevent negative quantities
        if (newQuantity == 0) {
            deleteProductFromCart(cartId, productId);
        } else{

            cartItem.setProductPrice(product.getSpecialPrice());
        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        cartItem.setDiscount(product.getDiscount());
        cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
        cartRepository.save(cart);

         }
        CartItem updatedItem = cartItemRepository.save(cartItem);
        if(updatedItem.getQuantity() == 0) {
            cartItemRepository.deleteById(updatedItem.getCarItemId());
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItems = cart.getItems();
        Stream<ProductDTO> productDTOStream = cartItems.stream().map(
                item -> {
                    ProductDTO prod = modelMapper.map(item.getProduct(), ProductDTO.class);
                    prod.setQuantity(item.getQuantity());

                    return prod;
                }
        );

        cartDTO.setProducts(productDTOStream.toList());

        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow( () -> new ResourceNotFoundException("Cart","cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId, cartId);
        if(cartItem == null)
        {
            throw new ResourceNotFoundException("Product","productId",productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));
        cartRepository.deleteCartItemByProductIdAndCartId(cartId,productId);
        return "Product " + cartItem.getProduct().getProductName() + " has been removed from the cart";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {


        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart", "CartId", cartId));
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));


        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);

        if(cartItem == null)
        {
            throw new APIException("Product " + product.getProductName() + " is not available in the cart");
        }


        // 1000 - 100 * 2 = 800
        double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());

        //200
        cartItem.setProductPrice(product.getSpecialPrice());

        //800 + (200*2) = 1200
        cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItemRepository.save(cartItem);
    }//100*2


    private Cart createCart(){
        Cart userCart = cartRepository.findCartByEmail((authUtil.loggedInEmail()));

        if(userCart != null){
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        Cart newCart = cartRepository.save(cart);
        return newCart;
    }
}
