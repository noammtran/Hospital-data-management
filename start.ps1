
$ErrorActionPreference = 'Stop'

$dbUrl  = 'jdbc:oracle:thin:@26.219.218.80:1521/freepdb1'
$dbUser = 'hospital'
$dbPass = 'Hospital123'

$backendDir  = 'D:\Project\Protect_project\hospital'
$frontendDir = 'D:\Project\Protect_project\hospital-frontend'

Write-Host '==> Khoi dong cac service Oracle...' -ForegroundColor Cyan
Start-Service OracleOraDB23Home1TNSListener
Start-Service OracleServiceFREE

Write-Host '==> Mo cua so backend (Spring Boot) -> http://localhost:8080 ...' -ForegroundColor Cyan
$backendCmd = "`$env:DB_URL='$dbUrl'; `$env:DB_USERNAME='$dbUser'; `$env:DB_PASSWORD='$dbPass'; .\mvnw spring-boot:run"
Start-Process powershell -WorkingDirectory $backendDir -ArgumentList '-NoExit', '-Command', $backendCmd

Write-Host '==> Mo cua so frontend (Angular) -> http://localhost:4200 ...' -ForegroundColor Cyan
Start-Process powershell -WorkingDirectory $frontendDir -ArgumentList '-NoExit', '-Command', 'ng serve'

Write-Host ''
Write-Host 'Da khoi dong xong. Hai cua so moi dang chay backend va frontend.' -ForegroundColor Green
Write-Host 'Khi khong dung nua, chay: .\stop.ps1' -ForegroundColor Green
