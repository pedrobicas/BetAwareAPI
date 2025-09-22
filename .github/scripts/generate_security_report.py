#!/usr/bin/env python3
"""
Enhanced Security Report Generator
Consolidates results from multiple SAST tools with advanced reporting capabilities
"""

import json
import os
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

def parse_semgrep_results():
    """Parse Semgrep JSON results"""
    results = []
    json_file = Path("semgrep-analysis-results/semgrep-results.json")
    
    if json_file.exists():
        with open(json_file, 'r') as f:
            data = json.load(f)
            
        for result in data.get('results', []):
            rule_id = result.get('check_id', 'unknown')
            message = result['message']
            severity = result.get('extra', {}).get('severity', 'INFO')
            
            # Map severity levels
            severity_map = {'ERROR': 'HIGH', 'WARNING': 'MEDIUM', 'INFO': 'LOW'}
            mapped_severity = severity_map.get(severity, 'LOW')
            
            results.append({
                'tool': 'Semgrep',
                'rule_id': rule_id,
                'message': message,
                'severity': mapped_severity,
                'file': result.get('path', 'unknown'),
                'line': result.get('start', {}).get('line', 'N/A'),
                'category': 'Static Analysis',
                'cwe': result.get('extra', {}).get('metadata', {}).get('cwe', 'N/A'),
                'owasp': result.get('extra', {}).get('metadata', {}).get('owasp', 'N/A')
            })
    
    return results

def parse_dependency_check_results():
    """Parse OWASP Dependency Check JSON results"""
    results = []
    json_file = Path("dependency-check-results/dependency-check-report.json")
    
    if json_file.exists():
        with open(json_file, 'r') as f:
            data = json.load(f)
        
        for dependency in data.get('dependencies', []):
            file_path = dependency.get('fileName', 'unknown')
            
            for vuln in dependency.get('vulnerabilities', []):
                cve = vuln.get('name', 'unknown')
                severity = vuln.get('severity', 'LOW')
                description = vuln.get('description', 'No description available')
                
                results.append({
                    'tool': 'OWASP Dependency Check',
                    'rule_id': cve,
                    'message': description,
                    'severity': severity,
                    'file': file_path,
                    'line': 'N/A',
                    'category': 'Dependency Vulnerability',
                    'cwe': vuln.get('cwe', 'N/A'),
                    'owasp': 'A06:2021 - Vulnerable and Outdated Components'
                })
    
    return results

