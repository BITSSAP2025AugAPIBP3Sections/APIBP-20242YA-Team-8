#!/bin/bash

# Test script to trigger logging events and verify SolarWinds integration

BASE_URL="http://localhost:8080"

echo "=== Testing Vaultify API Logging ==="
echo ""

# Test 1: Register a user
echo "1. Testing user registration..."
REGISTER_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass123"}')

echo "Register Response: $REGISTER_RESPONSE"
echo ""

# Test 2: Login
echo "2. Testing user login..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass123"}')

echo "Login Response: $LOGIN_RESPONSE"

# Extract token from login response
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
echo "Extracted Token: $TOKEN"
echo ""

# Test 3: Test token verification
echo "3. Testing token verification..."
ME_RESPONSE=$(curl -s -X GET "${BASE_URL}/auth/me" \
  -H "Authorization: Bearer $TOKEN")

echo "Me Response: $ME_RESPONSE"
echo ""

# Test 4: Test invalid login (should trigger security alerts)
echo "4. Testing invalid login (security alert)..."
INVALID_LOGIN=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"wrongpassword"}')

echo "Invalid Login Response: $INVALID_LOGIN"
echo ""

# Test 5: Test accessing protected resource without token
echo "5. Testing unauthorized access..."
UNAUTH_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/files/folder/1")

echo "Unauthorized Response: $UNAUTH_RESPONSE"
echo ""

echo "=== Logging Test Complete ==="
echo "Check the application logs and SolarWinds dashboard for logged events"
