package com.datasampler.datagenerator.service;

import com.datasampler.datagenerator.model.TransactionRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataGeneratorServiceTest {

    private final DataGeneratorService service = new DataGeneratorService();

    @Test
    public void testGenerateTransactionRecords() {
        testBasicTransactionRecordGeneration();
        testUniqueTransactionRecordGeneration();
    }

    private void testBasicTransactionRecordGeneration() {
        // Test generating a specific number of records
        int count = 5;
        List<TransactionRecord> records = service.generateTransactionRecords(count, count);

        // Verify the correct number of records was generated
        assertEquals(count, records.size());

        // Verify primaryKey is set for each record
        for (TransactionRecord record : records) {
            assertNotNull(record.getPrimaryKey());
            String expectedPrimaryKey = record.getAccountUid() + "_" + record.getProductCd() + "_" +
                                       record.getTxnPostedDate() + "_" + record.getTxnUid();
            assertEquals(expectedPrimaryKey, record.getPrimaryKey());
        }

        // Verify each record has valid data
        for (TransactionRecord record : records) {
            assertNotNull(record.getAccountUid());
            assertNotNull(record.getProductCd());
            assertNotNull(record.getTxnPostedDate());
            assertNotNull(record.getTxnDate());
            assertNotNull(record.getTxnType());
            assertNotNull(record.getAmount());
            assertNotNull(record.getCategory());
            assertNotNull(record.getSubCategory());
            assertNotNull(record.getTxnUid());
            assertNotNull(record.getTokenizedPan());
            assertNotNull(record.getLast4digitNbr());

            // Verify transaction type is either PURCHASE or FEE
            assertTrue(record.getTxnType().equals("PURCHASE") || record.getTxnType().equals("FEE"));

            // Verify category and subCategory combinations
            if ("PURCHASE".equals(record.getTxnType())) {
                // For PURCHASE transactions - just verify category and subcategory are not null
                assertNotNull(record.getCategory());
                assertNotNull(record.getSubCategory());
            } else {
                // For FEE transactions
                assertEquals("Fees and Charges", record.getCategory());
                assertTrue(Arrays.asList("Card Payment", "Returns", "Late Payment Fee", "Annual Fee & Charges").contains(record.getSubCategory()));
            }

            // Verify tokenized PAN format
            assertEquals(16, record.getTokenizedPan().length());

            // Verify last 4 digits format
            assertEquals(4, record.getLast4digitNbr().length());

            // Verify last 4 digits match the last 4 of tokenized PAN
            assertEquals(record.getTokenizedPan().substring(12), record.getLast4digitNbr());
        }
    }

    private void testUniqueTransactionRecordGeneration() {
        // Test generating records with uniqueSampleCount < dataSampleCount
        int dataSampleCount = 10;
        int uniqueSampleCount = 5;

        List<TransactionRecord> records = service.generateTransactionRecords(dataSampleCount, uniqueSampleCount);

        // Verify the correct number of records was generated
        assertEquals(dataSampleCount, records.size());

        // Verify we have the correct number of unique account/product/date combinations
        long uniqueCombinations = records.stream()
                .map(r -> r.getAccountUid() + "_" + r.getProductCd() + "_" + r.getTxnPostedDate())
                .distinct()
                .count();

        // Print the unique combinations for debugging
        System.out.println("Expected unique combinations: " + uniqueSampleCount);
        System.out.println("Actual unique combinations: " + uniqueCombinations);
        records.stream()
                .map(r -> r.getAccountUid() + "_" + r.getProductCd() + "_" + r.getTxnPostedDate())
                .distinct()
                .forEach(System.out::println);

        assertEquals(uniqueSampleCount, uniqueCombinations);
    }

    @Test
    public void testConvertToCsv() {
        // Create a test record
        LocalDate testDate = LocalDate.of(2023, 1, 15);
        TransactionRecord record = TransactionRecord.builder()
                .accountUid("000000123456789012")
                .productCd("CREDIT")
                .txnPostedDate(testDate)
                .txnDate(testDate.minusDays(1))
                .txnType("PURCHASE")
                .amount(new BigDecimal("123.45"))
                .category("Shopping")
                .subCategory("Grocery")
                .txnUid(7890123)
                .tokenizedPan("4111ABCDEF1111")
                .last4digitNbr("1111")
                .primaryKey("000000123456789012_CREDIT_2023-01-15_7890123")
                .build();

        List<TransactionRecord> records = List.of(record);

        // Convert to CSV
        String csv = service.convertToCsv(records);

        // Verify CSV format
        String expectedHeader = "primary_key,account_uid,product_cd,txn_posted_date,txn_date,txn_type,amount,category,sub_category,txn_uid,tokenized_pan,last4digitNbr";
        assertTrue(csv.startsWith(expectedHeader));

        // Verify each field is in the CSV (not checking exact format due to potential quoting)
        assertTrue(csv.contains("000000123456789012"));
        assertTrue(csv.contains("CREDIT"));
        assertTrue(csv.contains(testDate.toString()));
        assertTrue(csv.contains(testDate.minusDays(1).toString()));
        assertTrue(csv.contains("PURCHASE"));
        assertTrue(csv.contains("123.45"));
        assertTrue(csv.contains("Shopping"));
        assertTrue(csv.contains("Grocery"));
        assertTrue(csv.contains("7890123"));
        assertTrue(csv.contains("4111ABCDEF1111"));
        assertTrue(csv.contains("1111"));
    }
}
