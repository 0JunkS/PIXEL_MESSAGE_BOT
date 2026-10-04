# 📱 PixelBot - 화면 감시 메신저봇 (KakaoTalk Screen Bot)

기존 메신저봇처럼 상단바 알림(Notification)에만 의존하지 않고, **카카오톡 대화 화면을 실시간으로 감지**하여 사람 대신 자동으로 타이핑 및 전송해주는 백그라운드 자동화 안드로이드 앱입니다.

---

## ✨ 핵심 특징

1. **노코드(No-Code) 매크로 규칙**
   - 코딩 없이 UI에서 바로 트리거와 응답 설정 가능
   - 기본 탑재 예시:
     - `!고승현` ➡️ **`고승현 되기 {count}일차`** (호출 시마다 1일차, 2일차, 3일차... 자동 1씩 증가!)
     - `!도움말` ➡️ 명령어 가이드 자동 전송
     - `!주사위` ➡️ `🎲 주사위 결과: {random:1..6}` 동적 치환
     - `!시간` ➡️ `⏰ 현재 시각: {time:HH:mm}` 동적 치환
   - 동적 변수 지원:
     - `{count}` : 1부터 호출될 때마다 1씩 자동 증가 (영구 저장)
     - `{count:시작값}` : 지정한 숫자부터 1씩 증가 (예: `{count:10}` ➡️ 10, 11, 12...)
     - `{dday:YYYY-MM-DD}` : 특정 날짜로부터 며칠 경과했는지 계산
     - `{random:min..max}`, `{time:형식}`, `{sender}`, `{room}`

2. **JavaScript(Rhino) 스크립팅 엔진**
   - 메신저봇R 호환 `response(room, msg, sender, isGroupChat, replier)` 구조 탑재
   - 복잡한 봇 로직, 조건 분기, 텍스트 가공을 자바스크립트로 직접 코딩 가능
   - 앱 내에서 가상 메시지로 즉시 테스트해볼 수 있는 **시뮬레이터** 제공

3. **화면 텍스트/픽셀 감시 (배터리 효율 극대화)**
   - Android **AccessibilityService(접근성 서비스)** 엔진 탑재: 무식한 60FPS 화면 캡처 대신, 카카오톡 화면의 텍스트 노드가 변경될 때만 감지하므로 **배터리 소모가 거의 없음 (0%대 유지)**.
   - 화면에 `!고승현`이라는 글자가 나타나면 ➡️ 카카오톡 입력창(`EditText`)을 찾아 자동으로 입력 ➡️ `[전송]` 버튼을 눌러 완전 자동 전송.
   - 최근 전송 쿨타임(기본 3초) 기능으로 무한 루프 방지.

4. **백그라운드 지속 실행 (생존 보장)**
   - 안드로이드 절전 모드(Doze)로 꺼지지 않도록 **Foreground Service** 및 배터리 최적화 예외 등록 기능 제공.

---

## 🚀 GitHub Actions로 APK / AAB 릴리즈 빌드하기

이 프로젝트에는 **GitHub Actions 자동 빌드 & 릴리즈 워크플로우**(`.github/workflows/release.yml`)가 이미 포함되어 있습니다. PC에 Android Studio나 Java가 설치되어 있지 않아도 깃허브에 올리기만 하면 클라우드에서 자동으로 APK와 AAB가 빌드됩니다.

### 1단계: 깃허브(GitHub) 새 저장소 생성
1. [GitHub](https://github.com/new)에 접속하여 새 리포지토리(Repository)를 만듭니다 (예: `PixelMessengerBot`, Public 또는 Private).

### 2단계: 프로젝트를 깃허브에 푸시
터미널(PowerShell 또는 Git Bash)에서 이 폴더(`PixelMessengerBot`)로 이동 후 아래 명령어를 입력합니다:

```bash
cd "c:\Users\Windows 10\Downloads\scrcpy-win64-v4.1\PixelMessengerBot"

# Git 초기화 및 커밋
git init
git add .
git commit -m "feat: Initial commit for PixelMessengerBot"
git branch -M main

# 자신의 깃허브 리포지토리 URL로 연결 (본인 주소로 변경하세요)
git remote add origin https://github.com/당신의아이디/PixelMessengerBot.git

# 푸시
git push -u origin main
```

### 3단계: 자동 빌드 실행 및 APK 다운로드
1. 깃허브 저장소 페이지의 상단 **[Actions]** 탭으로 이동합니다.
2. 좌측 목록에서 **`Build and Release Android APK/AAB`**를 클릭합니다.
3. 우측의 **`Run workflow`** 버튼을 누릅니다.
4. 약 3~5분 후 빌드가 완료되면:
   - 저장소 우측 **[Releases]** 메뉴에 **`v1.0.0`** 릴리즈가 생성됩니다.
   - **`PixelBot-release.apk`** (스마트폰에 바로 설치 가능한 APK)
   - **`PixelBot-release.aab`** (스토어 배포용 번들)
   다운로드 링크가 제공됩니다!

---

## 📲 스마트폰 설정 및 사용 방법

1. **APK 설치**
   - 다운로드받은 `PixelBot-release.apk` 파일을 스마트폰으로 옮겨 설치합니다. (출처를 알 수 없는 앱 허용)
2. **앱 실행 후 접근성 권한 켜기**
   - 앱 상단의 **[권한 켜기]** 버튼을 누릅니다.
   - 스마트폰 설정의 `접근성` ➡️ `설치된 앱` ➡️ **PixelBot - 화면 감지 메신저봇**을 찾아 **사용 중(ON)**으로 켭니다.
3. **배터리 최적화 예외 등록**
   - 앱 4번째 탭 `[설정/배터리]`에서 **[배터리 최적화 예외 요청하기]**를 눌러 "허용"을 선택합니다.
4. **동작 테스트**
   - 카카오톡 아무 채팅방이나 열어두고, 상대방이 `!고승현`을 보내거나 본인이 입력하면 즉시 **`나는 고승현이다`**가 자동으로 입력 및 전송됩니다!
