#!/bin/bash

# BetAware API Security Pipeline - Local Execution Script
# Executa todas as verificações de segurança localmente

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="BetAware API"
LOG_DIR="./security-logs"
REPORT_DIR="./security-reports"

echo -e "${BLUE}🔒 Security Pipeline - Local Execution${NC}"
echo -e "${BLUE}Project: ${PROJECT_NAME}${NC}"
echo -e "${BLUE}Date: $(date)${NC}"
echo ""

# Create directories
mkdir -p $LOG_DIR $REPORT_DIR

# Function to log messages
log() {
    echo -e "${GREEN}[$(date +'%H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%H:%M:%S')] ERROR: $1${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        error "Java not found. Please install Java 17+"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        error "Maven not found. Please install Maven 3.8+"
        exit 1
    fi
    
    # Check Docker (optional for DAST)
    if ! command -v docker &> /dev/null; then
        warn "Docker not found. DAST tests will be skipped."
        SKIP_DAST=true
    fi
    
    # Check if Semgrep is available
    if ! command -v semgrep &> /dev/null; then
        warn "Semgrep not found. Install with: pip install semgrep"
        SKIP_SEMGREP=true
    fi
    
    log "Prerequisites check completed"
}

# Function to run SAST
run_sast() {
    log "Starting SAST (Static Application Security Testing)..."
    
    # Clean and compile first
    log "Compiling project..."
    mvn clean compile test-compile >> $LOG_DIR/maven-compile.log 2>&1
    
    # Semgrep
    if [ "$SKIP_SEMGREP" != true ]; then
        log "Running Semgrep analysis..."
        semgrep --config=.semgrep/settings.yml --config=.semgrep/custom-rules.yml \
                --json --output=$REPORT_DIR/semgrep-results.json . \
                >> $LOG_DIR/semgrep.log 2>&1 || warn "Semgrep found issues"
    fi
    
    # SpotBugs + FindSecBugs
    log "Running SpotBugs security analysis..."
    mvn spotbugs:check >> $LOG_DIR/spotbugs.log 2>&1 || warn "SpotBugs found issues"
    
    # PMD Security Rules
    log "Running PMD security analysis..."
    mvn pmd:check >> $LOG_DIR/pmd.log 2>&1 || warn "PMD found issues"
    
    # Copy reports
    if [ -f "target/spotbugsXml.xml" ]; then
        cp target/spotbugsXml.xml $REPORT_DIR/spotbugs-report.xml
    fi
    
    if [ -f "target/pmd.xml" ]; then
        cp target/pmd.xml $REPORT_DIR/pmd-report.xml
    fi
    
    log "SAST analysis completed"
}

