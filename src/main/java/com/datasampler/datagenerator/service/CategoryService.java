package com.datasampler.datagenerator.service;

import com.datasampler.datagenerator.model.Category;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private static final String CATEGORIES_JSON_PATH = "categories.json";
    private static final String UNCATEGORIZED_GUID = "CAT-00000100";

    private List<Category> allCategories;
    private Map<String, Category> categoryGuidMap;
    private Map<String, List<Category>> parentToChildrenMap;
    private Map<String, String> categoryNameToGuidMap;
    private Map<String, String> subcategoryToParentGuidMap;

    @PostConstruct
    public void init() {
        try {
            loadCategories();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load categories from JSON", e);
        }
    }

    private void loadCategories() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource(CATEGORIES_JSON_PATH);

        try (InputStream inputStream = resource.getInputStream()) {
            allCategories = mapper.readValue(inputStream, new TypeReference<List<Category>>() {});

            // Initialize maps for quick lookups
            categoryGuidMap = allCategories.stream()
                    .collect(Collectors.toMap(Category::getCategoryGUID, category -> category));

            // Use a more robust approach for categoryNameToGuidMap to handle potential duplicates
            categoryNameToGuidMap = new HashMap<>();
            for (Category category : allCategories) {
                categoryNameToGuidMap.put(category.getCategory(), category.getCategoryGUID());
            }

            // Create parent to children map
            parentToChildrenMap = new HashMap<>();
            for (Category category : allCategories) {
                String parentGuid = category.getParentCategoryGUID();
                if (!parentGuid.isEmpty()) {
                    parentToChildrenMap.computeIfAbsent(parentGuid, k -> new ArrayList<>()).add(category);
                }
            }

            // Create subcategory to parent GUID map
            subcategoryToParentGuidMap = new HashMap<>();
            for (Category category : allCategories) {
                if (!category.getParentCategoryGUID().isEmpty()) {
                    subcategoryToParentGuidMap.put(category.getCategory(), category.getParentCategoryGUID());
                }
            }
        }
    }

    public String getCategoryGuidByName(String categoryName) {
        return categoryNameToGuidMap.getOrDefault(categoryName, UNCATEGORIZED_GUID);
    }

    public List<Category> getSubcategories(String parentCategoryGuid) {
        return parentToChildrenMap.getOrDefault(parentCategoryGuid, Collections.emptyList());
    }

    public List<Category> getParentCategories() {
        return allCategories.stream()
                .filter(c -> c.getParentCategoryGUID().isEmpty())
                .collect(Collectors.toList());
    }

    public Category getCategoryByGuid(String categoryGuid) {
        return categoryGuidMap.get(categoryGuid);
    }

    public String getRandomSubcategoryGuid(String parentCategoryGuid, Random random) {
        List<Category> subcategories = getSubcategories(parentCategoryGuid);
        if (subcategories.isEmpty()) {
            return UNCATEGORIZED_GUID;
        }
        return subcategories.get(random.nextInt(subcategories.size())).getCategoryGUID();
    }

    public String getRandomParentCategoryGuid(Random random) {
        List<Category> parents = getParentCategories();
        // Filter out Uncategorized for random selection
        parents = parents.stream()
                .filter(c -> !c.getCategoryGUID().equals(UNCATEGORIZED_GUID))
                .collect(Collectors.toList());

        if (parents.isEmpty()) {
            return UNCATEGORIZED_GUID;
        }
        return parents.get(random.nextInt(parents.size())).getCategoryGUID();
    }

    public String getParentGuidForSubcategory(String subcategoryName) {
        return subcategoryToParentGuidMap.getOrDefault(subcategoryName, "");
    }

    public String getUncategorizedGuid() {
        return UNCATEGORIZED_GUID;
    }

    public String getCategoryNameByGuid(String categoryGuid) {
        Category category = categoryGuidMap.get(categoryGuid);
        return category != null ? category.getCategory() : "Uncategorized";
    }
}
