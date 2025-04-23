package com.datasampler.datagenerator.service;

import com.datasampler.datagenerator.model.TransactionRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CategoryIntegrationTest {

    @Autowired
    private DataGeneratorService dataGeneratorService;

    @Autowired
    private CategoryService categoryService;

    @Test
    public void testCategoryGuidIntegration() {
        // Generate a small number of records
        List<TransactionRecord> records = dataGeneratorService.generateTransactionRecords(10);

        // Verify each record has a categoryGUID
        for (TransactionRecord record : records) {
            assertNotNull(record.getCategoryGUID(), "CategoryGUID should not be null");
            assertTrue(record.getCategoryGUID().startsWith("CAT-"), "CategoryGUID should start with 'CAT-'");

            // Verify the categoryGUID maps to the correct category/subcategory
            String categoryName = categoryService.getCategoryNameByGuid(record.getCategoryGUID());

            // The categoryName should match either the subcategory or the category
            boolean matchesSubcategory = record.getSubCategory().equals(categoryName);
            boolean matchesCategory = record.getCategory().equals(categoryName);

            assertTrue(matchesSubcategory || matchesCategory,
                    "CategoryGUID should map to either the subcategory or category name. " +
                    "Expected: " + record.getSubCategory() + " or " + record.getCategory() +
                    ", but was: " + categoryName);
        }
    }

    @Test
    public void testCsvOutput() {
        // Generate a small number of records
        List<TransactionRecord> records = dataGeneratorService.generateTransactionRecords(5);

        // Convert to CSV
        String csv = dataGeneratorService.convertToCsv(records);

        // Verify CSV header includes category_guid and debit_credit_indicator
        assertTrue(csv.startsWith("primary_key,account_uid,product_cd,txn_posted_date,txn_date,txn_type,amount,category,sub_category,category_guid,debit_credit_indicator,txn_uid,tokenized_pan,last4digitNbr"),
                "CSV header should include category_guid and debit_credit_indicator fields");

        // Verify each record in CSV has a category_guid value
        String[] lines = csv.split("\\n");
        for (int i = 1; i < lines.length; i++) { // Skip header
            String line = lines[i];
            String[] fields = line.split(",");

            // The category_guid should be the 10th field (index 9)
            assertTrue(fields.length > 10, "CSV line should have enough fields");
            String categoryGuid = fields[9].replace("\"", ""); // Remove quotes if present
            assertTrue(categoryGuid.startsWith("CAT-"), "CategoryGUID in CSV should start with 'CAT-'");

            // The debit_credit_indicator should be the 11th field (index 10)
            String debitCreditIndicator = fields[10].replace("\"", ""); // Remove quotes if present
            assertTrue(debitCreditIndicator.equals("D") || debitCreditIndicator.equals("C"),
                    "DebitCreditIndicator should be either 'D' or 'C'");
        }
    }

    @Test
    public void testFeesAndChargesAlwaysHaveFeeTransactionType() {
        // Generate a larger number of records to ensure we get some of each type
        List<TransactionRecord> records = dataGeneratorService.generateTransactionRecords(50);

        // Check each record
        for (TransactionRecord record : records) {
            if ("Fees and Charges".equals(record.getCategory())) {
                assertEquals("FEE", record.getTxnType(),
                        "Transactions with category 'Fees and Charges' should always have transaction type 'FEE'");
            }

            if ("FEE".equals(record.getTxnType())) {
                assertEquals("Fees and Charges", record.getCategory(),
                        "Transactions with type 'FEE' should always have category 'Fees and Charges'");
            }
        }
    }

    @Test
    public void testDebitCreditIndicator() {
        // Generate a larger number of records to ensure we get some of each type
        List<TransactionRecord> records = dataGeneratorService.generateTransactionRecords(50);

        // Check each record
        for (TransactionRecord record : records) {
            assertNotNull(record.getDebitCreditIndicator(), "DebitCreditIndicator should not be null");

            // PURCHASE and FEE should be Debit (D)
            if ("PURCHASE".equals(record.getTxnType()) || "FEE".equals(record.getTxnType())) {
                assertEquals("D", record.getDebitCreditIndicator(),
                        "Transactions with type 'PURCHASE' or 'FEE' should have debitCreditIndicator 'D'");
            }

            // PAYMENT should be Credit (C) and have category Uncategorized
            if ("PAYMENT".equals(record.getTxnType())) {
                assertEquals("C", record.getDebitCreditIndicator(),
                        "Transactions with type 'PAYMENT' should have debitCreditIndicator 'C'");
                assertEquals("Uncategorized", record.getCategory(),
                        "Transactions with type 'PAYMENT' should have category 'Uncategorized'");
                assertEquals("Uncategorized", record.getSubCategory(),
                        "Transactions with type 'PAYMENT' should have subCategory 'Uncategorized'");
                assertEquals(categoryService.getUncategorizedGuid(), record.getCategoryGUID(),
                        "Transactions with type 'PAYMENT' should have the Uncategorized categoryGUID");
            }

            // Verify indicator matches transaction type
            if ("D".equals(record.getDebitCreditIndicator())) {
                assertTrue("PURCHASE".equals(record.getTxnType()) || "FEE".equals(record.getTxnType()),
                        "Transactions with debitCreditIndicator 'D' should have type 'PURCHASE' or 'FEE'");
            }

            if ("C".equals(record.getDebitCreditIndicator())) {
                assertEquals("PAYMENT", record.getTxnType(),
                        "Transactions with debitCreditIndicator 'C' should have type 'PAYMENT'");
            }
        }
    }
}
