#!/bin/bash

# BetAware API - Comprehensive Security Scanning Script
# This script performs SAST, DAST, and SCA security testing

set -e

echo "🔒 Starting BetAware API Security Scan..."
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create reports directory
mkdir -p security-reports

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 1. SAST - Static Application Security Testing
echo ""
print_status "Starting SAST (Static Application Security Testing)..."
echo "================================================"

# Run SpotBugs security analysis
print_status "Running SpotBugs security analysis..."
mvn compile spotbugs:check -Dspotbugs.failOnError=false
if [ $? -eq 0 ]; then
    print_success "SpotBugs analysis completed"
else
    print_warning "SpotBugs found potential security issues"
fi

# Run PMD security rules
print_status "Running PMD security analysis..."
mvn pmd:check -Dpmd.failOnViolation=false
if [ $? -eq 0 ]; then
    print_success "PMD analysis completed"
else
    print_warning "PMD found potential security issues"
fi

# 2. SCA - Software Composition Analysis
echo ""
print_status "Starting SCA (Software Composition Analysis)..."
echo "================================================"

# Run OWASP Dependency Check
print_status "Running OWASP Dependency Check..."
mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7
if [ $? -eq 0 ]; then
    print_success "OWASP Dependency Check completed - No critical vulnerabilities found"
else
    print_error "OWASP Dependency Check found critical vulnerabilities!"
    SECURITY_ISSUES=true
fi

# Copy dependency check report
if [ -f "target/dependency-check-report.html" ]; then
    cp target/dependency-check-report.html security-reports/
    print_status "Dependency check report saved to security-reports/"
fi

# 3. Build and prepare for DAST
echo ""
print_status "Building application for DAST testing..."
echo "================================================"

# Clean and build the application
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    print_error "Build failed! Cannot proceed with DAST testing."
    exit 1
fi

# 4. DAST - Dynamic Application Security Testing
echo ""
print_status "Starting DAST (Dynamic Application Security Testing)..."
echo "================================================"

# Start the application in background
print_status "Starting BetAware API for DAST testing..."
java -jar target/*.jar --spring.profiles.active=test &
APP_PID=$!

# Wait for application to start
sleep 30

# Check if application is running
if curl -f http://localhost:8080/v1/health > /dev/null 2>&1; then
    print_success "Application started successfully"
else
    print_error "Application failed to start!"
    kill $APP_PID 2>/dev/null || true
    exit 1
fi

# Run basic security tests
print_status "Running basic security tests..."

# Test for common security headers
print_status "Checking security headers..."
HEADERS_RESPONSE=$(curl -I http://localhost:8080/v1/health 2>/dev/null)

if echo "$HEADERS_RESPONSE" | grep -i "x-frame-options" > /dev/null; then
    print_success "X-Frame-Options header present"
else
    print_warning "X-Frame-Options header missing"
fi

if echo "$HEADERS_RESPONSE" | grep -i "x-content-type-options" > /dev/null; then
    print_success "X-Content-Type-Options header present"
else
    print_warning "X-Content-Type-Options header missing"
fi

if echo "$HEADERS_RESPONSE" | grep -i "strict-transport-security" > /dev/null; then
    print_success "Strict-Transport-Security header present"
else
    print_warning "Strict-Transport-Security header missing"
fi

# Test for information disclosure
print_status "Testing for information disclosure..."
ERROR_RESPONSE=$(curl -s http://localhost:8080/nonexistent-endpoint)
if echo "$ERROR_RESPONSE" | grep -i "stack trace\|exception\|error" > /dev/null; then
    print_warning "Potential information disclosure in error responses"
else
    print_success "No obvious information disclosure detected"
fi

# Stop the application
print_status "Stopping application..."
kill $APP_PID 2>/dev/null || true
wait $APP_PID 2>/dev/null || true

# 5. Generate Security Report
echo ""
print_status "Generating consolidated security report..."
echo "================================================"

REPORT_FILE="security-reports/security-scan-report.md"
cat > "$REPORT_FILE" << EOF
# BetAware API - Security Scan Report

**Generated on:** $(date)
**Scan Type:** Comprehensive (SAST + SCA + DAST)

## Executive Summary

This report contains the results of automated security testing performed on the BetAware API.

## SAST Results (Static Application Security Testing)

### SpotBugs Security Analysis
- **Status:** Completed
- **Report Location:** target/spotbugsXml.xml
- **Focus:** Security vulnerabilities in source code

### PMD Security Analysis  
- **Status:** Completed
- **Report Location:** target/pmd.xml
- **Focus:** Code quality and security patterns

## SCA Results (Software Composition Analysis)

### OWASP Dependency Check
- **Status:** Completed
- **Report Location:** security-reports/dependency-check-report.html
- **Focus:** Known vulnerabilities in dependencies
- **Critical Threshold:** CVSS >= 7.0

## DAST Results (Dynamic Application Security Testing)

### Security Headers Analysis
- X-Frame-Options: $(echo "$HEADERS_RESPONSE" | grep -i "x-frame-options" > /dev/null && echo "✅ Present" || echo "❌ Missing")
- X-Content-Type-Options: $(echo "$HEADERS_RESPONSE" | grep -i "x-content-type-options" > /dev/null && echo "✅ Present" || echo "❌ Missing")
- Strict-Transport-Security: $(echo "$HEADERS_RESPONSE" | grep -i "strict-transport-security" > /dev/null && echo "✅ Present" || echo "❌ Missing")

### Information Disclosure Testing
- Error Response Analysis: $(echo "$ERROR_RESPONSE" | grep -i "stack trace\|exception\|error" > /dev/null && echo "⚠️ Potential disclosure" || echo "✅ No obvious disclosure")

## Recommendations

1. **Security Headers:** Implement missing security headers in Spring Security configuration
2. **Error Handling:** Ensure error responses don't leak sensitive information
3. **Dependency Management:** Keep dependencies updated and monitor for new vulnerabilities
4. **Regular Scanning:** Integrate this security scan into CI/CD pipeline

## Next Steps

1. Review detailed reports in the security-reports directory
2. Address any critical or high-severity findings
3. Implement recommended security controls
4. Schedule regular security assessments

---
*This report was generated automatically by the BetAware API security scanning pipeline.*
EOF

print_success "Security report generated: $REPORT_FILE"

# 6. Summary
echo ""
echo "================================================"
if [ "$SECURITY_ISSUES" = "true" ]; then
    print_error "Security scan completed with CRITICAL ISSUES found!"
    print_error "Please review the reports and address critical vulnerabilities before deployment."
    exit 1
else
    print_success "Security scan completed successfully!"
    print_success "No critical security issues detected."
fi

echo ""
print_status "Reports available in security-reports/ directory:"
ls -la security-reports/

echo ""
print_status "Security scan completed. Review the reports and address any findings."