package com.datasampler.datagenerator.controller;

import com.datasampler.datagenerator.model.TransactionRecord;
import com.datasampler.datagenerator.service.DataGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/data")
public class DataGeneratorController {

    private final DataGeneratorService dataGeneratorService;

    @Autowired
    public DataGeneratorController(DataGeneratorService dataGeneratorService) {
        this.dataGeneratorService = dataGeneratorService;
    }

    /**
     * Endpoint to generate and download transaction data in the specified format
     *
     * @param fileType The type of file to generate (currently supports CSV)
     * @param dataSampleCount The number of data samples to generate
     * @param uniqueSampleCount The number of unique composite keys to generate (defaults to dataSampleCount)
     * @return ResponseEntity with the generated file as a downloadable attachment
     */
    @GetMapping("/generate")
    public ResponseEntity<String> generateData(
            @RequestParam(defaultValue = "CSV") String fileType,
            @RequestParam(defaultValue = "100") int dataSampleCount,
            @RequestParam(required = false) Integer uniqueSampleCount) {

        // Validate input parameters
        if (dataSampleCount <= 0) {
            return ResponseEntity.badRequest().body("Data sample count must be greater than 0");
        }

        if (!"CSV".equalsIgnoreCase(fileType)) {
            return ResponseEntity.badRequest().body("Currently only CSV file type is supported");
        }

        // If uniqueSampleCount is not provided, use dataSampleCount
        if (uniqueSampleCount == null) {
            uniqueSampleCount = dataSampleCount;
        }

        // Validate uniqueSampleCount
        if (uniqueSampleCount <= 0) {
            return ResponseEntity.badRequest().body("Unique sample count must be greater than 0");
        }

        if (uniqueSampleCount > dataSampleCount) {
            return ResponseEntity.badRequest().body("Unique sample count cannot be greater than data sample count");
        }

        // Generate transaction records
        List<TransactionRecord> records = dataGeneratorService.generateTransactionRecords(dataSampleCount, uniqueSampleCount);

        // Convert to CSV
        String fileContent = dataGeneratorService.convertToCsv(records);

        // We're removing the BOM as it might cause issues with Mostly AI
        // String bomPrefix = "\uFEFF";
        // fileContent = bomPrefix + fileContent;

        // Generate a filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "transactionsFileSample"  + ".csv";

        // Set headers for file download with UTF-8 encoding
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = new MediaType("text", "csv", StandardCharsets.UTF_8);
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", filename);
        headers.add(HttpHeaders.CONTENT_ENCODING, "UTF-8");

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }
}
