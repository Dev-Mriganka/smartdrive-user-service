#!/bin/bash

# User Service Endpoint Testing Script
# This script tests all endpoints of the user-service

BASE_URL="http://localhost:8086"
echo "🧪 Testing User Service Endpoints at $BASE_URL"
echo "================================================"

# Test 1: Health Check
echo "1. Testing Health Check..."
curl -s "$BASE_URL/actuator/health" | jq .
echo -e "\n"

# Test 2: User Registration
echo "2. Testing User Registration..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "email": "test1@example.com",
    "password": "TestPass123!",
    "confirmPassword": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }')

echo "$REGISTER_RESPONSE" | jq .
echo -e "\n"

# Test 3: Duplicate Registration (should fail)
echo "3. Testing Duplicate Registration (should fail)..."
curl -s -X POST "$BASE_URL/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "email": "test1@example.com",
    "password": "TestPass123!",
    "confirmPassword": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }' | jq .
echo -e "\n"

# Test 4: Invalid Registration (missing fields)
echo "4. Testing Invalid Registration (missing fields)..."
curl -s -X POST "$BASE_URL/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test2@example.com",
    "password": "TestPass123!"
  }' | jq .
echo -e "\n"

# Test 5: Profile Endpoint (should return 404 for non-existent user)
echo "5. Testing Profile Endpoint (non-existent user)..."
curl -s "$BASE_URL/api/v1/users/profile" | jq .
echo -e "\n"

# Test 6: Email Verification (should fail with invalid token)
echo "6. Testing Email Verification (invalid token)..."
curl -s -X POST "$BASE_URL/api/v1/users/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "invalid-token"
  }' | jq .
echo -e "\n"

# Test 7: Resend Verification (should fail without proper context)
echo "7. Testing Resend Verification..."
curl -s -X POST "$BASE_URL/api/v1/users/resend-verification" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test1@example.com"
  }' | jq .
echo -e "\n"

echo "✅ User Service Endpoint Testing Complete!"
echo "================================================"
