#!/bin/bash

# Script to run Artillery tests and record directly to Artillery Cloud
# Usage: ./run-artillery-cloud.sh [test-file.yml] [API_KEY]

if [ -z "$1" ]; then
    echo "Usage: $0 <test-file.yml> [API_KEY]"
    echo ""
    echo "Examples:"
    echo "  $0 artillery-rest-api.yml YOUR_API_KEY"
    echo "  $0 artillery-rest-api.yml  # Will prompt for API key"
    echo ""
    echo "Get your API key from: https://app.artillery.io/settings/api-keys"
    exit 1
fi

TEST_FILE=$1
API_KEY=${2:-""}
TARGET_URL=${TARGET_URL:-http://localhost:8080}

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

# Get API key if not provided
if [ -z "$API_KEY" ]; then
    echo "Enter your Artillery Cloud API key:"
    echo "Get it from: https://app.artillery.io/settings/api-keys"
    read -s API_KEY
    echo ""
    
    if [ -z "$API_KEY" ]; then
        echo "Error: API key is required"
        exit 1
    fi
fi

echo "=========================================="
echo "Running Artillery Test with Cloud Recording"
echo "=========================================="
echo "Test file: $TEST_FILE"
echo "Target URL: $TARGET_URL"
echo "Recording to: Artillery Cloud"
echo "=========================================="
echo ""

# Run the test with recording
artillery run --target "$TARGET_URL" --record --key "$API_KEY" "$TEST_FILE"

echo ""
echo "=========================================="
echo "Test completed!"
echo "View results at: https://app.artillery.io"
echo "=========================================="


