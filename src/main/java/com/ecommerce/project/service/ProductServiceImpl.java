package com.ecommerce.project.service;


import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    CartRepository cartRepository;
    @Autowired
    CartService cartService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final FileService fileService;


    @Value("${project.image}")
    private String path;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, ModelMapper modelMapper, FileService fileService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.modelMapper = modelMapper;
        this.fileService = fileService;
    }

    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productdto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "categoryId", categoryId));


        boolean isProductNotPresent = true;

        List<Product> products = category.getProducts();
        for (Product value : products) {

            if (value.getProductName().equals(productdto.getProductName())) {
                isProductNotPresent = false;
                break;
            }
        }
      if(isProductNotPresent){
          Product product = modelMapper.map(productdto, Product.class);
          product.setImage("default.png");
          product.setCategory(category);
          double specialPrice = product.getPrice() -
                  ((product.getDiscount() * 0.01) * product.getPrice());
          product.setSpecialPrice(specialPrice);
          Product savedProduct = productRepository.save(product);
          return modelMapper.map(savedProduct, ProductDTO.class);
      }else
      {
          throw new APIException("Product already exists");
      }

    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber,
                                          Integer pageSize,String sortBy,String sortOrder) {

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase( "asc")? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Product> productPage = productRepository.findAll(pageable);

        List<Product> products = productPage.getContent();
        if(products.isEmpty()){
            throw new APIException("No Products Found");
        }

        List<ProductDTO> productDTOS = products.stream().map(p -> modelMapper.map(p,ProductDTO.class)).toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());

        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId,Integer pageNumber,
                                            Integer pageSize,String sortBy,String sortOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category","categoryId",categoryId));


        Sort sortByAndOrder = sortOrder.equalsIgnoreCase( "asc")? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Product> productPage = productRepository.findByCategoryOrderByPriceAsc(category,pageable);

        List<Product> products = productPage.getContent();


        List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product,ProductDTO.class))
                .toList();

        if(products.isEmpty()){

            throw new APIException(category.getCategoryName() +"category does not have any products");
        }

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());

        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse searchProductByKeyword(String keyword,Integer pageNumber,
                                                  Integer pageSize,String sortBy,String sortOrder) {


        Sort sortByAndOrder = sortOrder.equalsIgnoreCase( "asc")? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Product> productPage = productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%',pageable);

        List<Product> products = productPage.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product,ProductDTO.class))
                .toList();

        if(products.size()==0){

            throw new APIException("Product Not Found with keyword : "  +  keyword );
        }

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalElements(productPage.getTotalElements());

        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setLastPage(productPage.isLast());
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(ProductDTO productDTO, Long productId) {

        Product productFromDB = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product","ProductId",productId));

        Product product = modelMapper.map(productDTO,Product.class);

        productFromDB.setProductName(product.getProductName());
        productFromDB.setDescription(product.getDescription());
        productFromDB.setQuantity(product.getQuantity());
        productFromDB.setDiscount(product.getDiscount());
        productFromDB.setPrice(product.getPrice());
        productFromDB.setSpecialPrice(product.getSpecialPrice());
        productFromDB.setImage(product.getImage());
        Product productUpdated = productRepository.save(product);

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
            List<ProductDTO> products = cart.getItems().stream()
                    .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class))
                    .toList();
            cartDTO.setProducts(products);
            return cartDTO;
        }).toList();

        cartDTOs.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(),productId) );
        return modelMapper.map(productUpdated, ProductDTO.class);
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow( () -> new ResourceNotFoundException("Product","ProductId",productId));
        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));
        productRepository.delete(product);
        return modelMapper.map(product,ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {

        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        // Upload image to server
        // Get the file name of uploaded image
        String fileName = fileService.uploadImage(path, image);

        // Updating the new file name to the product
        productFromDb.setImage(fileName);

        // Save updated product
        Product updatedProduct = productRepository.save(productFromDb);

        // return DTO after mapping product to DTO
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }




}
