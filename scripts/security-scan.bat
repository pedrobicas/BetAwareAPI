@echo off
REM BetAware API - Comprehensive Security Scanning Script (Windows)
REM This script performs SAST, DAST, and SCA security testing

setlocal enabledelayedexpansion

echo 🔒 Starting BetAware API Security Scan...
echo ================================================

REM Create reports directory
if not exist security-reports mkdir security-reports

set SECURITY_ISSUES=false

echo.
echo [INFO] Starting SAST (Static Application Security Testing)...
echo ================================================

REM Run SpotBugs security analysis
echo [INFO] Running SpotBugs security analysis...
call mvn compile spotbugs:check -Dspotbugs.failOnError=false
if !errorlevel! equ 0 (
    echo [SUCCESS] SpotBugs analysis completed
) else (
    echo [WARNING] SpotBugs found potential security issues
)

REM Run PMD security rules
echo [INFO] Running PMD security analysis...
call mvn pmd:check -Dpmd.failOnViolation=false
if !errorlevel! equ 0 (
    echo [SUCCESS] PMD analysis completed
) else (
    echo [WARNING] PMD found potential security issues
)

echo.
echo [INFO] Starting SCA (Software Composition Analysis)...
echo ================================================

REM Run OWASP Dependency Check
echo [INFO] Running OWASP Dependency Check...
call mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7
if !errorlevel! equ 0 (
    echo [SUCCESS] OWASP Dependency Check completed - No critical vulnerabilities found
) else (
    echo [ERROR] OWASP Dependency Check found critical vulnerabilities!
    set SECURITY_ISSUES=true
)

REM Copy dependency check report
if exist "target\dependency-check-report.html" (
    copy "target\dependency-check-report.html" "security-reports\"
    echo [INFO] Dependency check report saved to security-reports\
)

echo.
echo [INFO] Building application for DAST testing...
echo ================================================

REM Clean and build the application
call mvn clean package -DskipTests
if !errorlevel! neq 0 (
    echo [ERROR] Build failed! Cannot proceed with DAST testing.
    exit /b 1
)

echo.
echo [INFO] Starting DAST (Dynamic Application Security Testing)...
echo ================================================

REM Start the application in background
echo [INFO] Starting BetAware API for DAST testing...
for %%f in (target\*.jar) do set JAR_FILE=%%f
start /b java -jar !JAR_FILE! --spring.profiles.active=test

REM Wait for application to start
echo [INFO] Waiting for application to start...
timeout /t 30 /nobreak >nul

REM Check if application is running
curl -f http://localhost:8080/v1/health >nul 2>&1
if !errorlevel! equ 0 (
    echo [SUCCESS] Application started successfully
) else (
    echo [ERROR] Application failed to start!
    taskkill /f /im java.exe >nul 2>&1
    exit /b 1
)

REM Run basic security tests
echo [INFO] Running basic security tests...

REM Test for common security headers
echo [INFO] Checking security headers...
curl -I http://localhost:8080/v1/health > temp_headers.txt 2>nul

findstr /i "x-frame-options" temp_headers.txt >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] X-Frame-Options header present
    set XFRAME_STATUS=Present
) else (
    echo [WARNING] X-Frame-Options header missing
    set XFRAME_STATUS=Missing
)

findstr /i "x-content-type-options" temp_headers.txt >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] X-Content-Type-Options header present
    set XCONTENT_STATUS=Present
) else (
    echo [WARNING] X-Content-Type-Options header missing
    set XCONTENT_STATUS=Missing
)

findstr /i "strict-transport-security" temp_headers.txt >nul
if !errorlevel! equ 0 (
    echo [SUCCESS] Strict-Transport-Security header present
    set HSTS_STATUS=Present
) else (
    echo [WARNING] Strict-Transport-Security header missing
    set HSTS_STATUS=Missing
)

REM Test for information disclosure
echo [INFO] Testing for information disclosure...
curl -s http://localhost:8080/nonexistent-endpoint > temp_error.txt 2>nul
findstr /i "stack trace exception error" temp_error.txt >nul
if !errorlevel! equ 0 (
    echo [WARNING] Potential information disclosure in error responses
    set INFO_DISCLOSURE=Potential disclosure
) else (
    echo [SUCCESS] No obvious information disclosure detected
    set INFO_DISCLOSURE=No obvious disclosure
)

REM Stop the application
echo [INFO] Stopping application...
taskkill /f /im java.exe >nul 2>&1

