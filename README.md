# Bilzy (Android)

> 영수증을 찍으면 AI가 알아서 정산해주는 모임 비용 정산 앱

Bilzy는 영수증을 OCR로 인식하고, QR로 멤버를 모은 뒤, **AI(Gemini)가 "안 먹은 메뉴" 등 특이사항을 반영해 1/N을 자동 계산**해주는 정산 앱입니다. 이 저장소는 **Android 클라이언트**입니다. (백엔드: FastAPI + Supabase — 별도 저장소)

---

## ✨ 주요 기능

| 기능 | 설명 |
|---|---|
| 소셜 로그인 | 카카오 / 네이버 로그인 (JWT 발급, 토큰 자동 리프레시), 프로필 사진 표시 |
| 영수증 스캔 | CameraX로 촬영 → 서버 OCR(Gemini)로 항목·금액 인식 → 직접 수정. 인식 실패 시에만 "직접 입력" 폴백 (촬영 자체를 건너뛰는 진입점은 없음) |
| 다차(n차) 정산 | 한 정산방에서 여러 영수증(라운드)을 누적 스캔 → 라운드별로 참여 인원·제외 항목을 따로 지정 |
| QR 정산방 | QR 코드 생성(ZXing) / 스캔(ML Kit)으로 멤버 초대·입장, 서명·만료(24h)·무효화 가능한 초대 토큰 |
| AI 정산 | "안 먹은 항목"을 칩으로 선택 → 라운드별 특이사항으로 변환해 AI가 참여자별 금액·사유 계산 |
| 정산 결과 | 참여자별 금액·계산 사유·프로필 사진 표시, 정산 완료 시 내역 기록 |
| 홈 / 마이페이지 / 정산내역 | 최근 정산 내역(인원수 포함), 정산내역 목록·상세, 프로필, 계좌 정보 관리(AES-256-GCM 암호화) |

---

## 🏗️ 기술 스택

- **언어/UI**: Kotlin, ViewBinding, Material 3, Navigation Component (단일 Activity + Fragment)
- **아키텍처**: MVVM + 레이어드(`data` / `domain` / `ui`)
- **DI**: Hilt
- **비동기**: Coroutines / Flow
- **네트워크**: Retrofit + OkHttp + kotlinx.serialization
  - `AuthInterceptor`(Bearer 부착) + `TokenAuthenticator`(401 → refresh → 재시도)
- **로컬 저장**: DataStore (액세스/리프레시 토큰, 닉네임)
- **카메라/스캔**: CameraX, ML Kit Barcode(QR 인식), ZXing(QR 생성), Coil(이미지 로딩)
- **로그인 SDK**: Kakao SDK, Naver SDK

화면 간 공유 상태는 nav_graph 스코프 ViewModel을 사용합니다.
- `ScanFlowViewModel` — 스캔/OCR/확정 플로우의 `settlement_id`·라운드·항목 공유
- `RoomViewModel` — 정산방 입장 이후 화면들의 정산방 상세·멤버·AI 결과 공유

| 라이브러리 | 버전 |
|---|---|
| Navigation | 2.9.0 |
| Hilt | 2.56.1 |
| Retrofit / OkHttp | 2.11.0 / 4.12.0 |
| kotlinx.serialization | 1.7.3 |
| Coroutines | 1.9.0 |
| CameraX | 1.3.4 |
| ML Kit Barcode | 17.3.0 |
| ZXing core | 3.5.3 |
| Coil | 2.7.0 |
| Kakao SDK | 2.20.6 |
| Naver 로그인 SDK | 5.10.0 |
| DataStore | 1.1.1 |

---

## 📁 프로젝트 구조

```
app/src/main/java/com/android/bilzy
├── data
│   ├── remote      # BilzyApi(Retrofit), DTO, Interceptor/Authenticator
│   ├── repository  # Repository 구현
│   ├── local       # DataStore (TokenStore)
│   └── auth        # KakaoLoginManager, NaverLoginManager
├── domain
│   ├── model       # Settlement, SettlementMember, ReceiptItem ...
│   └── repository  # Repository 인터페이스
├── di              # Hilt 모듈 (NetworkModule 등)
├── util            # QrGenerator, JoinLink 등
└── ui
    ├── auth        # 온보딩 / 로그인 / 회원가입
    ├── home        # 홈 (최근 정산 내역)
    ├── scan        # 카메라 / 인식 / QR 스캔
    ├── settlement  # OCR 결과 / 항목 추가 / 라운드 선택 / QR 초대
    ├── room        # 입장 → 멤버대기 → 금액조정 → 계산 → 결과 → 완료
    ├── history     # 정산 내역 목록 / 상세
    └── mypage      # 마이페이지 / 계좌
```

