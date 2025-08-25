#!/bin/bash

# User Service Testing Script
# Tests the user-service endpoints

BASE_URL="http://localhost:8086"

echo "🚀 SmartDrive User Service Testing"
echo "=================================="
echo "User Service URL: $BASE_URL"
echo "Timestamp: $(date)"
echo ""

# Function to test endpoint and log results
test_user_endpoint() {
    local endpoint=$1
    local method=${2:-GET}
    local data=${3:-""}
    local auth_header=${4:-""}
    local description=$5
    
    echo "🔍 Testing: $description"
    echo "   Endpoint: $method $endpoint"
    
    local curl_cmd="curl -s -w '\n%{http_code}' -X $method '$BASE_URL$endpoint'"
    
    if [ -n "$auth_header" ]; then
        curl_cmd="$curl_cmd -H '$auth_header'"
    fi
    
    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$data'"
    elif [ "$method" = "PUT" ] && [ -n "$data" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$data'"
    fi
    
    response=$(eval $curl_cmd 2>/dev/null)
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo "   ✅ Status: $http_code - SUCCESS"
        echo "   📄 Response: $(echo "$body" | head -c 200)..."
    elif [ "$http_code" -ge 400 ] && [ "$http_code" -lt 500 ]; then
        echo "   ⚠️  Status: $http_code - CLIENT ERROR"
        echo "   📄 Response: $(echo "$body" | head -c 200)..."
    elif [ "$http_code" -ge 500 ]; then
        echo "   ❌ Status: $http_code - SERVER ERROR"
        echo "   📄 Response: $(echo "$body" | head -c 200)..."
    else
        echo "   ❓ Status: $http_code - UNKNOWN"
    fi
    echo ""
}

# 1. Test Health Check
echo "🏥 STEP 1: HEALTH CHECK"
echo "======================="

test_user_endpoint "/actuator/health" "GET" "" "" "Health Check"

# 2. Test User Registration
echo "👤 STEP 2: USER REGISTRATION"
echo "============================"

REGISTER_DATA='{"username":"testuser","email":"test@example.com","password":"TestPassword123!","confirmPassword":"TestPassword123!","firstName":"Test","lastName":"User","phoneNumber":"+1234567890","bio":"Test user for SmartDrive"}'
test_user_endpoint "/api/v1/users/register" "POST" "$REGISTER_DATA" "" "User Registration"

# 3. Test Duplicate Registration
echo "🔄 STEP 3: DUPLICATE REGISTRATION"
echo "================================="

test_user_endpoint "/api/v1/users/register" "POST" "$REGISTER_DATA" "" "Duplicate User Registration (Should Fail)"

# 4. Test Email Verification (with invalid token)
echo "📧 STEP 4: EMAIL VERIFICATION"
echo "============================="

VERIFY_DATA='{"token":"invalid_token_here"}'
test_user_endpoint "/api/v1/users/verify" "POST" "$VERIFY_DATA" "" "Email Verification (Invalid Token)"

# 5. Test Resend Verification Email
echo "📧 STEP 5: RESEND VERIFICATION"
echo "=============================="

test_user_endpoint "/api/v1/users/resend-verification?email=test@example.com" "POST" "" "" "Resend Verification Email"

# 6. Test Protected Endpoints (No Auth)
echo "🔒 STEP 6: PROTECTED ENDPOINTS (NO AUTH)"
echo "========================================"

test_user_endpoint "/api/v1/users/profile" "GET" "" "" "Get User Profile (No Auth - Should Fail)"
test_user_endpoint "/api/v1/admin/users" "GET" "" "" "Get All Users (No Auth - Should Fail)"

# 7. Test Admin Endpoints (No Auth)
echo "👑 STEP 7: ADMIN ENDPOINTS (NO AUTH)"
echo "===================================="

test_user_endpoint "/api/v1/admin/users/statistics" "GET" "" "" "Get User Statistics (No Auth - Should Fail)"
test_user_endpoint "/api/v1/admin/users/testuser/toggle" "PUT" "" "" "Toggle User Account (No Auth - Should Fail)"

# 8. Test with Invalid Token
echo "🎫 STEP 8: INVALID TOKEN TESTING"
echo "================================"

INVALID_TOKEN="Authorization: Bearer invalid_token_here"
test_user_endpoint "/api/v1/users/profile" "GET" "" "$INVALID_TOKEN" "Get User Profile (Invalid Token)"
test_user_endpoint "/api/v1/admin/users" "GET" "" "$INVALID_TOKEN" "Get All Users (Invalid Token)"

echo "🎯 USER SERVICE TESTING COMPLETED"
echo "================================="
echo ""
echo "📊 Summary:"
echo "- Health check should be accessible"
echo "- User registration should work"
echo "- Duplicate registration should fail"
echo "- Email verification should handle invalid tokens"
echo "- Protected endpoints should require authentication"
echo "- Admin endpoints should require admin role"
