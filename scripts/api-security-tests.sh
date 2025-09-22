#!/bin/bash

# API Security Tests - Custom security tests for BetAware API
# Tests for common API vulnerabilities

set -e

BASE_URL="http://localhost:8080"
API_PREFIX="/api/v1"
LOG_FILE="./security-logs/api-security-tests.log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

log() {
    echo -e "${GREEN}[$(date +'%H:%M:%S')] $1${NC}" | tee -a $LOG_FILE
}

warn() {
    echo -e "${YELLOW}[$(date +'%H:%M:%S')] WARNING: $1${NC}" | tee -a $LOG_FILE
}

error() {
    echo -e "${RED}[$(date +'%H:%M:%S')] ERROR: $1${NC}" | tee -a $LOG_FILE
}

test_result() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ $1 -eq 0 ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log "✅ TEST PASSED: $2"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        error "❌ TEST FAILED: $2"
    fi
}

# Test 1: Authentication Bypass
test_authentication_bypass() {
    log "Testing authentication bypass vulnerabilities..."
    
    # Test accessing protected endpoint without token
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$API_PREFIX/apostas")
    [ "$response" = "401" ]
    test_result $? "Access protected endpoint without authentication should return 401"
    
    # Test with invalid token
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer invalid-token" \
        "$BASE_URL$API_PREFIX/apostas")
    [ "$response" = "401" ]
    test_result $? "Access with invalid token should return 401"
    
    # Test with malformed Authorization header
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: InvalidFormat" \
        "$BASE_URL$API_PREFIX/apostas")
    [ "$response" = "401" ]
    test_result $? "Access with malformed auth header should return 401"
}

# Test 2: SQL Injection
test_sql_injection() {
    log "Testing SQL injection vulnerabilities..."
    
    # Test SQL injection in login
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL$API_PREFIX/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin'\''OR 1=1--","password":"test"}')
    [ "$response" != "200" ]
    test_result $? "SQL injection in login should not succeed"
    
    # Test SQL injection with UNION
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL$API_PREFIX/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin'\'' UNION SELECT 1,2,3--","password":"test"}')
    [ "$response" != "200" ]
    test_result $? "UNION-based SQL injection should not succeed"
    
    # Test time-based SQL injection
    start_time=$(date +%s)
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL$API_PREFIX/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin'\'' OR SLEEP(5)--","password":"test"}')
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    [ $duration -lt 3 ]
    test_result $? "Time-based SQL injection should not cause delays"
}

# Test 3: Cross-Site Scripting (XSS)
test_xss() {
    log "Testing XSS vulnerabilities..."
    
    # Test XSS in registration
    response=$(curl -s -X POST "$BASE_URL$API_PREFIX/auth/register" \
        -H "Content-Type: application/json" \
        -d '{"username":"<script>alert(1)</script>","email":"test@test.com","password":"test123"}')
    echo "$response" | grep -v "<script>" > /dev/null
    test_result $? "XSS payload in username should be sanitized"
    
    # Test XSS in email field
    response=$(curl -s -X POST "$BASE_URL$API_PREFIX/auth/register" \
        -H "Content-Type: application/json" \
        -d '{"username":"testuser","email":"<img src=x onerror=alert(1)>@test.com","password":"test123"}')
    echo "$response" | grep -v "<img src=x" > /dev/null
    test_result $? "XSS payload in email should be sanitized"
    
    # Test stored XSS prevention
    response=$(curl -s -X POST "$BASE_URL$API_PREFIX/auth/register" \
        -H "Content-Type: application/json" \
        -d '{"username":"javascript:alert(1)","email":"test@test.com","password":"test123"}')
    echo "$response" | grep -v "javascript:" > /dev/null
    test_result $? "JavaScript protocol should be sanitized"
}

# Test 4: Input Validation
test_input_validation() {
    log "Testing input validation..."
    
    # Test extremely long username
    long_username=$(python3 -c "print('A' * 10000)")
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL$API_PREFIX/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$long_username\",\"email\":\"test@test.com\",\"password\":\"test123\"}")
    [ "$response" = "400" ]
    test_result $? "Extremely long username should be rejected"
    
    # Test invalid email format
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL$API_PREFIX/auth/register" \
        -H "Content-Type: application/json" \
        -d '{"username":"testuser","email":"invalid-email","password":"test123"}')
    [ "$response" = "400" ]
    test_result $? "Invalid email format should be rejected"
    
    # Test null byte injection
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL$API_PREFIX/auth/register" \
        -H "Content-Type: application/json" \
        -d '{"username":"test\u0000user","email":"test@test.com","password":"test123"}')
    [ "$response" = "400" ]
    test_result $? "Null byte injection should be rejected"
}

# Test 5: HTTP Methods Security
test_http_methods() {
    log "Testing HTTP methods security..."
    
    # Test TRACE method
    response=$(curl -s -o /dev/null -w "%{http_code}" -X TRACE "$BASE_URL$API_PREFIX/auth/login")
    [ "$response" = "405" ]
    test_result $? "TRACE method should be disabled"
    
    # Test OPTIONS method
    response=$(curl -s -o /dev/null -w "%{http_code}" -X OPTIONS "$BASE_URL$API_PREFIX/auth/login")
    [ "$response" = "200" ] || [ "$response" = "405" ]
    test_result $? "OPTIONS method should be properly configured"
    
    # Test HEAD method
    response=$(curl -s -o /dev/null -w "%{http_code}" -X HEAD "$BASE_URL$API_PREFIX/health")
    [ "$response" = "200" ]
    test_result $? "HEAD method should work for public endpoints"
}