---

## 🚀 빌드 & 실행

### 요구 사항
- Android Studio (최신), JDK 17
- `compileSdk 36` / `minSdk 26` / `targetSdk 36`
- 실행 중인 백엔드 서버 (FastAPI)

### 1. `local.properties` 설정
저장소에 커밋되지 않습니다. 아래 키를 추가하세요.

```properties
sdk.dir=/path/to/Android/sdk
KAKAO_NATIVE_APP_KEY=여기에_카카오_네이티브_앱키
NAVER_CLIENT_ID=여기에_네이버_클라이언트_ID
NAVER_CLIENT_SECRET=여기에_네이버_클라이언트_시크릿
NAVER_CLIENT_NAME=Bilzy
```

> 카카오 키는 SDK 초기화와 로그인 리다이렉트 scheme(`kakao{앱키}`)에 사용됩니다.
> 네이버 값은 미설정이어도 앱이 크래시하지 않고 로그인 버튼만 안전하게 실패합니다. 백엔드 `.env`에도 동일한 `NAVER_CLIENT_ID`/`NAVER_CLIENT_SECRET`을 설정해야 실제 로그인이 동작합니다.

### 2. 백엔드 주소 (`app/build.gradle.kts`)
빌드 타입별 `BASE_URL`이 정의되어 있습니다.

| 빌드 타입 | BASE_URL |
|---|---|
| debug | `http://10.0.2.2:8000/` (에뮬레이터 → 로컬 호스트) |
| release | `https://api.bilzy.app/` |

### 3. 빌드 / 설치
```bash
./gradlew :app:assembleDebug          # 디버그 APK 빌드
./gradlew :app:compileDebugKotlin     # 컴파일만 (빠른 검증)
./gradlew :app:installDebug           # 에뮬레이터/기기 설치
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔄 핵심 플로우

**영수증 정산(호스트)**
```
스캔 → 인식(OCR, 실패 시에만 직접 입력 폴백) → 결과 확인/수정
→ "완료하기" 또는 "추가 스캔하기"(다음 라운드로 반복)
→ QR 초대(서명·만료 토큰) → 정산방 입장 → 멤버 대기
→ 라운드별 참여 선택 → 금액 조정(안 먹은 항목 선택) → 계산(AI) → 결과 → 완료
```

**QR 참여(게스트)**
```
QR 스캔 / 딥링크(bilzy://join/{id}?token=...) → 입장 확인 → 정산방 join → 입장 → (이하 동일)
```

AI 정산은 금액 조정 화면에서 고른 "안 먹은 항목"을 라운드별 특이사항으로 만들어
`POST /settlements/{id}/calculate`로 전달하고, 계산된 참여자별 금액·사유를 결과 화면에 표시합니다.
(AI 호출 실패 시 균등 분배로 자동 폴백)

---

## ✅ 진행 상황

**구현 완료**
- 카카오 / 네이버 로그인 + 토큰 자동 리프레시, 프로필 사진 표시
- 홈(최근 내역) / 마이페이지(프로필) / 계좌 정보 관리
- 영수증 스캔 → OCR → 항목 수정 → 확정 (인식 실패 시 직접 입력 폴백)
- 다차(n차) 정산: 라운드별 영수증·참여자·제외 항목 관리
- QR 생성 / 스캔 / 서명·만료·무효화 가능한 딥링크 초대
- 정산방 흐름 전체 (입장 ~ 완료)
- AI 정산 연동 (칩 선택 → Gemini 계산 → 결과·사유)
- 정산 내역 목록 / 상세
- 이미지 압축 + 인메모리 캐시(SWR) 응답속도 최적화
- 보안 감사(IDOR / CORS / rate limit / 이미지 검증) 완료

**미완성 / 예정**
- 네이버 로그인 실기기 크리덴셜 테스트
- 호스트가 스캔한 라운드에 본인이 빠졌다고 착각할 수 있는 UX 보강
- 초대 토큰 재발급(QR 다시 만들기) 진입점을 QR 초대 화면 밖에서도 제공

> **제품 결정**: 스캔을 건너뛰는 직접 입력 진입점, 별도 "저장 영수증 보관함"은 의도적으로 제공하지 않습니다.

---

## 🔗 연관 저장소
- **Backend**: FastAPI + Supabase (OCR·AI 정산·정산방 API)
