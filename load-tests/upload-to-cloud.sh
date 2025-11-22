#!/bin/bash
# Script to upload Artillery test results to Artillery Cloud
# Usage: ./upload-to-cloud.sh [result-file.json] or ./upload-to-cloud.sh (uploads all)

if [ -z "$1" ]; then
    echo "=========================================="
    echo "Uploading All Artillery Results to Cloud"
    echo "=========================================="
    echo ""
    
    # Check if artillery is installed
    if ! command -v artillery &> /dev/null; then
        echo "Error: Artillery is not installed. Please install it first:"
        echo "npm install -g artillery"
        exit 1
    fi
    
    # Find all JSON result files
    FILES=()
    
    if [ -d "artillery-results" ]; then
        while IFS= read -r -d '' file; do
            FILES+=("$file")
        done < <(find artillery-results -name "*.json" -type f -print0 2>/dev/null)
    fi
    
    if [ -d "results" ]; then
        while IFS= read -r -d '' file; do
            FILES+=("$file")
        done < <(find results -name "*.json" -type f -print0 2>/dev/null)
    fi
    
    if [ ${#FILES[@]} -eq 0 ]; then
        echo "No result files found in artillery-results/ or results/ directories"
        exit 1
    fi
    
    echo "Found ${#FILES[@]} result file(s):"
    for file in "${FILES[@]}"; do
        echo "  - $file"
    done
    echo ""
    
    read -p "Upload all files to Artillery Cloud? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 0
    fi
    
    echo ""
    for file in "${FILES[@]}"; do
        echo "Uploading: $file"
        artillery upload "$file"
        echo ""
    done
    
    echo "=========================================="
    echo "Upload complete!"
    echo "View your results at: https://app.artillery.io"
    echo "=========================================="
else
    RESULT_FILE=$1
    
    if [ ! -f "$RESULT_FILE" ]; then
        echo "Error: File '$RESULT_FILE' not found!"
        exit 1
    fi
    
    echo "=========================================="
    echo "Uploading to Artillery Cloud"
    echo "=========================================="
    echo "File: $RESULT_FILE"
    echo ""
    
    # Check if artillery is installed
    if ! command -v artillery &> /dev/null; then
        echo "Error: Artillery is not installed. Please install it first:"
        echo "npm install -g artillery"
        exit 1
    fi
    
    artillery upload "$RESULT_FILE"
    
    echo ""
    echo "=========================================="
    echo "Upload complete!"
    echo "View your results at: https://app.artillery.io"
    echo "=========================================="
fi

