# Artillery Load Testing for Vaultify API

This directory contains Artillery load test configurations for the Vaultify API.

## Prerequisites

1. **Install Artillery**:
   ```bash
   npm install -g artillery
   ```

2. **Start the Vaultify Backend**:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

## Test Configurations

### 1. `artillery-rest-api.yml` - REST API Load Test
Tests all REST API endpoints including:
- User registration and authentication
- Folder CRUD operations
- File operations
- Permission/sharing operations

**Run it:**
```bash
artillery run --output results.json artillery-rest-api.yml
```

### 2. `artillery-auth.yml` - Authentication Focused Test
High-load test specifically for authentication endpoints:
- User registration
- User login
- Token validation

**Run it:**
```bash
artillery run --output results.json artillery-auth.yml
```

### 3. `artillery-graphql.yml` - GraphQL API Load Test
Tests GraphQL queries and mutations:
- GraphQL queries (currentUser, folders, files, permissions)
- GraphQL mutations (createFolder)

**Run it:**
```bash
artillery run --output results.json artillery-graphql.yml
```

### 4. `artillery-complete.yml` - Complete Workflow Test
Comprehensive test with multiple scenarios:
- Complete API workflow (60% weight)
- Read-only operations (30% weight)
- GraphQL operations (10% weight)

**Run it:**
```bash
artillery run --output results.json artillery-complete.yml
```

## Running Tests

### Basic Run (Results Saved Automatically)

#### Run Single Test with Auto-Saved Results
```bash
./run-artillery-single.sh artillery-rest-api.yml
```
This automatically saves results to `artillery-results/rest-api-results_TIMESTAMP.json`

#### Run All Tests with Auto-Saved Results
```bash
./run-artillery-tests.sh
```
This saves all results with timestamps in the `artillery-results/` directory.

### Manual Run with Custom Output File
```bash
artillery run --output my-results.json artillery-rest-api.yml
```

### Run with Custom Target
```bash
artillery run --target http://your-server:8080 --output results.json artillery-rest-api.yml
```

### Run with Custom Output File
```bash
./run-artillery-single.sh artillery-rest-api.yml http://localhost:8080 results/my-custom-name.json
```

## Results Storage

### Automatic Results Storage

All test results are automatically saved to JSON files:

- **Location**: `artillery-results/` directory
- **Naming**: `{test-name}_{timestamp}.json`
- **Format**: JSON (can be converted to HTML reports)

### Example Output Files:
```
artillery-results/
  ├── rest-api-results_20240115_143022.json
  ├── auth-results_20240115_143045.json
  ├── graphql-results_20240115_143108.json
  └── complete-results_20240115_143130.json
```

### Viewing Results

**Note**: The `artillery report` command is deprecated in newer versions of Artillery. Use one of these alternatives:

#### Option 1: View Summary in Terminal
Artillery automatically displays a summary at the end of each test run.

#### Option 2: Use jq to Extract Metrics
```bash
# View overall summary
cat artillery-results/rest-api-results_*.json | jq '.aggregate'

# View latency metrics
cat artillery-results/rest-api-results_*.json | jq '.aggregate.latency'

# View HTTP status codes
cat artillery-results/rest-api-results_*.json | jq '.aggregate.codes'

# View request rate
cat artillery-results/rest-api-results_*.json | jq '.aggregate.rate'
```

#### Option 3: Use Artillery Cloud (Recommended)
Artillery Cloud provides comprehensive reporting and visualizations:
- Sign up at https://app.artillery.io
- Upload your JSON results using the `artillery upload` command

**Upload existing JSON results:**
```bash
# Upload a single result file
artillery upload artillery-results/rest-api-results_TIMESTAMP.json

# Upload multiple files
artillery upload artillery-results/*.json
```

**Or run tests directly to Artillery Cloud:**
```bash
# Run test and upload to cloud automatically
artillery run --output artillery-results/test.json artillery-rest-api.yml
artillery upload artillery-results/test.json
```

After uploading, you can view detailed visualizations, charts, and reports in your Artillery Cloud dashboard.

#### Option 4: Create Custom Scripts
You can write simple scripts to parse and display the JSON results in a readable format.

## Understanding Results

Artillery provides detailed metrics in the JSON output:

### Key Metrics:
- **http.codes**: HTTP status code distribution
- **http.response_time**: Response time statistics (min, max, mean, median, p95, p99)
- **http.request_rate**: Requests per second
- **vusers**: Virtual users statistics
- **scenarios**: Scenario execution statistics

### Sample Output:
```
Summary report @ 14:30:15(+0000) 2024-01-15

Scenarios launched:  150
Scenarios completed: 150
Requests completed:   1350
Mean response/sec:    22.5
Response time (msec):
  min: 23
  max: 1234
  median: 98
  p95: 312
  p99: 567
Scenario counts:
  REST API Load Test: 150 (100%)
Codes:
  200: 1350
  201: 0
  401: 0
  500: 0
```

### Viewing JSON Results

You can also inspect the JSON results directly:

```bash
# Pretty print JSON
cat artillery-results/rest-api-results_*.json | jq '.'

# Extract specific metrics
cat artillery-results/rest-api-results_*.json | jq '.aggregate.latency'
cat artillery-results/rest-api-results_*.json | jq '.aggregate.codes'
```

