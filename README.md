# Transaction Data Generator

## Business Use Case

This project provides a robust solution for generating synthetic transaction data that closely mimics real-world financial transactions. It addresses several key business needs:

1. **Testing and Development**: Provides realistic test data for financial applications without exposing sensitive customer information.

2. **Data Science and Analytics**: Generates consistent datasets for developing and testing analytical models.

3. **Training and Demonstration**: Creates sample data for training materials and product demonstrations.

4. **Integration Testing**: Produces data with specific patterns to test system integrations.

5. **Synthetic Data for AI Training**: Generates data suitable for training AI models like Mostly AI, maintaining statistical properties while preserving privacy.

## Business Rules and Data Characteristics

The data generator implements numerous business rules to ensure the generated data is realistic and useful:

### Account Identification
- **Account UID**: 20-digit string with first 6 digits as zeros and remaining 14 digits as random numbers
- **Product Code**: Limited to "CREDIT" or "DEBIT" values
- **Primary Key**: Composite key combining accountUid, productCd, txnPostedDate, and txnUid with underscore separators

### Transaction Details
- **Transaction Posted Date**: Date when the transaction was posted (defaults to current year, can be specified)
- **Transaction Date**: Date when the transaction occurred (0-2 days before posted date, always in same year as posted date)
- **Transaction Type**: "PURCHASE", "FEE", or "PAYMENT"
- **Amount**: Random values between $1 and $10,000 with 2 decimal places
- **Tokenized PAN**: 16-character string representing a tokenized Primary Account Number
- **Debit/Credit Indicator**: "D" for Debit transactions (PURCHASE, FEE), "C" for Credit transactions (PAYMENT)

### Categorization
- **Categories**: Transactions are categorized into 15 different categories including:
  - Shopping, Entertainment, Travel, Government Services, Home & Garden
  - Dining, Education, Healthcare, Automotive, Utilities
  - Charity, Professional Services, Insurance, Subscriptions, Uncategorized

- **Subcategories**: Each category has specific subcategories:
  - Shopping: Grocery, Homegoods, Cloths, Books, Electronics & Tech, etc.
  - Entertainment: OtpPlatform, Cinemas, Games, Music & Shows
  - Travel: Flight Booking, Bus Booking, Hotel and Stay, Space Travel, Car Rental & Taxi
  - And many more subcategories for other main categories

- **Category GUID**: Each category and subcategory has a unique identifier in the format CAT-{UniqueID}

- **Transaction Type Categorization**:
  - **Fee Transactions**: Always categorized as "Fees and Charges" with subcategories like "Card Payment", "Returns", "Late Payment Fee", etc.
  - **Payment Transactions**: Always categorized as "Uncategorized" with CategoryGUID "CAT-00000100"
  - **Purchase Transactions**: Can be any category except "Fees and Charges"

### Data Generation Controls
- **Data Sample Count**: Controls the total number of records generated
- **Unique Sample Count**: Controls the number of unique business entities (unique combinations of accountUid, productCd, and txnPostedDate)
- **Transaction Type**: Optional filter to generate only specific transaction types (PURCHASE, FEE, or PAYMENT)
- **Year**: Optional parameter to generate transactions for a specific year (defaults to current year)
- **Category Repetition Avoidance**: Logic to avoid excessive repetition of categories

### Output Format
- **CSV Format**: Properly formatted CSV with headers
- **UTF-8 Encoding**: Files are encoded in UTF-8 without BOM
- **Special Character Handling**: Fields with special characters are properly quoted

### CSV Output Fields
The generated CSV file includes the following columns:

1. `primary_key`: Composite key (accountUid_productCd_txnPostedDate_txnUid)
2. `account_uid`: 20-digit account identifier
3. `product_cd`: Product code (CREDIT or DEBIT)
4. `txn_posted_date`: Date when the transaction was posted (YYYY-MM-DD)
5. `txn_date`: Date when the transaction occurred (YYYY-MM-DD)
6. `txn_type`: Transaction type (PURCHASE, FEE, or PAYMENT)
7. `amount`: Transaction amount
8. `category`: Transaction category
9. `sub_category`: Transaction subcategory
10. `category_guid`: Unique identifier for the category/subcategory
11. `debit_credit_indicator`: D for Debit transactions, C for Credit transactions
12. `txn_uid`: Unique transaction identifier
13. `tokenized_pan`: Tokenized Primary Account Number
14. `last4digitNbr`: Last 4 digits of the PAN

## API Usage

### Generate Transaction Data
```
GET /api/data/generate
```

#### Required Parameters
- `fileType`: Output format (currently only CSV is supported)
- `dataSampleCount`: Total number of records to generate

#### Optional Parameters
- `uniqueSampleCount`: Number of unique business entities to generate (defaults to dataSampleCount)
- `txnType`: Type of transactions to generate (PURCHASE, FEE, or PAYMENT)
  - When not specified, generates a mix of all transaction types
  - PURCHASE: ~70% of transactions when not filtered
  - FEE: ~15% of transactions when not filtered
  - PAYMENT: ~15% of transactions when not filtered
- `year`: Year for transaction dates
  - When not specified, uses the current year
  - For current year, dates are distributed from January 1st to yesterday
  - For other years, dates are distributed across all 12 months

#### Response
- Content-Type: text/csv
- Content-Disposition: attachment; filename="transactions.csv"
- CSV file with headers and properly formatted transaction data

#### Error Responses
- 400 Bad Request: If parameters are invalid (e.g., invalid transaction type, invalid year)
- 500 Internal Server Error: If an unexpected error occurs during data generation

## Implementation Details

Built with Java and Spring Boot, the application follows a clean architecture with:

- **Controller Layer**: Handles HTTP requests and responses
- **Service Layer**: Contains the business logic for data generation
- **Model Layer**: Defines the data structures

The implementation ensures:
- Thread safety for concurrent requests
- Proper error handling
- Comprehensive test coverage
- Configurable data generation parameters
- Consistent date handling across year boundaries

## Configuration

### Categories JSON
The application uses a JSON file (`src/main/resources/categories.json`) to define the hierarchy of transaction categories and subcategories. Each entry in this file has:

- `categoryGUID`: Unique identifier in the format CAT-{UniqueID}
- `category`: Name of the category
- `parentCategoryGUID`: GUID of the parent category (empty for top-level categories)

This structure allows for flexible category management and ensures consistent categorization across generated transactions.

## Getting Started

### Prerequisites
- Java 17 or higher
- Gradle

### Running the Application
```bash
./gradlew bootRun
```

### Running Tests
```bash
./gradlew test
```

### Building the Application
```bash
./gradlew build
```

### Example API Calls

```bash
# Basic usage - Generate 10 transactions with default settings
curl -o transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=10"

# Control unique business entities - Generate 20 transactions with 5 unique account/product combinations
curl -o unique_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=20&uniqueSampleCount=5"

# Filter by transaction type
curl -o purchase_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=10&txnType=PURCHASE"
curl -o fee_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=10&txnType=FEE"
curl -o payment_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=10&txnType=PAYMENT"

# Generate transactions for specific years
curl -o current_year_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=10&year=2025"
curl -o past_year_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=10&year=2024"
curl -o future_year_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=10&year=2026"

# Combine parameters
curl -o fee_2024_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=5&txnType=FEE&year=2024"
curl -o payment_2025_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=5&txnType=PAYMENT&year=2025"
curl -o purchase_2026_unique_transactions.csv "http://localhost:8080/api/data/generate?fileType=CSV&dataSampleCount=20&uniqueSampleCount=5&txnType=PURCHASE&year=2026"
```
