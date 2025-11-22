#!/bin/bash

# Script to run all Artillery load tests for Vaultify API
# Usage: ./run-artillery-tests.sh [TARGET_URL]

TARGET_URL=${1:-http://localhost:8080}
RESULTS_DIR="artillery-results"

# Create results directory if it doesn't exist
mkdir -p "$RESULTS_DIR"

echo "=========================================="
echo "Vaultify API Artillery Load Tests"
echo "=========================================="
echo "Target URL: $TARGET_URL"
echo "Results will be saved in: $RESULTS_DIR"
echo "=========================================="
echo ""

# Check if artillery is installed
if ! command -v artillery &> /dev/null; then
    echo "Error: Artillery is not installed. Please install it first:"
    echo "npm install -g artillery"
    exit 1
fi

# Check if backend is accessible
echo "Checking if backend is accessible..."
if ! curl -s -o /dev/null -w "%{http_code}" "$TARGET_URL/auth/register" | grep -q "40[0-9]\|20[0-9]"; then
    echo "Warning: Backend may not be accessible at $TARGET_URL"
    echo "Please ensure the backend is running before proceeding."
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "Starting Artillery load tests..."
echo ""

# Generate timestamp for unique filenames
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Test 1: REST API Test
echo "1. Running REST API Load Test..."
artillery run --target "$TARGET_URL" --output "$RESULTS_DIR/rest-api-results_${TIMESTAMP}.json" artillery-rest-api.yml
echo "Results saved to: $RESULTS_DIR/rest-api-results_${TIMESTAMP}.json"
echo ""

# Test 2: Auth Load Test
echo "2. Running Authentication Load Test..."
artillery run --target "$TARGET_URL" --output "$RESULTS_DIR/auth-results_${TIMESTAMP}.json" artillery-auth.yml
echo "Results saved to: $RESULTS_DIR/auth-results_${TIMESTAMP}.json"
echo ""

# Test 3: GraphQL Test
echo "3. Running GraphQL Load Test..."
artillery run --target "$TARGET_URL" --output "$RESULTS_DIR/graphql-results_${TIMESTAMP}.json" artillery-graphql.yml
echo "Results saved to: $RESULTS_DIR/graphql-results_${TIMESTAMP}.json"
echo ""

# Test 4: Complete Workflow Test
echo "4. Running Complete Workflow Load Test..."
artillery run --target "$TARGET_URL" --output "$RESULTS_DIR/complete-results_${TIMESTAMP}.json" artillery-complete.yml
echo "Results saved to: $RESULTS_DIR/complete-results_${TIMESTAMP}.json"
echo ""

echo "=========================================="
echo "All Artillery load tests completed!"
echo "Results saved in: $RESULTS_DIR/"
echo ""
echo "Latest results (timestamp: ${TIMESTAMP}):"
echo "  - $RESULTS_DIR/rest-api-results_${TIMESTAMP}.json"
echo "  - $RESULTS_DIR/auth-results_${TIMESTAMP}.json"
echo "  - $RESULTS_DIR/graphql-results_${TIMESTAMP}.json"
echo "  - $RESULTS_DIR/complete-results_${TIMESTAMP}.json"
echo ""
echo "To view results:"
echo "  ./view-results.sh $RESULTS_DIR/rest-api-results_${TIMESTAMP}.json"
echo ""
echo "To upload to Artillery Cloud for visualizations:"
echo "  ./upload-to-cloud.sh $RESULTS_DIR/rest-api-results_${TIMESTAMP}.json"
echo "  Or: ./upload-to-cloud.sh (uploads all results)"
echo ""
echo "Artillery Cloud dashboard: https://app.artillery.io"
echo "=========================================="

