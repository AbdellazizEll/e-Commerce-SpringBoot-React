package com.ecommerce.project.controller;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.service.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@NoArgsConstructor
public class ProductController {


    @Autowired
    private ProductService productService;


    @PostMapping("/admin/categories/{categoryId}/product")
    public ResponseEntity<ProductDTO> addProduct(@Valid @RequestBody ProductDTO product,
                                                 @PathVariable Long categoryId){


        ProductDTO productDTO = productService.addProduct(categoryId, product);

        return new ResponseEntity<>(productDTO, HttpStatus.CREATED);
    }

    @GetMapping("/public/product")
    public ResponseEntity<ProductResponse> getAllProduct(@RequestParam(name="pageNumber" , defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
                                                         @RequestParam(name="pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
                                                         @RequestParam(name="sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY) String sortBy,
                                                         @RequestParam(name="sortOrder",defaultValue = AppConstants.SORT_DIR) String sortOrder)
    {
       ProductResponse productResponse =  productService.getAllProducts(pageNumber,pageSize,sortBy,sortOrder);

       return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }


    @GetMapping("/public/categories/{categoryId}/products")
    public ResponseEntity<ProductResponse> getProductsByCategory(@PathVariable Long categoryId,
                                                                 @RequestParam(name="pageNumber" , defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
                                                                 @RequestParam(name="pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
                                                                 @RequestParam(name="sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY) String sortBy,
                                                                 @RequestParam(name="sortOrder",defaultValue = AppConstants.SORT_DIR) String sortOrder)
    {
        ProductResponse productResponse = productService.searchByCategory(categoryId,pageNumber,pageSize,sortBy,sortOrder);

        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping("/public/products/keyword/{keyword}")
    public ResponseEntity<ProductResponse> getProductsByKeyword(@PathVariable String keyword,
                                                                @RequestParam(name="pageNumber" , defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
                                                                @RequestParam(name="pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
                                                                @RequestParam(name="sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY) String sortBy,
                                                                @RequestParam(name="sortOrder",defaultValue = AppConstants.SORT_DIR) String sortOrder)
    {
       ProductResponse response = productService.searchProductByKeyword(keyword,pageNumber,pageSize,sortBy,sortOrder);

        return new ResponseEntity<>(response, HttpStatus.FOUND);

    }

    @PutMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDTO> updateProduct(@Valid @RequestBody ProductDTO productDTO,
                                                      @PathVariable Long productId) {
        ProductDTO savedProduct = productService.updateProduct(productDTO,productId);
        return new ResponseEntity<>(savedProduct,HttpStatus.OK);


    }

    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable("productId") Long productId) {

        ProductDTO statusObj = productService.deleteProduct(productId);
        return ResponseEntity.status(HttpStatus.OK).body(statusObj);

    }

    @PutMapping("/products/{productId}/image")
    public ResponseEntity<ProductDTO> updateProductImage(@PathVariable Long productId,
                                                         @RequestParam("Image") MultipartFile image) throws IOException {

        ProductDTO updatedProduct =  productService.updateProductImage(productId, image);
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);

    }

}
