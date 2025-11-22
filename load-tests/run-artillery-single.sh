#!/bin/bash

# Script to run a single Artillery test and save results
# Usage: ./run-artillery-single.sh <test-file> [TARGET_URL] [OUTPUT_FILE]

if [ -z "$1" ]; then
    echo "Usage: $0 <test-file.yml> [TARGET_URL] [OUTPUT_FILE]"
    echo "Example: $0 artillery-rest-api.yml"
    echo "Example: $0 artillery-rest-api.yml http://localhost:8080 results/my-test.json"
    exit 1
fi

TEST_FILE=$1
TARGET_URL=${2:-http://localhost:8080}
RESULTS_DIR="artillery-results"

# Create results directory if it doesn't exist
mkdir -p "$RESULTS_DIR"

# Generate output filename if not provided
if [ -z "$3" ]; then
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    TEST_NAME=$(basename "$TEST_FILE" .yml)
    OUTPUT_FILE="$RESULTS_DIR/${TEST_NAME}_${TIMESTAMP}.json"
else
    OUTPUT_FILE="$3"
fi

echo "=========================================="
echo "Running Artillery Load Test"
echo "=========================================="
echo "Test file: $TEST_FILE"
echo "Target URL: $TARGET_URL"
echo "Output file: $OUTPUT_FILE"
echo "=========================================="
echo ""

# Check if test file exists
if [ ! -f "$TEST_FILE" ]; then
    echo "Error: Test file '$TEST_FILE' not found!"
    exit 1
fi

# Check if artillery is installed
if ! command -v artillery &> /dev/null; then
    echo "Error: Artillery is not installed. Please install it first:"
    echo "npm install -g artillery"
    exit 1
fi

# Run the test
artillery run --target "$TARGET_URL" --output "$OUTPUT_FILE" "$TEST_FILE"

echo ""
echo "=========================================="
echo "Test completed!"
echo "Results saved to: $OUTPUT_FILE"
echo ""
echo "To view results:"
echo "  ./view-results.sh $OUTPUT_FILE"
echo ""
echo "To upload to Artillery Cloud for visualizations:"
echo "  ./upload-to-cloud.sh $OUTPUT_FILE"
echo ""
echo "Artillery Cloud dashboard: https://app.artillery.io"
echo "=========================================="

