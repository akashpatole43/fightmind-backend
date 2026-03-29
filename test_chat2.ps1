$loginBody = @{
    email = "admin@fightmind.ai"
    password = "Patole@04"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginResponse.token

Write-Host "Got token: $token"

try {
    $boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    $LF = "`r`n"
    $bodyLines = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"query`"",
        "",
        "What is a jab?",
        "--$boundary--"
    ) -join $LF

    $requestOpt = @{
        Uri = "http://localhost:8080/api/chat/send"
        Method = "Post"
        Headers = @{ "Authorization" = "Bearer $token" }
        ContentType = "multipart/form-data; boundary=$boundary"
        Body = [System.Text.Encoding]::UTF8.GetBytes($bodyLines)
    }
    
    $chatResponse = Invoke-WebRequest @requestOpt
    
    Write-Host "Chat response code: $($chatResponse.StatusCode)"
    Write-Host "Chat response content: $($chatResponse.Content)"
} catch {
    Write-Host "Chat failed with code: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Chat error details: $($_.Exception.Response.Content)"
    
    $stream = $_.Exception.Response.GetResponseStream()
    if ($stream) {
        $reader = New-Object System.IO.StreamReader($stream)
        $msg = $reader.ReadToEnd()
        Write-Host "Server returned: $msg"
    }
}
