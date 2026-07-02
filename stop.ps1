
foreach ($item in @(@{ Name = 'frontend (Angular)'; Port = 4200 }, @{ Name = 'backend (Spring Boot)'; Port = 8080 })) {
    Write-Host "==> Tat $($item.Name) tren cong $($item.Port)..." -ForegroundColor Cyan
    $conns = Get-NetTCPConnection -LocalPort $item.Port -State Listen -ErrorAction SilentlyContinue
    if ($conns) {
        $conns.OwningProcess | Sort-Object -Unique | ForEach-Object {
            Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
        }
    } else {
        Write-Host "    (khong co gi chay tren cong $($item.Port))" -ForegroundColor DarkGray
    }
}


Write-Host '==> Dung cac service Oracle...' -ForegroundColor Cyan
Stop-Service OracleServiceFREE -Force -ErrorAction SilentlyContinue
Stop-Service OracleOraDB23Home1TNSListener -Force -ErrorAction SilentlyContinue

Write-Host ''
Write-Host 'Da tat het. Co the dong cac cua so backend/frontend con lai (neu con).' -ForegroundColor Green
