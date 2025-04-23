package com.datasampler.datagenerator.service;

import com.datasampler.datagenerator.model.TransactionRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DataGeneratorServiceTest {

    @Autowired
    private DataGeneratorService service;

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

            // Verify transaction type is either PURCHASE, FEE, or PAYMENT
            assertTrue(record.getTxnType().equals("PURCHASE") || record.getTxnType().equals("FEE") || record.getTxnType().equals("PAYMENT"));

            // Verify category and subCategory combinations
            if ("PURCHASE".equals(record.getTxnType())) {
                // For PURCHASE transactions - just verify category and subcategory are not null
                assertNotNull(record.getCategory());
                assertNotNull(record.getSubCategory());
                assertEquals("D", record.getDebitCreditIndicator());
            } else if ("FEE".equals(record.getTxnType())) {
                // For FEE transactions
                assertEquals("Fees and Charges", record.getCategory());
                assertTrue(Arrays.asList("Card Payment", "Returns", "Late Payment Fee", "Annual Fee & Charges").contains(record.getSubCategory()));
                assertEquals("D", record.getDebitCreditIndicator());
            } else if ("PAYMENT".equals(record.getTxnType())) {
                // For PAYMENT transactions
                assertEquals("Uncategorized", record.getCategory());
                assertEquals("Uncategorized", record.getSubCategory());
                assertEquals("C", record.getDebitCreditIndicator());
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

        // Use a specific year parameter to ensure consistent date generation
        int testYear = 2024;
        List<TransactionRecord> records = service.generateTransactionRecords(dataSampleCount, uniqueSampleCount, null, testYear);

        // Verify the correct number of records was generated
        assertEquals(dataSampleCount, records.size());

        // Count unique primary keys instead of account/product/date combinations
        // This is more reliable since our primary key generation ensures uniqueness
        long uniquePrimaryKeys = records.stream()
                .map(TransactionRecord::getPrimaryKey)
                .distinct()
                .count();

        // Print the unique combinations for debugging
        System.out.println("Expected unique primary keys: " + dataSampleCount);
        System.out.println("Actual unique primary keys: " + uniquePrimaryKeys);

        // Verify each record has a unique primary key
        assertEquals(dataSampleCount, uniquePrimaryKeys, "Each record should have a unique primary key");

        // Count unique account/product combinations (should be limited by uniqueSampleCount)
        long uniqueAccountProductCombinations = records.stream()
                .map(r -> r.getAccountUid() + "_" + r.getProductCd())
                .distinct()
                .count();

        // Verify we have at most uniqueSampleCount unique account/product combinations
        assertTrue(uniqueAccountProductCombinations <= uniqueSampleCount,
                "Should have at most " + uniqueSampleCount + " unique account/product combinations");
    }

    @Test
    public void testGenerateTransactionRecordsWithYearParameter() {
        // Test with year parameter
        int testYear = 2024;
        List<TransactionRecord> yearRecords = service.generateTransactionRecords(20, 10, null, testYear);
        assertEquals(20, yearRecords.size(), "Should generate 20 records");

        // Verify all records have posted dates in the specified year
        for (TransactionRecord record : yearRecords) {
            assertEquals(testYear, record.getTxnPostedDate().getYear(),
                    "All records should have posted dates in year " + testYear);

            // Transaction date should be on or before posted date
            assertTrue(record.getTxnDate().isBefore(record.getTxnPostedDate()) ||
                       record.getTxnDate().isEqual(record.getTxnPostedDate()),
                    "Transaction date should be on or before posted date");

            // Transaction date should also be in the specified year
            assertEquals(testYear, record.getTxnDate().getYear(),
                    "All records should have transaction dates in year " + testYear);
        }

        // Test with both year and txnType parameters
        List<TransactionRecord> yearAndTypeRecords = service.generateTransactionRecords(20, 10, "PURCHASE", testYear);
        assertEquals(20, yearAndTypeRecords.size(), "Should generate 20 records");

        // Verify all records have posted dates in the specified year and the specified transaction type
        for (TransactionRecord record : yearAndTypeRecords) {
            assertEquals(testYear, record.getTxnPostedDate().getYear(),
                    "All records should have posted dates in year " + testYear);
            assertEquals("PURCHASE", record.getTxnType(),
                    "All records should have PURCHASE transaction type");
        }
    }

    @Test
    public void testGenerateTransactionRecordsWithCurrentYear() {
        // Use the current year explicitly to avoid test failures when transaction dates
        // cross year boundaries (e.g., for dates in early January)
        int currentYear = LocalDate.now().getYear();

        // Test with explicit current year parameter
        List<TransactionRecord> currentYearRecords = service.generateTransactionRecords(20, 10, null, currentYear);
        assertEquals(20, currentYearRecords.size(), "Should generate 20 records");

        LocalDate today = LocalDate.now();

        // Verify all records have posted dates in the current year and up to yesterday
        for (TransactionRecord record : currentYearRecords) {
            assertEquals(currentYear, record.getTxnPostedDate().getYear(),
                    "All records should have posted dates in current year");

            // Posted date should be on or before today
            assertTrue(record.getTxnPostedDate().isBefore(today.plusDays(1)),
                    "Posted date should be on or before today");

            // Transaction date should be on or before posted date
            assertTrue(record.getTxnDate().isBefore(record.getTxnPostedDate()) ||
                       record.getTxnDate().isEqual(record.getTxnPostedDate()),
                    "Transaction date should be on or before posted date");

            // Transaction date should also be in the current year
            assertEquals(currentYear, record.getTxnDate().getYear(),
                    "All records should have transaction dates in current year");
        }

        // Also test with no year parameter (should use current year)
        List<TransactionRecord> implicitYearRecords = service.generateTransactionRecords(5, 5);

        // Verify all records have posted dates in the current year
        for (TransactionRecord record : implicitYearRecords) {
            int recordYear = record.getTxnPostedDate().getYear();
            assertEquals(currentYear, recordYear,
                    "All records should have posted dates in current year");

            // Transaction date should be in the same year as posted date
            int txnYear = record.getTxnDate().getYear();
            assertEquals(recordYear, txnYear,
                    "Transaction date should be in the same year as posted date");
        }
    }

    @Test
    public void testGenerateTransactionRecordsWithTxnTypeFilter() {
        // Test with PURCHASE transaction type
        List<TransactionRecord> purchaseRecords = service.generateTransactionRecords(20, 10, "PURCHASE");
        assertEquals(20, purchaseRecords.size(), "Should generate 20 records");

        // Verify all records have PURCHASE transaction type
        for (TransactionRecord record : purchaseRecords) {
            assertEquals("PURCHASE", record.getTxnType(), "All records should have PURCHASE transaction type");
            assertEquals("D", record.getDebitCreditIndicator(), "PURCHASE transactions should have debitCreditIndicator 'D'");
        }

        // Test with FEE transaction type
        List<TransactionRecord> feeRecords = service.generateTransactionRecords(20, 10, "FEE");
        assertEquals(20, feeRecords.size(), "Should generate 20 records");

        // Verify all records have FEE transaction type
        for (TransactionRecord record : feeRecords) {
            assertEquals("FEE", record.getTxnType(), "All records should have FEE transaction type");
            assertEquals("Fees and Charges", record.getCategory(), "FEE transactions should have category 'Fees and Charges'");
            assertEquals("D", record.getDebitCreditIndicator(), "FEE transactions should have debitCreditIndicator 'D'");
        }

        // Test with PAYMENT transaction type
        List<TransactionRecord> paymentRecords = service.generateTransactionRecords(20, 10, "PAYMENT");
        assertEquals(20, paymentRecords.size(), "Should generate 20 records");

        // Verify all records have PAYMENT transaction type
        for (TransactionRecord record : paymentRecords) {
            assertEquals("PAYMENT", record.getTxnType(), "All records should have PAYMENT transaction type");
            assertEquals("Uncategorized", record.getCategory(), "PAYMENT transactions should have category 'Uncategorized'");
            assertEquals("Uncategorized", record.getSubCategory(), "PAYMENT transactions should have subCategory 'Uncategorized'");
            assertEquals("C", record.getDebitCreditIndicator(), "PAYMENT transactions should have debitCreditIndicator 'C'");
        }
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
        String expectedHeader = "primary_key,account_uid,product_cd,txn_posted_date,txn_date,txn_type,amount,category,sub_category,category_guid,debit_credit_indicator,txn_uid,tokenized_pan,last4digitNbr";
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
