package com.datasampler.datagenerator.service;

import com.datasampler.datagenerator.model.TransactionRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DataGeneratorService {

    private static final String[] PRODUCT_CODES = {"CREDIT"};
    private static final Random RANDOM = new Random();
    private static final AtomicInteger TXN_UID_GENERATOR = new AtomicInteger(1000000);

    // Set to track recently used categories to avoid repetition
    private final Set<String> usedCategories = new HashSet<>();

    /**
     * Generates a list of random transaction records
     *
     * @param dataSampleCount Number of records to generate per unique sample
     * @param uniqueSampleCount Number of unique composite keys to generate
     * @return List of generated transaction records
     */
    public List<TransactionRecord> generateTransactionRecords(int dataSampleCount, int uniqueSampleCount) {
        List<TransactionRecord> records = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();

        // Generate records with unique composite keys
        while (uniqueKeys.size() < uniqueSampleCount) {
            TransactionRecord record = generateRandomTransactionRecord();

            // Create a composite key from accountUid, productCd, txnPostedDate, and txnUid
            String compositeKey = record.getAccountUid() + "_" +
                                 record.getProductCd() + "_" +
                                 record.getTxnPostedDate() + "_" +
                                 record.getTxnUid();

            // Set the primaryKey field
            record.setPrimaryKey(compositeKey);

            // Only add the record if the composite key is unique
            if (uniqueKeys.add(compositeKey)) {
                records.add(record);
            }
        }

        // If dataSampleCount > uniqueSampleCount, create related records with the same primary key but varied details
        if (dataSampleCount > uniqueSampleCount) {
            int initialSize = records.size();
            for (int i = 0; i < dataSampleCount - initialSize; i++) {
                // Select a random record from the unique set to use as a base
                TransactionRecord originalRecord = records.get(RANDOM.nextInt(initialSize));

                // Create a new txnUid
                int newTxnUid = TXN_UID_GENERATOR.incrementAndGet();

                // Vary the transaction date (within 7 days of the original)
                LocalDate originalTxnDate = originalRecord.getTxnDate();
                LocalDate newTxnDate = originalTxnDate.plusDays(RANDOM.nextInt(7) - 3); // -3 to +3 days

                // Ensure txnDate is not after txnPostedDate
                if (newTxnDate.isAfter(originalRecord.getTxnPostedDate())) {
                    newTxnDate = originalRecord.getTxnPostedDate();
                }

                // For related records, either vary the original amount or generate a completely new amount
                BigDecimal newAmount;
                if (RANDOM.nextDouble() < 0.5) {
                    // 50% chance: Generate a completely new random amount between 1 and 10,000
                    double amount = 1 + (RANDOM.nextDouble() * 9999);
                    newAmount = new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
                } else {
                    // 50% chance: Vary the original amount (within 50% of the original)
                    BigDecimal originalAmount = originalRecord.getAmount();
                    double variationFactor = 0.5 + (RANDOM.nextDouble() * 1.0); // 0.5 to 1.5 (±50%)
                    newAmount = originalAmount.multiply(new BigDecimal(variationFactor))
                            .setScale(2, BigDecimal.ROUND_HALF_UP);
                }

                // Randomize category and subcategory based on transaction type
                String txnType = originalRecord.getTxnType();
                String category;
                String subCategory;

                // For PURCHASE transactions, fully randomize categories and subcategories
                if ("PURCHASE".equals(txnType)) {
                    // 70% chance to completely change the category
                    if (RANDOM.nextDouble() < 0.7) {
                        // Generate a completely new random category
                        category = generateRandomCategory(txnType);
                        // Generate a matching subcategory
                        subCategory = generateRandomSubCategory(txnType, category);
                    } else {
                        // Keep the original category but change subcategory
                        category = originalRecord.getCategory();
                        subCategory = generateRandomSubCategory(txnType, category);
                    }
                } else if ("FEE".equals(txnType)) {
                    // For FEE transactions, keep the category as "Fees and Charges" but randomize subcategory
                    category = "Fees and Charges";
                    subCategory = generateRandomSubCategory(txnType, category);
                } else {
                    // Fallback - keep original values
                    category = originalRecord.getCategory();
                    subCategory = originalRecord.getSubCategory();
                }

                TransactionRecord relatedRecord = TransactionRecord.builder()
                    .accountUid(originalRecord.getAccountUid())
                    .productCd(originalRecord.getProductCd())
                    .txnPostedDate(originalRecord.getTxnPostedDate()) // Keep the same posted date for the primary key
                    .txnDate(newTxnDate) // Varied transaction date
                    .txnType(txnType)
                    .amount(newAmount) // Varied amount
                    .category(category)
                    .subCategory(subCategory) // Potentially varied subcategory
                    .txnUid(newTxnUid)
                    .tokenizedPan(originalRecord.getTokenizedPan())
                    .last4digitNbr(originalRecord.getLast4digitNbr())
                    .primaryKey(originalRecord.getAccountUid() + "_" +
                               originalRecord.getProductCd() + "_" +
                               originalRecord.getTxnPostedDate() + "_" +
                               newTxnUid) // Create a new primary key with the new txnUid
                    .build();

                records.add(relatedRecord);
            }
        }

        return records;
    }

    /**
     * Generates a list of random transaction records (legacy method for backward compatibility)
     *
     * @param count Number of records to generate
     * @return List of generated transaction records
     */
    public List<TransactionRecord> generateTransactionRecords(int count) {
        return generateTransactionRecords(count, count);
    }

    /**
     * Generates a single random transaction record
     *
     * @return A randomly generated transaction record
     */
    private TransactionRecord generateRandomTransactionRecord() {
        LocalDate postedDate = generateRandomDate();
        // Transaction date is usually on or before the posted date
        LocalDate txnDate = postedDate.minusDays(RANDOM.nextInt(3)); // 0-2 days before posted date

        String tokenizedPan = generateRandomTokenizedPan();

        String txnType = generateRandomTxnType();
        String category = generateRandomCategory(txnType);
        String subCategory = generateRandomSubCategory(txnType, category);

        String accountUid = generateRandomAccountUid();
        String productCd = generateRandomProductCd();
        int txnUid = TXN_UID_GENERATOR.incrementAndGet();

        // Create the primary key
        String primaryKey = accountUid + "_" + productCd + "_" + postedDate + "_" + txnUid;

        return TransactionRecord.builder()
                .accountUid(accountUid)
                .productCd(productCd)
                .txnPostedDate(postedDate)
                .txnDate(txnDate)
                .txnType(txnType)
                .amount(generateRandomAmount())
                .category(category)
                .subCategory(subCategory)
                .txnUid(txnUid)
                .tokenizedPan(tokenizedPan)
                .last4digitNbr(tokenizedPan.substring(tokenizedPan.length() - 4))
                .primaryKey(primaryKey)
                .build();
    }

    private String generateRandomAccountUid() {
        // First 6 digits are zeros
        StringBuilder sb = new StringBuilder("000000");

        // Generate 14 random digits
        for (int i = 0; i < 14; i++) {
            sb.append(RANDOM.nextInt(10));
        }

        return sb.toString();
    }

    private String generateRandomProductCd() {
        return PRODUCT_CODES[RANDOM.nextInt(PRODUCT_CODES.length)];
    }

    private LocalDate generateRandomDate() {
        // Calculate date 24 months ago from today
        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusMonths(24);

        long minDay = minDate.toEpochDay();
        long maxDay = today.toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        return LocalDate.ofEpochDay(randomDay);
    }

    // LocalTime no longer needed as we're using LocalDate

    private String generateRandomTxnType() {
        // 80% chance of PURCHASE, 20% chance of FEE
        return RANDOM.nextDouble() < 0.8 ? "PURCHASE" : "FEE";
    }

    private BigDecimal generateRandomAmount() {
        // Generate amount between 1 and 10,000
        double amount = 1 + (RANDOM.nextDouble() * 9999);
        return new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private String generateRandomCategory(String txnType) {
        if ("PURCHASE".equals(txnType)) {
            // Expanded categories for PURCHASE transactions
            String[] categories = {
                "Shopping", "Entertainment", "Travel", "Government Services",
                "Uncategorized", "Home & Garden", "Dining", "Education",
                "Healthcare", "Automotive", "Utilities", "Charity",
                "Professional Services", "Insurance", "Subscriptions"
            };

            // Try to find a category that hasn't been used recently
            String selectedCategory;

            // First try to find an unused category
            do {
                selectedCategory = categories[RANDOM.nextInt(categories.length)];
                // Break after a few attempts to avoid infinite loop
                if (RANDOM.nextInt(5) == 0) break;
            } while (usedCategories.contains(selectedCategory) && usedCategories.size() < categories.length);

            // Add to used categories (limit size to avoid memory issues)
            if (usedCategories.size() >= 10) {
                usedCategories.clear();
            }
            usedCategories.add(selectedCategory);

            return selectedCategory;
        } else {
            // Category for FEE transactions
            return "Fees and Charges";
        }
    }

    private String generateRandomSubCategory(String txnType, String category) {
        if ("PURCHASE".equals(txnType)) {
            // SubCategories based on category for PURCHASE transactions
            switch (category) {
                case "Shopping":
                    String[] shoppingSubCategories = {"Grocery", "Homegoods", "Cloths", "Books", "Uncategorized", "Electronics & Tech"};
                    return shoppingSubCategories[RANDOM.nextInt(shoppingSubCategories.length)];
                case "Entertainment":
                    String[] entertainmentSubCategories = {"OtpPlatform", "Cinemas", "Games", "Music & Shows"};
                    return entertainmentSubCategories[RANDOM.nextInt(entertainmentSubCategories.length)];
                case "Travel":
                    String[] travelSubCategories = {"Flight Booking", "Bus Booking", "Hotel and Stay", "Space Travel", "Car Rental & Taxi"};
                    return travelSubCategories[RANDOM.nextInt(travelSubCategories.length)];
                case "Government Services":
                    String[] governmentSubCategories = {"Tax payment", "State Service Payment", "Park Tickets", "License & Permits"};
                    return governmentSubCategories[RANDOM.nextInt(governmentSubCategories.length)];
                case "Uncategorized":
                    return "Uncategorized";
                case "Home & Garden":
                    String[] homeSubCategories = {"Furniture", "Gardening", "Home Improvement", "Decor & Accessories"};
                    return homeSubCategories[RANDOM.nextInt(homeSubCategories.length)];
                case "Dining":
                    String[] diningSubCategories = {"Restaurant", "Fast Food", "Cafe", "Delivery", "Bar & Pub"};
                    return diningSubCategories[RANDOM.nextInt(diningSubCategories.length)];
                case "Education":
                    String[] educationSubCategories = {"Tuition", "Books & Supplies", "Online Courses", "Workshops", "Student Loans"};
                    return educationSubCategories[RANDOM.nextInt(educationSubCategories.length)];
                case "Healthcare":
                    String[] healthcareSubCategories = {"Doctor Visit", "Pharmacy", "Hospital", "Dental", "Vision", "Insurance"};
                    return healthcareSubCategories[RANDOM.nextInt(healthcareSubCategories.length)];
                case "Automotive":
                    String[] automotiveSubCategories = {"Fuel", "Maintenance", "Purchase", "Rental", "Insurance", "Parking"};
                    return automotiveSubCategories[RANDOM.nextInt(automotiveSubCategories.length)];
                case "Utilities":
                    String[] utilitiesSubCategories = {"Electricity", "Water", "Gas", "Internet", "Phone", "Cable TV"};
                    return utilitiesSubCategories[RANDOM.nextInt(utilitiesSubCategories.length)];
                case "Charity":
                    String[] charitySubCategories = {"Donation", "Fundraiser", "Volunteer", "Non-profit", "Community Service"};
                    return charitySubCategories[RANDOM.nextInt(charitySubCategories.length)];
                case "Professional Services":
                    String[] professionalSubCategories = {"Legal", "Accounting", "Consulting", "Financial Advisor", "Real Estate"};
                    return professionalSubCategories[RANDOM.nextInt(professionalSubCategories.length)];
                case "Insurance":
                    String[] insuranceSubCategories = {"Health", "Auto", "Home", "Life", "Travel", "Pet"};
                    return insuranceSubCategories[RANDOM.nextInt(insuranceSubCategories.length)];
                case "Subscriptions":
                    String[] subscriptionSubCategories = {"Streaming", "Software", "Magazine", "Membership", "Box Service"};
                    return subscriptionSubCategories[RANDOM.nextInt(subscriptionSubCategories.length)];
                default:
                    return "Uncategorized";
            }
        } else {
            // SubCategories for FEE transactions
            String[] feeSubCategories = {"Card Payment", "Returns", "Late Payment Fee", "Annual Fee & Charges"};
            return feeSubCategories[RANDOM.nextInt(feeSubCategories.length)];
        }
    }

    // No longer needed as we're using TXN_UID_GENERATOR.incrementAndGet()

    private String generateRandomTokenizedPan() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (i > 5 && i < 12) {
                sb.append('X');
            } else {
                sb.append(RANDOM.nextInt(10));
            }
        }
        return sb.toString();
    }

    // No longer needed as we're extracting last4digitNbr from tokenizedPan

    /**
     * Converts a list of transaction records to CSV format
     *
     * @param records List of transaction records
     * @return CSV formatted string
     */
    public String convertToCsv(List<TransactionRecord> records) {
        StringBuilder csv = new StringBuilder();

        // Add header
        csv.append("primary_key,account_uid,product_cd,txn_posted_date,txn_date,txn_type,amount,category,sub_category,txn_uid,tokenized_pan,last4digitNbr\n");

        // Add records
        for (TransactionRecord record : records) {
            csv.append(formatCsvValue(record.getPrimaryKey())).append(",")
               .append(formatCsvValue(record.getAccountUid())).append(",")
               .append(formatCsvValue(record.getProductCd())).append(",")
               .append(formatCsvValue(record.getTxnPostedDate().toString())).append(",")
               .append(formatCsvValue(record.getTxnDate().toString())).append(",")
               .append(formatCsvValue(record.getTxnType())).append(",")
               .append(formatCsvValue(record.getAmount().toString())).append(",")
               .append(formatCsvValue(record.getCategory())).append(",")
               .append(formatCsvValue(record.getSubCategory())).append(",")
               .append(formatCsvValue(String.valueOf(record.getTxnUid()))).append(",")
               .append(formatCsvValue(record.getTokenizedPan())).append(",")
               .append(formatCsvValue(record.getLast4digitNbr())).append("\n");
        }

        return csv.toString();
    }

    /**
     * Formats a value for CSV output, adding double quotes if the value contains special characters
     *
     * @param value The value to format
     * @return The formatted value
     */
    private String formatCsvValue(String value) {
        if (value == null) {
            return "";
        }

        // Check if the value contains special characters that require quoting
        boolean needsQuotes = value.contains(",") || value.contains("\"") ||
                             value.contains("\n") || value.contains("\r") ||
                             value.contains(" ") || value.contains(";") ||
                             value.contains("'") || value.contains("-") ||
                             value.contains("&") || value.contains("@") ||
                             value.contains("(") || value.contains(")") ||
                             value.contains("[") || value.contains("]") ||
                             value.contains("{") || value.contains("}") ||
                             value.contains("!") || value.contains("?") ||
                             value.contains("#") || value.contains("$") ||
                             value.contains("%") || value.contains("^") ||
                             value.contains("*") || value.contains("+") ||
                             value.contains("=") || value.contains("<") ||
                             value.contains(">") || value.contains("/") ||
                             value.contains("\\");

        if (needsQuotes) {
            // Escape any double quotes by doubling them and wrap the value in double quotes
            return "\"" + value.replace("\"", "\"\"") + "\"";
        } else {
            return value;
        }
    }
}
