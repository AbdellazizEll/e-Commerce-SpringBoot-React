package com.ecommerce.project.controller;


import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;



    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getCategories(@RequestParam(name="pageNumber" , defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
                                                          @RequestParam(name="pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
                                                          @RequestParam(name="sortBy", defaultValue = AppConstants.SORT_CATEGORIES_BY) String sortBy,
                                                          @RequestParam(name="sortOrder",defaultValue = AppConstants.SORT_DIR) String sortOrder) {
        CategoryResponse categories = categoryService.getAllCategories(pageNumber, pageSize,sortBy,sortOrder);
        return new ResponseEntity<>(categories,HttpStatus.OK);
    }


    @PostMapping("/public/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO category) {
      CategoryDTO savedCategoryDTO =   categoryService.createCategory(category);
        return new ResponseEntity<>(savedCategoryDTO,HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable("categoryId") Long categoryId) {

            CategoryDTO statusObj = categoryService.deleteCategory(categoryId);
            return ResponseEntity.status(HttpStatus.OK).body(statusObj);

    }

    @PutMapping("/public/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@RequestBody CategoryDTO category,
                                                 @PathVariable Long categoryId) {
            CategoryDTO savedCategory = categoryService.updateCategory(category,categoryId);
            return new ResponseEntity<>(savedCategory,HttpStatus.OK);


    }

}
