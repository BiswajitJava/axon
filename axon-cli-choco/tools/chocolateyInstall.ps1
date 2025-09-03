# tools/chocolateyInstall.ps1
$ErrorActionPreference = 'Stop'

$packageName = $MyInvocation.MyCommand.Definition
$version = '0.0.1' # <--- UPDATE THIS
$url = "https://github.com/your-username/axon-cli/releases/download/v${version}/axon-cli-windows-latest-amd64.exe" # <--- UPDATE THIS
$checksum = 'REPLACE_WITH_WINDOWS_SHA256' # <--- UPDATE THIS

$installDir = Join-Path $env:ProgramData 'axon-cli'
$toolFile = Join-Path $installDir 'axon.exe'

Install-ChocolateyPackage -PackageName $packageName `
                          -Url $url `
                          -Checksum $checksum `
                          -ChecksumType 'sha256' `
                          -InstallDir $installDir `
                          -UseOriginalFileName

Install-ChocolateyPath -PathToInstall $installDir -PathType 'Machine'