REM Clean up temp files
del temp_headers.txt >nul 2>&1
del temp_error.txt >nul 2>&1

echo.
echo [INFO] Generating consolidated security report...
echo ================================================

set REPORT_FILE=security-reports\security-scan-report.md

echo # BetAware API - Security Scan Report > !REPORT_FILE!
echo. >> !REPORT_FILE!
echo **Generated on:** %date% %time% >> !REPORT_FILE!
echo **Scan Type:** Comprehensive (SAST + SCA + DAST) >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo ## Executive Summary >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo This report contains the results of automated security testing performed on the BetAware API. >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo ## SAST Results (Static Application Security Testing) >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo ### SpotBugs Security Analysis >> !REPORT_FILE!
echo - **Status:** Completed >> !REPORT_FILE!
echo - **Report Location:** target\spotbugsXml.xml >> !REPORT_FILE!
echo - **Focus:** Security vulnerabilities in source code >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo ### PMD Security Analysis >> !REPORT_FILE!
echo - **Status:** Completed >> !REPORT_FILE!
echo - **Report Location:** target\pmd.xml >> !REPORT_FILE!
echo - **Focus:** Code quality and security patterns >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo ## SCA Results (Software Composition Analysis) >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo ### OWASP Dependency Check >> !REPORT_FILE!
echo - **Status:** Completed >> !REPORT_FILE!
echo - **Report Location:** security-reports\dependency-check-report.html >> !REPORT_FILE!
echo - **Focus:** Known vulnerabilities in dependencies >> !REPORT_FILE!
echo - **Critical Threshold:** CVSS ^>= 7.0 >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo ## DAST Results (Dynamic Application Security Testing) >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo ### Security Headers Analysis >> !REPORT_FILE!
if "!XFRAME_STATUS!"=="Present" (
    echo - X-Frame-Options: ✅ Present >> !REPORT_FILE!
) else (
    echo - X-Frame-Options: ❌ Missing >> !REPORT_FILE!
)
if "!XCONTENT_STATUS!"=="Present" (
    echo - X-Content-Type-Options: ✅ Present >> !REPORT_FILE!
) else (
    echo - X-Content-Type-Options: ❌ Missing >> !REPORT_FILE!
)
if "!HSTS_STATUS!"=="Present" (
    echo - Strict-Transport-Security: ✅ Present >> !REPORT_FILE!
) else (
    echo - Strict-Transport-Security: ❌ Missing >> !REPORT_FILE!
)
echo. >> !REPORT_FILE!
echo ### Information Disclosure Testing >> !REPORT_FILE!
if "!INFO_DISCLOSURE!"=="Potential disclosure" (
    echo - Error Response Analysis: ⚠️ Potential disclosure >> !REPORT_FILE!
) else (
    echo - Error Response Analysis: ✅ No obvious disclosure >> !REPORT_FILE!
)
echo. >> !REPORT_FILE!
echo ## Recommendations >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo 1. **Security Headers:** Implement missing security headers in Spring Security configuration >> !REPORT_FILE!
echo 2. **Error Handling:** Ensure error responses don't leak sensitive information >> !REPORT_FILE!
echo 3. **Dependency Management:** Keep dependencies updated and monitor for new vulnerabilities >> !REPORT_FILE!
echo 4. **Regular Scanning:** Integrate this security scan into CI/CD pipeline >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo ## Next Steps >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo 1. Review detailed reports in the security-reports directory >> !REPORT_FILE!
echo 2. Address any critical or high-severity findings >> !REPORT_FILE!
echo 3. Implement recommended security controls >> !REPORT_FILE!
echo 4. Schedule regular security assessments >> !REPORT_FILE!
echo. >> !REPORT_FILE!
echo --- >> !REPORT_FILE!
echo *This report was generated automatically by the BetAware API security scanning pipeline.* >> !REPORT_FILE!

echo [SUCCESS] Security report generated: !REPORT_FILE!

echo.
echo ================================================
if "!SECURITY_ISSUES!"=="true" (
    echo [ERROR] Security scan completed with CRITICAL ISSUES found!
    echo [ERROR] Please review the reports and address critical vulnerabilities before deployment.
    exit /b 1
) else (
    echo [SUCCESS] Security scan completed successfully!
    echo [SUCCESS] No critical security issues detected.
)

echo.
echo [INFO] Reports available in security-reports\ directory:
dir security-reports\

echo.
echo [INFO] Security scan completed. Review the reports and address any findings.

endlocal