# Function to run SCA
run_sca() {
    log "Starting SCA (Software Composition Analysis)..."
    
    # OWASP Dependency Check
    log "Running OWASP Dependency Check..."
    mvn dependency-check:check >> $LOG_DIR/dependency-check.log 2>&1 || warn "Vulnerabilities found in dependencies"
    
    # Copy dependency check reports
    if [ -d "target/dependency-check" ]; then
        cp -r target/dependency-check/* $REPORT_DIR/
    fi
    
    # Snyk (if token is available)
    if [ ! -z "$SNYK_TOKEN" ]; then
        log "Running Snyk analysis..."
        snyk test --file=pom.xml --json > $REPORT_DIR/snyk-results.json 2>&1 || warn "Snyk found vulnerabilities"
    else
        warn "SNYK_TOKEN not set. Skipping Snyk analysis."
    fi
    
    log "SCA analysis completed"
}

# Function to run build and tests
run_build() {
    log "Starting build and tests..."
    
    # Run tests
    log "Running unit tests..."
    mvn test >> $LOG_DIR/test.log 2>&1
    
    # Build application
    log "Building application..."
    mvn package -DskipTests >> $LOG_DIR/build.log 2>&1
    
    log "Build completed"
}

# Function to run DAST
run_dast() {
    if [ "$SKIP_DAST" = true ]; then
        warn "Skipping DAST - Docker not available"
        return
    fi
    
    log "Starting DAST (Dynamic Application Security Testing)..."
    
    # Start application in background
    log "Starting application for DAST..."
    java -jar target/*.jar --spring.profiles.active=test > $LOG_DIR/app.log 2>&1 &
    APP_PID=$!
    
    # Wait for application to start
    log "Waiting for application to start..."
    sleep 30
    
    # Check if application is running
    if ! curl -f http://localhost:8080/api/v1/health > /dev/null 2>&1; then
        error "Application failed to start"
        kill $APP_PID 2>/dev/null || true
        return 1
    fi
    
    # Run OWASP ZAP baseline scan
    log "Running OWASP ZAP baseline scan..."
    docker run -v $(pwd)/.zap:/zap/wrk/:rw \
               -v $(pwd)/$REPORT_DIR:/zap/reports/:rw \
               -t owasp/zap2docker-stable \
               zap-baseline.py \
               -t http://host.docker.internal:8080 \
               -r zap-report.html \
               -x zap-report.xml \
               >> $LOG_DIR/zap.log 2>&1 || warn "ZAP found security issues"
    
    # Custom API security tests
    log "Running custom API security tests..."
    ./scripts/api-security-tests.sh >> $LOG_DIR/api-tests.log 2>&1 || warn "API security tests found issues"
    
    # Stop application
    log "Stopping application..."
    kill $APP_PID 2>/dev/null || true
    
    log "DAST analysis completed"
}

# Function to generate consolidated report
generate_report() {
    log "Generating consolidated security report..."
    
    REPORT_FILE="$REPORT_DIR/SECURITY_SUMMARY_$(date +%Y%m%d_%H%M%S).md"
    
    cat > $REPORT_FILE << EOF
# Security Analysis Summary Report

**Generated on:** $(date)
**Project:** $PROJECT_NAME
**Execution Type:** Local

## Executive Summary

$(analyze_results)

## SAST Results
$(summarize_sast)

## SCA Results
$(summarize_sca)

## DAST Results
$(summarize_dast)

## Recommendations
$(generate_recommendations)

---
*Report generated automatically by Security Pipeline*
EOF
    
    log "Report generated: $REPORT_FILE"
}

# Function to analyze results
analyze_results() {
    CRITICAL_ISSUES=0
    HIGH_ISSUES=0
    MEDIUM_ISSUES=0
    
    # Count issues from different tools
    if [ -f "$REPORT_DIR/semgrep-results.json" ]; then
        SEMGREP_CRITICAL=$(jq '.results[] | select(.extra.severity == "ERROR")' $REPORT_DIR/semgrep-results.json | wc -l)
        CRITICAL_ISSUES=$((CRITICAL_ISSUES + SEMGREP_CRITICAL))
    fi
    
    if [ -f "$REPORT_DIR/dependency-check-report.json" ]; then
        DEPENDENCY_HIGH=$(jq '.dependencies[] | select(.vulnerabilities != null) | .vulnerabilities[] | select(.severity == "HIGH" or .severity == "CRITICAL")' $REPORT_DIR/dependency-check-report.json 2>/dev/null | wc -l)
        HIGH_ISSUES=$((HIGH_ISSUES + DEPENDENCY_HIGH))
    fi
    
    echo "- **Critical Issues:** $CRITICAL_ISSUES"
    echo "- **High Issues:** $HIGH_ISSUES"
    echo "- **Medium Issues:** $MEDIUM_ISSUES"
    echo ""
    
    if [ $CRITICAL_ISSUES -gt 0 ]; then
        echo "🔴 **Status:** CRITICAL - Immediate action required"
    elif [ $HIGH_ISSUES -gt 0 ]; then
        echo "🟡 **Status:** HIGH - Review and fix recommended"
    else
        echo "🟢 **Status:** GOOD - No critical issues found"
    fi
}

# Function to summarize SAST results
summarize_sast() {
    echo "### Tools Executed:"
    [ "$SKIP_SEMGREP" != true ] && echo "- ✅ Semgrep" || echo "- ⏭️ Semgrep (skipped)"
    echo "- ✅ SpotBugs + FindSecBugs"
    echo "- ✅ PMD Security Rules"
    echo ""
    echo "### Key Findings:"
    echo "- See individual tool reports for details"
    echo "- Reports available in: \`$REPORT_DIR/\`"
}

# Function to summarize SCA results
summarize_sca() {
    echo "### Tools Executed:"
    echo "- ✅ OWASP Dependency Check"
    [ ! -z "$SNYK_TOKEN" ] && echo "- ✅ Snyk" || echo "- ⏭️ Snyk (token not provided)"
    echo ""
    echo "### Dependency Status:"
    if [ -f "$REPORT_DIR/dependency-check-report.json" ]; then
        TOTAL_DEPS=$(jq '.dependencies | length' $REPORT_DIR/dependency-check-report.json 2>/dev/null || echo "N/A")
        VULNERABLE_DEPS=$(jq '.dependencies[] | select(.vulnerabilities != null)' $REPORT_DIR/dependency-check-report.json 2>/dev/null | wc -l)
        echo "- Total dependencies analyzed: $TOTAL_DEPS"
        echo "- Dependencies with vulnerabilities: $VULNERABLE_DEPS"
    else
        echo "- Dependency check report not found"
    fi
}

# Function to summarize DAST results
summarize_dast() {
    if [ "$SKIP_DAST" = true ]; then
        echo "- ⏭️ DAST skipped (Docker not available)"
        return
    fi
    
    echo "### Tools Executed:"
    echo "- ✅ OWASP ZAP Baseline Scan"
    echo "- ✅ Custom API Security Tests"
    echo ""
    echo "### Runtime Security Status:"
    if [ -f "$REPORT_DIR/zap-report.xml" ]; then
        HIGH_RISK=$(grep -c 'riskcode="3"' $REPORT_DIR/zap-report.xml 2>/dev/null || echo "0")
        MEDIUM_RISK=$(grep -c 'riskcode="2"' $REPORT_DIR/zap-report.xml 2>/dev/null || echo "0")
        echo "- High risk vulnerabilities: $HIGH_RISK"
        echo "- Medium risk vulnerabilities: $MEDIUM_RISK"
    else
        echo "- ZAP report not found"
    fi
}

# Function to generate recommendations
generate_recommendations() {
    echo "### Immediate Actions:"
    echo "1. Review all critical and high severity findings"
    echo "2. Update vulnerable dependencies identified in SCA"
    echo "3. Fix hardcoded secrets and credentials"
    echo ""
    echo "### Next Steps:"
    echo "1. Implement findings into CI/CD pipeline"
    echo "2. Set up automated dependency updates"
    echo "3. Configure security monitoring"
    echo ""
    echo "### Resources:"
    echo "- [Security Pipeline Documentation](./docs/security/SECURITY_PIPELINE.md)"
    echo "- [SAST Report](./docs/security/SAST_REPORT.md)"
    echo "- [DAST Report](./docs/security/DAST_REPORT.md)"
    echo "- [SCA Report](./docs/security/SCA_REPORT.md)"
}

# Function to cleanup
cleanup() {
    log "Cleaning up temporary files..."
    # Kill any remaining processes
    pkill -f "spring-boot" 2>/dev/null || true
    
    log "Security pipeline execution completed"
    log "Reports available in: $REPORT_DIR"
    log "Logs available in: $LOG_DIR"
}

# Main execution
main() {
    trap cleanup EXIT
    
    echo "Select security analysis to run:"
    echo "1) Full Analysis (SAST + SCA + Build + DAST)"
    echo "2) SAST Only"
    echo "3) SCA Only" 
    echo "4) DAST Only"
    echo "5) Custom Selection"
    read -p "Choose option [1-5]: " choice
    
    case $choice in
        1)
            check_prerequisites
            run_sast
            run_sca
            run_build
            run_dast
            generate_report
            ;;
        2)
            check_prerequisites
            run_sast
            ;;
        3)
            check_prerequisites
            run_sca
            ;;
        4)
            check_prerequisites
            run_build
            run_dast
            ;;
        5)
            check_prerequisites
            echo "Select components to run:"
            read -p "Run SAST? [y/N]: " run_sast_choice
            read -p "Run SCA? [y/N]: " run_sca_choice
            read -p "Run Build? [y/N]: " run_build_choice
            read -p "Run DAST? [y/N]: " run_dast_choice
            
            [[ $run_sast_choice =~ ^[Yy]$ ]] && run_sast
            [[ $run_sca_choice =~ ^[Yy]$ ]] && run_sca
            [[ $run_build_choice =~ ^[Yy]$ ]] && run_build
            [[ $run_dast_choice =~ ^[Yy]$ ]] && run_dast
            
            generate_report
            ;;
        *)
            echo "Invalid option"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"