def generate_enhanced_html_report(all_results):
    """Generate enhanced HTML security report"""
    
    # Count vulnerabilities by severity
    severity_counts = {'HIGH': 0, 'MEDIUM': 0, 'LOW': 0}
    for result in all_results:
        severity = result['severity']
        if severity in severity_counts:
            severity_counts[severity] += 1
    
    # Calculate security score
    total_high = severity_counts['HIGH']
    total_medium = severity_counts['MEDIUM']
    security_score = max(0, 100 - (total_high * 20) - (total_medium * 5))
    
    # Determine security status
    if total_high > 0:
        security_status = "🔴 CRITICAL"
        status_color = "#dc3545"
    elif total_medium > 0:
        security_status = "🟠 WARNING"
        status_color = "#fd7e14"
    else:
        security_status = "✅ SECURE"
        status_color = "#28a745"
    
    html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BetAware API - Security Analysis Report</title>
    <style>
        body {{ 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
            margin: 0; 
            padding: 20px; 
            background-color: #f8f9fa;
        }}
        .container {{ max-width: 1200px; margin: 0 auto; }}
        .header {{ 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px; 
            border-radius: 10px; 
            margin-bottom: 30px;
            text-align: center;
        }}
        .security-score {{
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            margin-bottom: 30px;
            text-align: center;
        }}
        .score-value {{ font-size: 3rem; font-weight: bold; color: {status_color}; }}
        .status-badge {{
            display: inline-block;
            padding: 10px 20px;
            border-radius: 20px;
            background-color: {status_color};
            color: white;
            font-weight: bold;
        }}
        .summary-grid {{ 
            display: grid; 
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); 
            gap: 20px; 
            margin-bottom: 30px;
        }}
        .metric-card {{ 
            background: white;
            padding: 25px; 
            border-radius: 10px; 
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }}
        table {{ 
            width: 100%; 
            border-collapse: collapse; 
            background: white;
            border-radius: 10px;
            overflow: hidden;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }}
        th, td {{ border: 1px solid #ddd; padding: 12px; text-align: left; }}
        th {{ background-color: #495057; color: white; }}
        .severity-HIGH {{ background-color: #f8d7da; }}
        .severity-MEDIUM {{ background-color: #fff3cd; }}
        .severity-LOW {{ background-color: #d1ecf1; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔒 BetAware API Security Report</h1>
            <p>Generated on: {datetime.now().strftime('%B %d, %Y at %H:%M:%S')}</p>
        </div>
        
        <div class="security-score">
            <div class="score-value">{security_score}/100</div>
            <div class="status-badge">{security_status}</div>
            <p>Security Score based on {len(all_results)} findings</p>
        </div>
        
        <div class="summary-grid">
            <div class="metric-card">
                <h3>🔍 Vulnerability Summary</h3>
                <p>🔴 High: <strong>{severity_counts['HIGH']}</strong></p>
                <p>🟠 Medium: <strong>{severity_counts['MEDIUM']}</strong></p>
                <p>🟡 Low: <strong>{severity_counts['LOW']}</strong></p>
                <hr>
                <p><strong>Total: {len(all_results)}</strong></p>
            </div>
        </div>
        
        <h2>📋 Detailed Findings</h2>
        <table>
            <thead>
                <tr>
                    <th>Tool</th>
                    <th>Severity</th>
                    <th>Rule ID</th>
                    <th>File</th>
                    <th>Line</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
"""
    
    # Sort results by severity
    severity_order = {'HIGH': 0, 'MEDIUM': 1, 'LOW': 2}
    sorted_results = sorted(all_results, key=lambda x: severity_order.get(x['severity'], 3))
    
    for result in sorted_results:
        severity_class = f"severity-{result['severity']}"
        html_content += f"""
                <tr class="{severity_class}">
                    <td>{result['tool']}</td>
                    <td><strong>{result['severity']}</strong></td>
                    <td><code>{result['rule_id']}</code></td>
                    <td>{result['file']}</td>
                    <td>{result['line']}</td>
                    <td>{result['message'][:150]}{'...' if len(result['message']) > 150 else ''}</td>
                </tr>
"""
    
    html_content += """
            </tbody>
        </table>
    </div>
</body>
</html>
"""
    
    with open('security-report.html', 'w', encoding='utf-8') as f:
        f.write(html_content)

def main():
    """Main function to generate comprehensive security reports"""
    print("🔒 Generating comprehensive security analysis report...")
    
    # Parse results from all tools
    all_results = []
    
    # Parse Semgrep results
    semgrep_results = parse_semgrep_results()
    all_results.extend(semgrep_results)
    print(f"📊 Found {len(semgrep_results)} Semgrep issues")
    
    # Parse Dependency Check results
    dep_results = parse_dependency_check_results()
    all_results.extend(dep_results)
    print(f"🛡️ Found {len(dep_results)} dependency vulnerabilities")
    
    # Generate comprehensive reports
    generate_enhanced_html_report(all_results)
    
    print(f"✅ Security reports generated with {len(all_results)} total issues")
    
    # Calculate final score and exit code
    high_count = len([r for r in all_results if r['severity'] == 'HIGH'])
    if high_count > 0:
        print(f"❌ SECURITY CHECK FAILED: {high_count} high severity vulnerabilities found")
        return 1
    else:
        print(f"✅ SECURITY CHECK PASSED: No high severity vulnerabilities")
        return 0

if __name__ == "__main__":
    exit(main())