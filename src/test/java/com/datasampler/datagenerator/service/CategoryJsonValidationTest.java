package com.datasampler.datagenerator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryJsonValidationTest {

    private static final String CATEGORIES_JSON_PATH = "categories.json";
    private static final String UNCATEGORIZED_GUID = "CAT-00000100";

    @Test
    public void testCategoriesJsonStructure() throws IOException {
        // Load the JSON file
        List<Map<String, String>> categories = loadCategoriesJson();
        
        // Verify the file is not empty
        assertFalse(categories.isEmpty(), "Categories JSON should not be empty");
        
        // Verify each entry has the required fields
        for (Map<String, String> category : categories) {
            assertTrue(category.containsKey("categoryGUID"), "Each category must have a categoryGUID");
            assertTrue(category.containsKey("category"), "Each category must have a category name");
            assertTrue(category.containsKey("parentCategoryGUID"), "Each category must have a parentCategoryGUID (can be empty)");
            
            // Verify categoryGUID format
            String categoryGUID = category.get("categoryGUID");
            assertTrue(categoryGUID.startsWith("CAT-"), "CategoryGUID must start with 'CAT-'");
            
            // Verify category name is not empty
            assertFalse(category.get("category").isEmpty(), "Category name should not be empty");
        }
    }
    
    @Test
    public void testParentChildRelationships() throws IOException {
        List<Map<String, String>> categories = loadCategoriesJson();
        
        // Collect all categoryGUIDs
        Set<String> allCategoryGUIDs = categories.stream()
                .map(c -> c.get("categoryGUID"))
                .collect(Collectors.toSet());
        
        // Verify parent-child relationships
        for (Map<String, String> category : categories) {
            String parentCategoryGUID = category.get("parentCategoryGUID");
            
            // If parentCategoryGUID is not empty, it should exist in the list of categoryGUIDs
            if (!parentCategoryGUID.isEmpty()) {
                assertTrue(allCategoryGUIDs.contains(parentCategoryGUID), 
                        "ParentCategoryGUID " + parentCategoryGUID + " for " + category.get("category") + 
                        " must exist as a categoryGUID in the list");
            }
        }
    }
    
    @Test
    public void testUniqueCategories() throws IOException {
        List<Map<String, String>> categories = loadCategoriesJson();
        
        // Verify unique categoryGUIDs
        Set<String> categoryGUIDs = new HashSet<>();
        for (Map<String, String> category : categories) {
            String guid = category.get("categoryGUID");
            assertFalse(categoryGUIDs.contains(guid), 
                    "CategoryGUID " + guid + " is duplicated for category " + category.get("category"));
            categoryGUIDs.add(guid);
        }
        
        // Verify unique category names within the same parent
        Map<String, Set<String>> parentToChildCategories = new HashMap<>();
        for (Map<String, String> category : categories) {
            String parentGUID = category.get("parentCategoryGUID");
            String categoryName = category.get("category");
            
            parentToChildCategories.putIfAbsent(parentGUID, new HashSet<>());
            Set<String> siblings = parentToChildCategories.get(parentGUID);
            
            assertFalse(siblings.contains(categoryName), 
                    "Category name " + categoryName + " is duplicated under parent " + parentGUID);
            siblings.add(categoryName);
        }
    }
    
    @Test
    public void testParentCategoryRequirements() throws IOException {
        List<Map<String, String>> categories = loadCategoriesJson();
        
        // Count parent categories (those with empty parentCategoryGUID)
        List<Map<String, String>> parentCategories = categories.stream()
                .filter(c -> c.get("parentCategoryGUID").isEmpty())
                .collect(Collectors.toList());
        
        // Verify we have at least 15 parent categories
        assertTrue(parentCategories.size() >= 15, 
                "There should be at least 15 parent categories, found " + parentCategories.size());
        
        // Verify each parent category has subcategories
        for (Map<String, String> parentCategory : parentCategories) {
            String parentGUID = parentCategory.get("categoryGUID");
            
            // Skip this check for Uncategorized
            if (parentGUID.equals(UNCATEGORIZED_GUID)) {
                continue;
            }
            
            // Count subcategories for this parent
            long subcategoryCount = categories.stream()
                    .filter(c -> c.get("parentCategoryGUID").equals(parentGUID))
                    .count();
            
            assertTrue(subcategoryCount >= 3 && subcategoryCount <= 5, 
                    "Parent category " + parentCategory.get("category") + " should have 3-5 subcategories, found " + subcategoryCount);
        }
    }
    
    @Test
    public void testUncategorizedCategory() throws IOException {
        List<Map<String, String>> categories = loadCategoriesJson();
        
        // Find the Uncategorized category
        Optional<Map<String, String>> uncategorized = categories.stream()
                .filter(c -> c.get("categoryGUID").equals(UNCATEGORIZED_GUID))
                .findFirst();
        
        // Verify it exists
        assertTrue(uncategorized.isPresent(), "Uncategorized category with GUID " + UNCATEGORIZED_GUID + " should exist");
        
        // Verify its properties
        Map<String, String> uncategorizedCategory = uncategorized.get();
        assertEquals("Uncategorized", uncategorizedCategory.get("category"), 
                "Category with GUID " + UNCATEGORIZED_GUID + " should be named 'Uncategorized'");
        assertEquals("", uncategorizedCategory.get("parentCategoryGUID"), 
                "Uncategorized category should have empty parentCategoryGUID");
    }
    
    private List<Map<String, String>> loadCategoriesJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource(CATEGORIES_JSON_PATH);
        
        try (InputStream inputStream = resource.getInputStream()) {
            return mapper.readValue(inputStream, new TypeReference<List<Map<String, String>>>() {});
        }
    }
}
