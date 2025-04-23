package com.datasampler.datagenerator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private String categoryGUID;
    private String category;
    private String parentCategoryGUID;
}
