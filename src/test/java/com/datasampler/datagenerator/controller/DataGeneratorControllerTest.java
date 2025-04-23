package com.datasampler.datagenerator.controller;

import com.datasampler.datagenerator.model.TransactionRecord;
import com.datasampler.datagenerator.service.DataGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataGeneratorController.class)
public class DataGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataGeneratorService dataGeneratorService;

    @Test
    public void testGenerateData() throws Exception {
        // Prepare test data
        LocalDate testDate = LocalDate.of(2023, 1, 15);
        List<TransactionRecord> mockRecords = Arrays.asList(
            TransactionRecord.builder()
                .accountUid("000000123456789012")
                .productCd("CREDIT")
                .txnPostedDate(testDate)
                .txnDate(testDate.minusDays(1))
                .txnType("PURCHASE")
                .amount(new BigDecimal("123.45"))
                .category("Shopping")
                .subCategory("Grocery")
                .txnUid(7890123)
                .tokenizedPan("4111XXXXXXXX1111")
                .last4digitNbr("1111")
                .build()
        );

        // Set primaryKey
        mockRecords.get(0).setPrimaryKey("000000123456789012_CREDIT_2023-01-15_7890123");

        String mockCsv = "primary_key,account_uid,product_cd,txn_posted_date,txn_date,txn_type,amount,category,sub_category,txn_uid,tokenized_pan,last4digitNbr\n" +
                         "000000123456789012_CREDIT_2023-01-15_7890123,000000123456789012,CREDIT,2023-01-15,2023-01-14,PURCHASE,123.45,Shopping,Grocery,7890123,4111XXXXXXXX1111,1111\n";

        // Mock service methods
        when(dataGeneratorService.generateTransactionRecords(anyInt(), anyInt(), any(), any())).thenReturn(mockRecords);
        when(dataGeneratorService.convertToCsv(mockRecords)).thenReturn(mockCsv);

        // Perform request and validate response
        mockMvc.perform(get("/api/data/generate")
                .param("fileType", "CSV")
                .param("dataSampleCount", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(mockCsv));
    }

    @Test
    public void testInvalidSampleCount() throws Exception {
        mockMvc.perform(get("/api/data/generate")
                .param("fileType", "CSV")
                .param("dataSampleCount", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testInvalidUniqueSampleCount() throws Exception {
        mockMvc.perform(get("/api/data/generate")
                .param("fileType", "CSV")
                .param("dataSampleCount", "10")
                .param("uniqueSampleCount", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUniqueSampleCountGreaterThanDataSampleCount() throws Exception {
        mockMvc.perform(get("/api/data/generate")
                .param("fileType", "CSV")
                .param("dataSampleCount", "5")
                .param("uniqueSampleCount", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUnsupportedFileType() throws Exception {
        mockMvc.perform(get("/api/data/generate")
                .param("fileType", "JSON")
                .param("dataSampleCount", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testWithTxnTypeParameter() throws Exception {
        // Prepare test data for FEE transaction type
        LocalDate testDate = LocalDate.of(2023, 1, 15);
        List<TransactionRecord> mockFeeRecords = Arrays.asList(
            TransactionRecord.builder()
                .accountUid("000000123456789012")
                .productCd("CREDIT")
                .txnPostedDate(testDate)
                .txnDate(testDate.minusDays(1))
                .txnType("FEE")
                .amount(new BigDecimal("45.67"))
                .category("Fees and Charges")
                .subCategory("Late Payment Fee")
                .categoryGUID("CAT-00001503")
                .debitCreditIndicator("D")
                .txnUid(7890123)
                .tokenizedPan("4111XXXXXXXX1111")
                .last4digitNbr("1111")
                .build()
        );

        // Set primaryKey
        mockFeeRecords.get(0).setPrimaryKey("000000123456789012_CREDIT_2023-01-15_7890123");

        String mockFeeCsv = "primary_key,account_uid,product_cd,txn_posted_date,txn_date,txn_type,amount,category,sub_category,category_guid,debit_credit_indicator,txn_uid,tokenized_pan,last4digitNbr\n" +
                         "000000123456789012_CREDIT_2023-01-15_7890123,000000123456789012,CREDIT,2023-01-15,2023-01-14,FEE,45.67,Fees and Charges,Late Payment Fee,CAT-00001503,D,7890123,4111XXXXXXXX1111,1111\n";

        // Mock service methods for FEE transaction type
        when(dataGeneratorService.generateTransactionRecords(anyInt(), anyInt(), any(), any())).thenReturn(mockFeeRecords);
        when(dataGeneratorService.convertToCsv(mockFeeRecords)).thenReturn(mockFeeCsv);

        // Perform request with txnType parameter and validate response
        mockMvc.perform(get("/api/data/generate")
                .param("fileType", "CSV")
                .param("dataSampleCount", "1")
                .param("txnType", "FEE"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(mockFeeCsv));
    }

    @Test
    public void testInvalidTxnType() throws Exception {
        mockMvc.perform(get("/api/data/generate")
                .param("fileType", "CSV")
                .param("dataSampleCount", "10")
                .param("txnType", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testWithYearParameter() throws Exception {
        // Prepare test data for year 2024
        LocalDate testDate = LocalDate.of(2024, 6, 15); // June 15, 2024
        List<TransactionRecord> mockYearRecords = Arrays.asList(
            TransactionRecord.builder()
                .accountUid("000000123456789012")
                .productCd("CREDIT")
                .txnPostedDate(testDate)
                .txnDate(testDate.minusDays(1))
                .txnType("PURCHASE")
                .amount(new BigDecimal("123.45"))
                .category("Shopping")
                .subCategory("Grocery")
                .categoryGUID("CAT-00000101")
                .debitCreditIndicator("D")
                .txnUid(7890123)
                .tokenizedPan("4111XXXXXXXX1111")
                .last4digitNbr("1111")
                .build()
        );

        // Set primaryKey
        mockYearRecords.get(0).setPrimaryKey("000000123456789012_CREDIT_2024-06-15_7890123");

        String mockYearCsv = "primary_key,account_uid,product_cd,txn_posted_date,txn_date,txn_type,amount,category,sub_category,category_guid,debit_credit_indicator,txn_uid,tokenized_pan,last4digitNbr\n" +
                         "000000123456789012_CREDIT_2024-06-15_7890123,000000123456789012,CREDIT,2024-06-15,2024-06-14,PURCHASE,123.45,Shopping,Grocery,CAT-00000101,D,7890123,4111XXXXXXXX1111,1111\n";

        // Mock service methods for year parameter
        when(dataGeneratorService.generateTransactionRecords(anyInt(), anyInt(), any(), eq(2024))).thenReturn(mockYearRecords);
        when(dataGeneratorService.convertToCsv(mockYearRecords)).thenReturn(mockYearCsv);

        // Perform request with year parameter and validate response
        mockMvc.perform(get("/api/data/generate")
                .param("fileType", "CSV")
                .param("dataSampleCount", "1")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(mockYearCsv));
    }

    @Test
    public void testInvalidYear() throws Exception {
        mockMvc.perform(get("/api/data/generate")
                .param("fileType", "CSV")
                .param("dataSampleCount", "10")
                .param("year", "1900"))
                .andExpect(status().isBadRequest());
    }
}
