package com.datasampler.datagenerator.service;

import com.datasampler.datagenerator.model.Category;
import com.datasampler.datagenerator.model.TransactionRecord;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CategoryService categoryService;

    /**
     * Generates a list of random transaction records
     *
     * @param dataSampleCount Number of records to generate
     * @return List of generated transaction records
     */
    public List<TransactionRecord> generateTransactionRecords(int dataSampleCount) {
        return generateTransactionRecords(dataSampleCount, dataSampleCount, null, null);
    }

    /**
     * Generates a list of random transaction records
     *
     * @param dataSampleCount Number of records to generate per unique sample
     * @param uniqueSampleCount Number of unique composite keys to generate
     * @return List of generated transaction records
     */
    public List<TransactionRecord> generateTransactionRecords(int dataSampleCount, int uniqueSampleCount) {
        return generateTransactionRecords(dataSampleCount, uniqueSampleCount, null, null);
    }

    /**
     * Generates a list of random transaction records
     *
     * @param dataSampleCount Number of records to generate per unique sample
     * @param uniqueSampleCount Number of unique composite keys to generate
     * @param txnType Optional transaction type filter (PURCHASE, FEE, PAYMENT, or null for all types)
     * @return List of generated transaction records
     */
    public List<TransactionRecord> generateTransactionRecords(int dataSampleCount, int uniqueSampleCount, String txnType) {
        return generateTransactionRecords(dataSampleCount, uniqueSampleCount, txnType, null);
    }

    /**
     * Generates a list of random transaction records
     *
     * @param dataSampleCount Number of records to generate per unique sample
     * @param uniqueSampleCount Number of unique composite keys to generate
     * @param txnType Optional transaction type filter (PURCHASE, FEE, PAYMENT, or null for all types)
     * @param year Optional year for transaction posted dates (e.g., 2024). If provided, dates will be distributed across this year
     * @return List of generated transaction records
     */
    public List<TransactionRecord> generateTransactionRecords(int dataSampleCount, int uniqueSampleCount, String txnType, Integer year) {
        List<TransactionRecord> records = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();

        // Generate records with unique composite keys
        while (uniqueKeys.size() < uniqueSampleCount) {
            TransactionRecord record = generateRandomTransactionRecord(txnType, year);

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

                // Vary the transaction date
                LocalDate originalTxnDate = originalRecord.getTxnDate();
                LocalDate originalPostedDate = originalRecord.getTxnPostedDate();
                LocalDate newTxnDate;

                // If year parameter is provided or null (which means use current year), ensure the date stays within that year
                if (year != null || originalRecord.getTxnPostedDate().getYear() == LocalDate.now().getYear()) {
                    int useYear = (year != null) ? year : LocalDate.now().getYear();
                    LocalDate today = LocalDate.now();

                    // If using current year, ensure dates are up to yesterday
                    if (useYear == today.getYear()) {
                        // Generate a date from January 1st to yesterday
                        LocalDate startDate = LocalDate.of(useYear, 1, 1);
                        LocalDate endDate = today.minusDays(1); // Yesterday

                        // Calculate random day between start date and end date
                        long minDay = startDate.toEpochDay();
                        long maxDay = endDate.toEpochDay();

                        // If the current date is January 1st, use that date
                        if (minDay > maxDay) {
                            originalPostedDate = startDate;
                        } else {
                            long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay + 1);
                            originalPostedDate = LocalDate.ofEpochDay(randomDay);
                        }
                    } else {
                        // For past or future years, generate a date distributed across all months
                        int month = RANDOM.nextInt(12) + 1; // 1-12 for January-December
                        int maxDaysInMonth = java.time.YearMonth.of(useYear, month).lengthOfMonth();
                        int day = RANDOM.nextInt(maxDaysInMonth) + 1; // 1 to max days in month

                        // Create new posted date in the specified year
                        originalPostedDate = LocalDate.of(useYear, month, day);
                    }

                    // Transaction date is 0-2 days before posted date
                    // Calculate days to subtract, but ensure we don't cross year boundary
                    int daysToSubtract = RANDOM.nextInt(3); // 0-2 days before posted date

                    // If subtracting days would cross year boundary, adjust to stay in the same year
                    if (originalPostedDate.getDayOfYear() <= daysToSubtract) {
                        daysToSubtract = originalPostedDate.getDayOfYear() - 1;
                        // If we're at January 1st, don't subtract any days
                        if (daysToSubtract < 0) {
                            daysToSubtract = 0;
                        }
                    }

                    newTxnDate = originalPostedDate.minusDays(daysToSubtract);

                    // Double-check that transaction date is in the same year as posted date
                    if (newTxnDate.getYear() != originalPostedDate.getYear()) {
                        newTxnDate = LocalDate.of(originalPostedDate.getYear(), 1, 1); // Use January 1st of the same year as a fallback
                    }
                } else {
                    // Without year parameter, vary within 7 days of the original
                    newTxnDate = originalTxnDate.plusDays(RANDOM.nextInt(7) - 3); // -3 to +3 days

                    // Ensure txnDate is not after txnPostedDate
                    if (newTxnDate.isAfter(originalPostedDate)) {
                        newTxnDate = originalPostedDate;
                    }
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

                // Randomize category and subcategory
                String category;
                String subCategory;
                String categoryGUID;
                String generatedTxnType;
                String debitCreditIndicator;

                // If txnType is provided, use it; otherwise, determine based on original or randomly
                if (txnType != null && !txnType.isEmpty()) {
                    generatedTxnType = txnType;
                } else {
                    // Determine transaction type: PURCHASE (60%), FEE (15%), or PAYMENT (25%)
                    double randomValue = RANDOM.nextDouble();
                    boolean originalIsFee = "FEE".equals(originalRecord.getTxnType());
                    boolean originalIsPayment = "PAYMENT".equals(originalRecord.getTxnType());

                    if (originalIsFee || (!originalIsPayment && randomValue < 0.15)) {
                        generatedTxnType = "FEE";
                    } else if (originalIsPayment || randomValue < 0.40) {
                        generatedTxnType = "PAYMENT";
                    } else {
                        generatedTxnType = "PURCHASE";
                    }
                }

                // Set category, subcategory, and debitCreditIndicator based on transaction type
                if ("FEE".equals(generatedTxnType)) {
                    // For FEE transactions, always use "Fees and Charges"
                    category = "Fees and Charges";
                    String feesGUID = categoryService.getCategoryGuidByName("Fees and Charges");
                    categoryGUID = categoryService.getRandomSubcategoryGuid(feesGUID, RANDOM);
                    subCategory = categoryService.getCategoryNameByGuid(categoryGUID);
                    debitCreditIndicator = "D"; // FEE is a Debit
                } else if ("PAYMENT".equals(generatedTxnType)) {
                    // For PAYMENT transactions
                    // Use Uncategorized for PAYMENT transactions
                    categoryGUID = categoryService.getUncategorizedGuid();
                    category = "Uncategorized";
                    subCategory = "Uncategorized";
                    debitCreditIndicator = "C"; // PAYMENT is a Credit
                } else {
                    // For PURCHASE transactions
                    debitCreditIndicator = "D"; // PURCHASE is a Debit

                    // 70% chance to completely change the category
                    if (RANDOM.nextDouble() < 0.7) {
                        // Get a random parent category (excluding "Fees and Charges")
                        do {
                            String parentGUID = categoryService.getRandomParentCategoryGuid(RANDOM);
                            category = categoryService.getCategoryNameByGuid(parentGUID);

                            // Get a random subcategory for this parent
                            categoryGUID = categoryService.getRandomSubcategoryGuid(parentGUID, RANDOM);
                            subCategory = categoryService.getCategoryNameByGuid(categoryGUID);
                        } while ("Fees and Charges".equals(category));
                    } else {
                        // Keep the original category but change subcategory
                        category = originalRecord.getCategory();

                        // If original category was "Fees and Charges", change it to something else
                        if ("Fees and Charges".equals(category)) {
                            do {
                                String parentGUID = categoryService.getRandomParentCategoryGuid(RANDOM);
                                category = categoryService.getCategoryNameByGuid(parentGUID);
                            } while ("Fees and Charges".equals(category));
                        }

                        String parentGUID = categoryService.getCategoryGuidByName(category);
                        categoryGUID = categoryService.getRandomSubcategoryGuid(parentGUID, RANDOM);
                        subCategory = categoryService.getCategoryNameByGuid(categoryGUID);
                    }
                }

                TransactionRecord relatedRecord = TransactionRecord.builder()
                    .accountUid(originalRecord.getAccountUid())
                    .productCd(originalRecord.getProductCd())
                    .txnPostedDate(originalPostedDate) // Use the potentially updated posted date
                    .txnDate(newTxnDate) // Varied transaction date
                    .txnType(generatedTxnType)
                    .amount(newAmount) // Varied amount
                    .category(category)
                    .subCategory(subCategory) // Potentially varied subcategory
                    .categoryGUID(categoryGUID)
                    .txnUid(newTxnUid)
                    .tokenizedPan(originalRecord.getTokenizedPan())
                    .last4digitNbr(originalRecord.getLast4digitNbr())
                    .debitCreditIndicator(debitCreditIndicator) // Set the debit/credit indicator
                    .primaryKey(originalRecord.getAccountUid() + "_" +
                               originalRecord.getProductCd() + "_" +
                               originalPostedDate + "_" + // Use the potentially updated posted date
                               newTxnUid) // Create a new primary key with the new txnUid
                    .build();

                records.add(relatedRecord);
            }
        }

        return records;
    }

    // This method is already defined above

    /**
     * Generates a single random transaction record
     *
     * @param txnType Optional transaction type filter (PURCHASE, FEE, PAYMENT, or null for all types)
     * @param year Optional year for transaction posted date (e.g., 2024). If provided, date will be within this year
     * @return A randomly generated transaction record
     */
    private TransactionRecord generateRandomTransactionRecord(String txnType, Integer year) {
        LocalDate postedDate = generateRandomDate(year);

        // Transaction date is usually on or before the posted date
        // Calculate days to subtract, but ensure we don't cross year boundary
        int daysToSubtract = RANDOM.nextInt(3); // 0-2 days before posted date

        // If subtracting days would cross year boundary, adjust to stay in the same year
        if (postedDate.getDayOfYear() <= daysToSubtract) {
            daysToSubtract = postedDate.getDayOfYear() - 1;
            // If we're at January 1st, don't subtract any days
            if (daysToSubtract < 0) {
                daysToSubtract = 0;
            }
        }

        LocalDate txnDate = postedDate.minusDays(daysToSubtract);

        // Double-check that transaction date is in the same year as posted date
        if (txnDate.getYear() != postedDate.getYear()) {
            txnDate = LocalDate.of(postedDate.getYear(), 1, 1); // Use January 1st of the same year as a fallback
        }

        String tokenizedPan = generateRandomTokenizedPan();

        // Get category and subcategory from CategoryService
        String categoryGUID;
        String category;
        String subCategory;
        String generatedTxnType;
        String debitCreditIndicator;

        // If txnType is provided, use it; otherwise, determine randomly
        if (txnType != null && !txnType.isEmpty()) {
            generatedTxnType = txnType;
        } else {
            // Determine transaction type: PURCHASE (70%), FEE (15%), or PAYMENT (15%)
            double randomValue = RANDOM.nextDouble();
            if (randomValue < 0.15) {
                generatedTxnType = "FEE";
            } else if (randomValue < 0.30) {
                generatedTxnType = "PAYMENT";
            } else {
                generatedTxnType = "PURCHASE";
            }
        }

        // Set category, subcategory, and debitCreditIndicator based on transaction type
        if ("FEE".equals(generatedTxnType)) {
            // For FEE transactions, always use "Fees and Charges" and txnType = "FEE"
            String feesGUID = categoryService.getCategoryGuidByName("Fees and Charges");
            categoryGUID = categoryService.getRandomSubcategoryGuid(feesGUID, RANDOM);
            category = "Fees and Charges";
            subCategory = categoryService.getCategoryNameByGuid(categoryGUID);
            debitCreditIndicator = "D"; // FEE is a Debit
        } else if ("PAYMENT".equals(generatedTxnType)) {
            // For PAYMENT transactions
            // Use Uncategorized for PAYMENT transactions
            categoryGUID = categoryService.getUncategorizedGuid();
            category = "Uncategorized";
            subCategory = "Uncategorized";
            debitCreditIndicator = "C"; // PAYMENT is a Credit
        } else {
            // For PURCHASE transactions

            // Get a random parent category (excluding "Fees and Charges")
            do {
                categoryGUID = categoryService.getRandomParentCategoryGuid(RANDOM);
                category = categoryService.getCategoryNameByGuid(categoryGUID);
            } while ("Fees and Charges".equals(category));

            // Get a random subcategory for this parent
            String subCategoryGUID = categoryService.getRandomSubcategoryGuid(categoryGUID, RANDOM);
            subCategory = categoryService.getCategoryNameByGuid(subCategoryGUID);

            // Update categoryGUID to the subcategory GUID
            categoryGUID = subCategoryGUID;
            debitCreditIndicator = "D"; // PURCHASE is a Debit
        }

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
                .txnType(generatedTxnType)
                .amount(generateRandomAmount())
                .category(category)
                .subCategory(subCategory)
                .categoryGUID(categoryGUID)
                .txnUid(txnUid)
                .tokenizedPan(tokenizedPan)
                .last4digitNbr(tokenizedPan.substring(tokenizedPan.length() - 4))
                .primaryKey(primaryKey)
                .debitCreditIndicator(debitCreditIndicator)
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
        // Always use current year with dates up to yesterday
        int currentYear = LocalDate.now().getYear();
        return generateRandomDate(currentYear);
    }

    private LocalDate generateRandomDate(Integer year) {
        LocalDate today = LocalDate.now();

        if (year == null) {
            // Use current year when no year is specified
            return generateRandomDate(today.getYear());
        }

        // If the specified year is the current year, ensure dates are up to yesterday
        if (year == today.getYear()) {
            // Generate a date from January 1st to yesterday
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = today.minusDays(1); // Yesterday

            // Calculate random day between start date and end date
            long minDay = startDate.toEpochDay();
            long maxDay = endDate.toEpochDay();

            // If the current date is January 1st, use that date
            if (minDay > maxDay) {
                return startDate;
            }

            long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay + 1);
            return LocalDate.ofEpochDay(randomDay);
        } else {
            // For past or future years, generate a date distributed across all months
            int month = RANDOM.nextInt(12) + 1; // 1-12 for January-December
            int maxDaysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();
            int day = RANDOM.nextInt(maxDaysInMonth) + 1; // 1 to max days in month

            return LocalDate.of(year, month, day);
        }
    }

    // LocalTime no longer needed as we're using LocalDate

    // This method is no longer needed as we're determining transaction type based on category
    /*
    private String generateRandomTxnType() {
        // 80% chance of PURCHASE, 20% chance of FEE
        return RANDOM.nextDouble() < 0.8 ? "PURCHASE" : "FEE";
    }
    */

    private BigDecimal generateRandomAmount() {
        // Generate amount between 1 and 10,000
        double amount = 1 + (RANDOM.nextDouble() * 9999);
        return new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    // These methods are no longer needed as we're using CategoryService
    // Keeping them commented out for reference

    /*
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
    */

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
        csv.append("primary_key,account_uid,product_cd,txn_posted_date,txn_date,txn_type,amount,category,sub_category,category_guid,debit_credit_indicator,txn_uid,tokenized_pan,last4digitNbr\n");

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
               .append(formatCsvValue(record.getCategoryGUID())).append(",")
               .append(formatCsvValue(record.getDebitCreditIndicator())).append(",")
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
