# Load Testing for Vaultify API

This directory contains Artillery load testing configurations for the Vaultify API.

## Quick Start

1. **Install Artillery**:
   ```bash
   npm install -g artillery
   ```

2. **Start the Vaultify Backend**:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

3. **Run Load Tests**:
   ```bash
   cd load-tests
   ./run-artillery-tests.sh
   ```

## Test Files

- **`artillery-rest-api.yml`** - REST API load test
- **`artillery-auth.yml`** - Authentication-focused load test
- **`artillery-graphql.yml`** - GraphQL API load test
- **`artillery-complete.yml`** - Complete workflow test

## Running Tests

### Run All Tests
```bash
./run-artillery-tests.sh
```

### Run Single Test
```bash
./run-artillery-single.sh artillery-rest-api.yml
```

### Run and Record to Artillery Cloud
```bash
# Record directly to cloud (real-time monitoring)
./run-artillery-cloud.sh artillery-rest-api.yml YOUR_API_KEY

# Or manually
artillery run artillery-rest-api.yml --record --key YOUR_API_KEY
```

### Manual Run
```bash
artillery run --output results.json artillery-rest-api.yml
```

## Results

All test results are automatically saved to JSON files in the `artillery-results/` directory.

View results:
```bash
# Quick summary view
./view-results.sh artillery-results/rest-api-results_TIMESTAMP.json

# Or use jq directly
cat artillery-results/rest-api-results_TIMESTAMP.json | jq '.aggregate'

# Upload to Artillery Cloud for visualizations
./upload-to-cloud.sh artillery-results/rest-api-results_TIMESTAMP.json
# Or upload all results
./upload-to-cloud.sh
```

## Documentation

- **[LOAD_TESTING_GUIDE.md](./LOAD_TESTING_GUIDE.md)** - Complete guide to load testing REST APIs
- **[ARTILLERY_README.md](./ARTILLERY_README.md)** - Complete Artillery documentation
- **[API_CURL_COMMANDS.md](./API_CURL_COMMANDS.md)** - All API endpoints with cURL commands

