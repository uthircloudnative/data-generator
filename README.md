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
- **Transaction Posted Date**: Date when the transaction was posted (within the last 24 months)
- **Transaction Date**: Date when the transaction occurred (0-2 days before posted date)
- **Transaction Type**: Either "PURCHASE" or "FEE"
- **Amount**: Random values between $1 and $10,000 with 2 decimal places
- **Tokenized PAN**: 16-character string representing a tokenized Primary Account Number

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

- **Fee Transactions**: Always categorized as "Fees and Charges" with subcategories like "Card Payment", "Returns", "Late Payment Fee", etc.

### Data Generation Controls
- **Data Sample Count**: Controls the total number of records generated
- **Unique Sample Count**: Controls the number of unique business entities (unique combinations of accountUid, productCd, and txnPostedDate)
- **Category Repetition Avoidance**: Logic to avoid excessive repetition of categories

### Output Format
- **CSV Format**: Properly formatted CSV with headers
- **UTF-8 Encoding**: Files are encoded in UTF-8 without BOM
- **Special Character Handling**: Fields with special characters are properly quoted

## API Usage

### Generate Transaction Data
```
GET /api/data/generate?fileType=CSV&dataSampleCount=10&uniqueSampleCount=3
```

Parameters:
- `fileType`: Output format (currently only CSV is supported)
- `dataSampleCount`: Total number of records to generate
- `uniqueSampleCount`: Number of unique business entities to generate

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


