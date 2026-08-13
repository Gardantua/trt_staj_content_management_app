param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$ActorId = "22222222-2222-2222-2222-222222222222"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http
$catalogPath = Join-Path $PSScriptRoot "..\docs\demo-data\tabii-catalog.json"
$catalog = Get-Content -LiteralPath $catalogPath -Raw -Encoding UTF8 | ConvertFrom-Json
$requestHeaders = @{ "X-Test-Actor-Id" = $ActorId; "X-Test-Actor-Roles" = "EDITOR" }
$seedDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("content-engagement-tabii-" + [guid]::NewGuid())
[void](New-Item -ItemType Directory -Path $seedDirectory)

function Invoke-JsonRequest {
    param([string]$Method, [string]$Path, $Body = $null)
    $parameters = @{ Uri = "$ApiBaseUrl$Path"; Method = $Method; Headers = $requestHeaders }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json; charset=utf-8"
        $parameters.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8 -Compress))
    }
    Invoke-RestMethod @parameters
}

function Send-MediaFile {
    param([string]$FilePath, [string]$MimeType)
    $client = [System.Net.Http.HttpClient]::new()
    $client.DefaultRequestHeaders.Add("X-Test-Actor-Id", $ActorId)
    $client.DefaultRequestHeaders.Add("X-Test-Actor-Roles", "EDITOR")
    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    $stream = [System.IO.File]::OpenRead($FilePath)
    $fileContent = [System.Net.Http.StreamContent]::new($stream)
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new($MimeType)
    $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($FilePath))
    try {
        $response = $client.PostAsync("$ApiBaseUrl/api/v1/admin/media/images", $multipart).GetAwaiter().GetResult()
        $response.EnsureSuccessStatusCode()
        ($response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json)
    } finally {
        $stream.Dispose(); $multipart.Dispose(); $client.Dispose()
    }
}

try {
    $existing = Invoke-JsonRequest -Method GET -Path "/api/v1/admin/contents?page=0&size=50"
    foreach ($item in $catalog) {
        $existingItem = $existing.items | Where-Object { $_.title -eq $item.title } | Select-Object -First 1
        if ($null -ne $existingItem -and $existingItem.publicationStatus -eq "PUBLISHED") {
            Write-Output ("Atlandi: {0} zaten var." -f $item.title)
            continue
        }
        $extension = if ($item.coverUrl.EndsWith(".png")) { ".png" } else { ".jpeg" }
        $mimeType = if ($extension -eq ".png") { "image/png" } else { "image/jpeg" }
        $coverPath = Join-Path $seedDirectory (([guid]::NewGuid().ToString()) + $extension)
        Invoke-WebRequest -Uri $item.coverUrl -OutFile $coverPath -UseBasicParsing
        $content = if ($null -ne $existingItem) {
            Invoke-JsonRequest -Method GET -Path "/api/v1/admin/contents/$($existingItem.id)"
        } else {
            Invoke-JsonRequest -Method POST -Path "/api/v1/admin/contents" -Body @{
                title = $item.title; description = $item.description; contentType = $item.contentType
            }
        }
        $media = Send-MediaFile -FilePath $coverPath -MimeType $mimeType
        [void](Invoke-JsonRequest -Method PUT -Path "/api/v1/admin/contents/$($content.id)/cover" -Body @{
            mediaAssetId = $media.id; alternativeText = "$($item.title) kapak gorseli"
        })
        if ($item.contentType -eq "SERIES") {
            $withSeason = if ($content.seasons.Count -eq 0) {
                Invoke-JsonRequest -Method POST -Path "/api/v1/admin/contents/$($content.id)/seasons" -Body @{ seasonNumber = 1; title = "1. Sezon" }
            } else { $content }
            $season = $withSeason.seasons[0]
            if ($season.episodes.Count -eq 0) {
                [void](Invoke-JsonRequest -Method POST -Path "/api/v1/admin/contents/$($content.id)/seasons/$($season.id)/episodes" -Body @{ episodeNumber = 1; title = "1. Bolum"; description = "Demo quiz kapsami icin baslangic bolumu." })
            }
        }
        [void](Invoke-JsonRequest -Method POST -Path "/api/v1/admin/contents/$($content.id)/publish")
        Write-Output ("Eklendi: {0} - kaynak: {1}" -f $item.title, $item.sourceUrl)
    }
} finally {
    if ((Test-Path -LiteralPath $seedDirectory) -and $seedDirectory.StartsWith([System.IO.Path]::GetTempPath())) {
        Remove-Item -LiteralPath $seedDirectory -Recurse -Force
    }
}
