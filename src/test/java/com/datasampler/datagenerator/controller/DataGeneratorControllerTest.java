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
        when(dataGeneratorService.generateTransactionRecords(anyInt(), anyInt())).thenReturn(mockRecords);
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
}