## Configuration Options

### Load Phases

Modify the `phases` section to adjust load:

```yaml
phases:
  - duration: 60      # Duration in seconds
    arrivalRate: 10   # New users per second
    name: "Warm up"
```

### Variables

Customize variables in the config:

```yaml
variables:
  password: "testpassword123"
  usernamePrefix: "loadtest_user"
```

### Think Time

Add delays between requests:

```yaml
- think: 2  # Wait 2 seconds
```

## Customization

### Change Target URL
```yaml
config:
  target: "http://your-server:8080"
```

### Adjust Load Profile
```yaml
phases:
  - duration: 120
    arrivalRate: 20  # Increase for higher load
    name: "High load"
```

### Add Custom Headers
```yaml
defaults:
  headers:
    Content-Type: "application/json"
    X-Custom-Header: "value"
```

## Artillery Cloud Integration

Artillery Cloud provides advanced visualizations, dashboards, and reporting for your load test results.

### Getting Started

1. **Sign up**: Create an account at https://app.artillery.io
2. **Authenticate**: Artillery will prompt you to authenticate on first upload
3. **Upload results**: Use the upload script or command

### Upload Results to Cloud

#### Option 1: Record Directly to Cloud (Recommended)
Run tests and record directly to Artillery Cloud in real-time:

```bash
# Record to cloud while running test
artillery run artillery-rest-api.yml --record --key YOUR_API_KEY

# Your API key can be found at: https://app.artillery.io/settings/api-keys
```

This streams results to Artillery Cloud as the test runs, so you can monitor in real-time.

#### Option 2: Upload Existing JSON Files
If you already have JSON result files:

```bash
# Upload a single file
./upload-to-cloud.sh artillery-results/rest-api-results_TIMESTAMP.json

# Upload all result files
./upload-to-cloud.sh
```

#### Option 3: Use Artillery CLI
```bash
# Upload a single file
artillery upload artillery-results/rest-api-results_TIMESTAMP.json

# Upload multiple files
artillery upload artillery-results/*.json
```

### What You Get in Artillery Cloud

- **Interactive Dashboards**: Visual charts and graphs
- **Performance Metrics**: Response times, throughput, error rates
- **Historical Comparisons**: Compare test runs over time
- **Team Collaboration**: Share results with your team
- **Custom Reports**: Generate and export reports
- **Alerts**: Set up alerts for performance degradation

### Workflow Options

#### Option A: Record Directly to Cloud (Recommended)
```bash
# Run test and record to cloud in real-time
artillery run artillery-rest-api.yml --record --key YOUR_API_KEY

# Or use the helper script
./run-artillery-cloud.sh artillery-rest-api.yml YOUR_API_KEY

# View results in real-time at https://app.artillery.io
```

#### Option B: Run Locally, Upload Later
```bash
# 1. Run your test locally
artillery run --output results.json artillery-rest-api.yml

# 2. View summary locally
./view-results.sh results.json

# 3. Upload to cloud for detailed analysis
./upload-to-cloud.sh results.json

# 4. View in dashboard
# Open https://app.artillery.io in your browser
```

### Getting Your API Key

1. Sign up or log in at https://app.artillery.io
2. Go to Settings → API Keys
3. Create a new API key or use an existing one
4. Use it with the `--record --key` flags

## Advanced Usage

### Save and Analyze Results
```bash
# Run test and save results
artillery run --output report.json artillery-rest-api.yml

# Analyze results with jq
cat report.json | jq '.aggregate'
cat report.json | jq '.aggregate.summaries."http.response_time"'
cat report.json | jq '.aggregate.counters | to_entries | map(select(.key | startswith("http.codes")))'
```

### Run with Custom Processor
The `artillery-processor.js` file contains helper functions for generating dynamic data.

### Monitor in Real-Time
```bash
artillery run --verbose --output results.json artillery-rest-api.yml
```

### Compare Results
Save multiple test runs and compare:

```bash
# Run test 1
artillery run --output baseline.json artillery-rest-api.yml

# Make changes to API

# Run test 2
artillery run --output after-changes.json artillery-rest-api.yml

# Compare (manually or with tools)
```

## Troubleshooting

### High Error Rates
- Check backend logs
- Reduce arrival rate
- Increase think time between requests
- Verify backend is running and accessible

### Slow Response Times
- Monitor backend resources (CPU, memory)
- Check database performance
- Review endpoint implementations
- Consider reducing load

### Connection Errors
- Verify backend is running
- Check target URL is correct
- Ensure firewall/network settings allow connections

### Results Not Saving
- Ensure you use `--output` flag or the provided scripts
- Check write permissions in the results directory
- Verify disk space is available

## Best Practices

1. **Start Small**: Begin with low arrival rates and gradually increase
2. **Monitor Backend**: Watch CPU, memory, and database during tests
3. **Test Realistic Scenarios**: Use load patterns that match expected usage
4. **Save Results**: Always save results for comparison and analysis
5. **Use Reports**: Generate HTML reports to visualize performance trends
6. **Clean Up**: Some tests create data - consider cleanup for production-like testing

## API Documentation

For complete API documentation with cURL commands, see [API_CURL_COMMANDS.md](./API_CURL_COMMANDS.md)
