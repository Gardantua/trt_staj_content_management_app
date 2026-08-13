param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$EditorActorId = "22222222-2222-2222-2222-222222222222",
    [string]$AdminActorId = "33333333-3333-3333-3333-333333333333",
    [ValidateRange(1, 10)][int]$QuizLimit = 3,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$usersPath = Join-Path $PSScriptRoot "..\docs\demo-data\demo-users.json"
$demoUsers = Get-Content -LiteralPath $usersPath -Raw -Encoding UTF8 | ConvertFrom-Json

function Invoke-DemoApi {
    param(
        [string]$Method,
        [string]$Path,
        [string]$ActorId,
        [string]$Role,
        $Body = $null,
        [string]$IdempotencyKey = ""
    )
    $headers = @{ "X-Test-Actor-Id" = $ActorId; "X-Test-Actor-Roles" = $Role }
    if ($IdempotencyKey) { $headers["Idempotency-Key"] = $IdempotencyKey }
    $parameters = @{ Uri = "$ApiBaseUrl$Path"; Method = $Method; Headers = $headers }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json; charset=utf-8"
        $parameters.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 6 -Compress))
    }
    Invoke-RestMethod @parameters
}

$publishedQuizzes = @(Invoke-DemoApi -Method GET -Path "/api/v1/quizzes" -ActorId $EditorActorId -Role "EDITOR") | Select-Object -First $QuizLimit
if ($publishedQuizzes.Count -eq 0) { throw "Önce en az bir quiz yayınlayın." }

$quizAnswerKeys = @{}
foreach ($quiz in $publishedQuizzes) {
    $quizDetails = Invoke-DemoApi -Method GET -Path "/api/v1/admin/quizzes/$($quiz.quizId)" -ActorId $EditorActorId -Role "EDITOR"
    $publishedVersion = $quizDetails.versions | Where-Object { $_.status -eq "PUBLISHED" } | Select-Object -First 1
    if ($null -eq $publishedVersion) { throw "Yayın sürümü okunamadı: $($quiz.title)" }
    $quizAnswerKeys[$quiz.quizId] = $publishedVersion.questions
}

foreach ($demoUser in $demoUsers) {
    $xpSummary = Invoke-DemoApi -Method GET -Path "/api/v1/me/xp" -ActorId $demoUser.actorId -Role "USER"
    if (-not $Force -and $xpSummary.transactionCount -gt 0) {
        Write-Output ("Atlandı: {0} için daha önce XP verisi üretilmiş." -f $demoUser.label)
        continue
    }

    foreach ($quiz in $publishedQuizzes) {
        $questions = @($quizAnswerKeys[$quiz.quizId] | Sort-Object questionOrder)
        $correctAnswerTarget = [Math]::Round($questions.Count * $demoUser.accuracyPercent / 100, 0, [MidpointRounding]::AwayFromZero)
        $attempt = Invoke-DemoApi -Method POST -Path "/api/v1/quizzes/$($quiz.quizId)/attempts" -ActorId $demoUser.actorId -Role "USER"
        $currentQuestion = $attempt.currentQuestion
        $answeredQuestionCount = $attempt.answeredQuestionCount
        $finalScore = $attempt.score

        while ($null -ne $currentQuestion) {
            $answerKey = $questions | Where-Object { $_.id -eq $currentQuestion.questionId } | Select-Object -First 1
            $correctOption = $answerKey.answerOptions | Where-Object { $_.correct } | Select-Object -First 1
            $wrongOption = $answerKey.answerOptions | Where-Object { -not $_.correct } | Select-Object -First 1
            $selectedOption = if ($answeredQuestionCount -lt $correctAnswerTarget) { $correctOption } else { $wrongOption }
            $requestKey = "demo-$($demoUser.actorId)-$($currentQuestion.questionId)"
            $answerResult = Invoke-DemoApi -Method POST -Path "/api/v1/attempts/$($attempt.attemptId)/answers" -ActorId $demoUser.actorId -Role "USER" -IdempotencyKey $requestKey -Body @{
                questionId = $currentQuestion.questionId
                selectedOptionId = $selectedOption.id
            }
            $answeredQuestionCount += 1
            $finalScore = $answerResult.score
            $currentQuestion = $answerResult.nextQuestion
        }

        Write-Output ("Üretildi: {0} | {1} | {2} puan" -f $demoUser.label, $quiz.title, $finalScore)
    }
}

$xpDeadline = [DateTime]::UtcNow.AddSeconds(10)
do {
    $readyUserCount = @($demoUsers | Where-Object {
        $summary = Invoke-DemoApi -Method GET -Path "/api/v1/me/xp" -ActorId $_.actorId -Role "USER"
        $summary.transactionCount -gt 0
    }).Count
    if ($readyUserCount -lt $demoUsers.Count) { Start-Sleep -Milliseconds 500 }
} while ($readyUserCount -lt $demoUsers.Count -and [DateTime]::UtcNow -lt $xpDeadline)

if ($readyUserCount -lt $demoUsers.Count) {
    Write-Warning "Bazı XP olayları henüz işlenmedi; sıralama birkaç saniye içinde kendiliğinden yenilenir."
}
[void](Invoke-DemoApi -Method POST -Path "/api/v1/admin/leaderboards/rebuild" -ActorId $AdminActorId -Role "ADMIN")
$leaderboard = Invoke-DemoApi -Method GET -Path "/api/v1/leaderboards/global?limit=20" -ActorId $demoUsers[0].actorId -Role "USER"
Write-Output ("Hazır: global sıralamada {0} katılımcı var." -f $leaderboard.participantCount)
Write-Output "Kullanıcı ekranında denemek için docs/demo-data/demo-users.json içindeki actorId değerlerinden birini kullanın."
