#!/bin/bash

# Script to view Artillery test results
# Usage: ./view-results.sh [result-file.json]

if [ -z "$1" ]; then
    echo "Usage: $0 <result-file.json>"
    echo ""
    echo "Available result files:"
    if [ -d "artillery-results" ]; then
        ls -1t artillery-results/*.json 2>/dev/null | head -5
    fi
    if [ -d "results" ]; then
        ls -1t results/*.json 2>/dev/null | head -5
    fi
    exit 1
fi

RESULT_FILE=$1

if [ ! -f "$RESULT_FILE" ]; then
    echo "Error: File '$RESULT_FILE' not found!"
    exit 1
fi

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Error: jq is not installed. Please install it first:"
    echo "  macOS: brew install jq"
    echo "  Linux: sudo apt-get install jq"
    exit 1
fi

echo "=========================================="
echo "Artillery Test Results Summary"
echo "=========================================="
echo "File: $RESULT_FILE"
echo ""

# Overall summary
echo "=== Overall Summary ==="
VUSERS_CREATED=$(jq -r '.aggregate.counters."vusers.created" // 0' "$RESULT_FILE")
VUSERS_COMPLETED=$(jq -r '.aggregate.counters."vusers.completed" // 0' "$RESULT_FILE")
VUSERS_FAILED=$(jq -r '.aggregate.counters."vusers.failed" // 0' "$RESULT_FILE")
HTTP_REQUESTS=$(jq -r '.aggregate.counters."http.requests" // 0' "$RESULT_FILE")
HTTP_RESPONSES=$(jq -r '.aggregate.counters."http.responses" // 0' "$RESULT_FILE")
REQUEST_RATE=$(jq -r '.aggregate.rates."http.request_rate" // 0 | floor' "$RESULT_FILE")

echo "  Virtual users created: $VUSERS_CREATED"
echo "  Virtual users completed: $VUSERS_COMPLETED"
echo "  Virtual users failed: $VUSERS_FAILED"
echo "  HTTP requests: $HTTP_REQUESTS"
echo "  HTTP responses: $HTTP_RESPONSES"
echo "  Mean requests/sec: $REQUEST_RATE"
echo ""

# Latency metrics
echo "=== Response Time (ms) ==="
jq -r '.aggregate.summaries."http.response_time" // {} | 
  "  min: \(.min // 0)
  max: \(.max // 0)
  median: \(.median // .p50 // 0)
  p95: \(.p95 // 0)
  p99: \(.p99 // 0)
  mean: \(.mean // 0 | floor)"' "$RESULT_FILE"
echo ""

# HTTP status codes
echo "=== HTTP Status Codes ==="
jq -r '.aggregate.counters | to_entries | map(select(.key | startswith("http.codes"))) | .[] | 
  "  \(.key | sub("http.codes."; "")): \(.value)"' "$RESULT_FILE" 2>/dev/null || echo "  (No status codes available)"
echo ""

# Errors
echo "=== Errors ==="
ERROR_COUNT=$(jq -r '[.aggregate.counters | to_entries | map(select(.key | startswith("errors"))) | .[].value] | add // 0' "$RESULT_FILE")
if [ "$ERROR_COUNT" -gt 0 ]; then
    jq -r '.aggregate.counters | to_entries | map(select(.key | startswith("errors"))) | .[] | 
      "  \(.key | sub("errors."; "")): \(.value)"' "$RESULT_FILE" 2>/dev/null
else
    echo "  No errors"
fi
echo ""

# Scenario counts
echo "=== Scenario Breakdown ==="
jq -r '.aggregate.counters | to_entries | map(select(.key | startswith("vusers.created_by_name"))) | .[] | 
  "  \(.key | sub("vusers.created_by_name."; "")): \(.value)"' "$RESULT_FILE" 2>/dev/null || echo "  (No scenario breakdown available)"
echo ""

echo "=========================================="
echo "For more details, run:"
echo "  cat $RESULT_FILE | jq '.'"
echo "=========================================="

