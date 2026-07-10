Write-Host "JAVA_HOME=$env:JAVA_HOME"
Set-Location "D:\lab\Agent服务工程\campus-assistant-java"
Write-Host "Running Maven test..."
cmd.exe /c "set JAVA_HOME=C:\Users\18489\.jdks\openjdk-21.0.2 && D:\Maven\apache-maven-3.9.11\bin\mvn.cmd test 2>&1"
