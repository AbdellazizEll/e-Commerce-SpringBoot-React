package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    //private List<Category> categories = new ArrayList<>();


    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper ;

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize,String sortBy,String sortOrder) {

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase( "asc")? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Category> categoriePage = categoryRepository.findAll(pageable);
       List<Category> categories = categoriePage.getContent();
       if(categories.isEmpty()){
           throw new APIException("No category created till now.");
       }

       List<CategoryDTO> categoryDTOS = categories.stream().map(c -> modelMapper.map(c,CategoryDTO.class)).toList();

       CategoryResponse categoryResponse = new CategoryResponse();
       categoryResponse.setContent(categoryDTOS);
       categoryResponse.setPageNumber(categoriePage.getNumber());
       categoryResponse.setPageSize(categoriePage.getSize());
        categoryResponse.setTotalElements(categoriePage.getTotalElements());
        categoryResponse.setTotalPages(categoriePage.getTotalPages());
        categoryResponse.setLastPage(!categoriePage.isLast());

        return categoryResponse;

    }

    @Override
    public CategoryDTO createCategory(CategoryDTO category) {
        Category categoryDTO = modelMapper.map(category,Category.class);

        Category categoryfromDB = categoryRepository.findByCategoryName(category.getCategoryName());
        if (categoryfromDB != null) {
            throw new APIException("Category  with the name "+ category.getCategoryName() + "already exists");
        }
Category savedCategory = categoryRepository.save(categoryDTO);
        return modelMapper.map( savedCategory,CategoryDTO.class);
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {


        Category category = categoryRepository.findById(categoryId).orElseThrow( () -> new ResourceNotFoundException("Category","CategoryId",categoryId));


        categoryRepository.delete(category);

        return modelMapper.map(category,CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO category,Long categoryId) {

        Category categoryDTO = modelMapper.map(category,Category.class);

        Category categoryFromDB = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category","CategoryId",categoryId));
      categoryDTO.setCategoryId(categoryId);
      Category categoryUpdated = categoryRepository.save(categoryDTO);
        return modelMapper.map(categoryUpdated,CategoryDTO.class);

    }
}