# Test 6: Security Headers
test_security_headers() {
    log "Testing security headers..."
    
    # Test X-Content-Type-Options
    header=$(curl -s -I "$BASE_URL$API_PREFIX/health" | grep -i "x-content-type-options" || echo "")
    [ ! -z "$header" ]
    test_result $? "X-Content-Type-Options header should be present"
    
    # Test X-Frame-Options
    header=$(curl -s -I "$BASE_URL$API_PREFIX/health" | grep -i "x-frame-options" || echo "")
    [ ! -z "$header" ]
    test_result $? "X-Frame-Options header should be present"
    
    # Test X-XSS-Protection
    header=$(curl -s -I "$BASE_URL$API_PREFIX/health" | grep -i "x-xss-protection" || echo "")
    [ ! -z "$header" ]
    test_result $? "X-XSS-Protection header should be present"
    
    # Test Content-Security-Policy
    header=$(curl -s -I "$BASE_URL$API_PREFIX/health" | grep -i "content-security-policy" || echo "")
    [ ! -z "$header" ]
    test_result $? "Content-Security-Policy header should be present"
}

# Test 7: CORS Configuration
test_cors() {
    log "Testing CORS configuration..."
    
    # Test CORS preflight
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X OPTIONS "$BASE_URL$API_PREFIX/auth/login" \
        -H "Origin: http://malicious-site.com" \
        -H "Access-Control-Request-Method: POST")
    
    # Check if response allows malicious origin
    cors_header=$(curl -s -I \
        -X OPTIONS "$BASE_URL$API_PREFIX/auth/login" \
        -H "Origin: http://malicious-site.com" | \
        grep -i "access-control-allow-origin" || echo "")
    
    echo "$cors_header" | grep -v "malicious-site.com" > /dev/null
    test_result $? "CORS should not allow malicious origins"
}

# Test 8: Rate Limiting
test_rate_limiting() {
    log "Testing rate limiting..."
    
    # Attempt multiple rapid requests
    log "Sending rapid requests to test rate limiting..."
    failed_count=0
    
    for i in {1..20}; do
        response=$(curl -s -o /dev/null -w "%{http_code}" \
            -X POST "$BASE_URL$API_PREFIX/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"username":"testuser","password":"wrongpassword"}')
        
        if [ "$response" = "429" ]; then
            failed_count=$((failed_count + 1))
        fi
        
        sleep 0.1
    done
    
    # Rate limiting should trigger after some attempts
    [ $failed_count -gt 0 ]
    test_result $? "Rate limiting should be implemented for authentication attempts"
}

# Test 9: Information Disclosure
test_information_disclosure() {
    log "Testing information disclosure..."
    
    # Test error message disclosure
    response=$(curl -s -X POST "$BASE_URL$API_PREFIX/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"nonexistent","password":"test"}')
    
    # Should not reveal whether username exists
    echo "$response" | grep -i "user not found\|invalid username" > /dev/null
    [ $? -ne 0 ]
    test_result $? "Error messages should not reveal if username exists"
    
    # Test stack trace disclosure
    echo "$response" | grep -i "exception\|stack trace\|debug" > /dev/null
    [ $? -ne 0 ]
    test_result $? "Error responses should not contain stack traces"
    
    # Test server information disclosure
    server_header=$(curl -s -I "$BASE_URL$API_PREFIX/health" | grep -i "server:" || echo "")
    echo "$server_header" | grep -i "tomcat\|apache\|nginx" > /dev/null
    [ $? -ne 0 ]
    test_result $? "Server header should not reveal server information"
}

# Test 10: Session Management
test_session_management() {
    log "Testing session management..."
    
    # Test JWT token validation
    invalid_jwt="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.invalid"
    
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $invalid_jwt" \
        "$BASE_URL$API_PREFIX/apostas")
    [ "$response" = "401" ]
    test_result $? "Invalid JWT should be rejected"
    
    # Test token in URL (should not be accepted)
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        "$BASE_URL$API_PREFIX/apostas?token=$invalid_jwt")
    [ "$response" = "401" ]
    test_result $? "Token in URL should not be accepted"
}

# Generate summary report
generate_summary() {
    log "Generating API security test summary..."
    
    echo "=== API Security Test Summary ===" | tee -a $LOG_FILE
    echo "Total Tests: $TOTAL_TESTS" | tee -a $LOG_FILE
    echo "Passed: $PASSED_TESTS" | tee -a $LOG_FILE
    echo "Failed: $FAILED_TESTS" | tee -a $LOG_FILE
    echo "Success Rate: $(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%" | tee -a $LOG_FILE
    
    if [ $FAILED_TESTS -gt 0 ]; then
        echo "" | tee -a $LOG_FILE
        echo "⚠️  SECURITY ISSUES DETECTED!" | tee -a $LOG_FILE
        echo "Please review the failed tests and implement necessary security measures." | tee -a $LOG_FILE
        return 1
    else
        echo "" | tee -a $LOG_FILE
        echo "✅ All security tests passed!" | tee -a $LOG_FILE
        return 0
    fi
}

# Main execution
main() {
    log "Starting API Security Tests for BetAware API"
    log "Base URL: $BASE_URL"
    
    # Check if application is running
    if ! curl -f "$BASE_URL/api/v1/health" > /dev/null 2>&1; then
        error "Application is not running on $BASE_URL"
        exit 1
    fi
    
    # Run all tests
    test_authentication_bypass
    test_sql_injection
    test_xss
    test_input_validation
    test_http_methods
    test_security_headers
    test_cors
    test_rate_limiting
    test_information_disclosure
    test_session_management
    
    # Generate summary
    generate_summary
}

# Execute main function
main "$@"