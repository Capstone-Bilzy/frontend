# Bilzy — Android 클라이언트

영수증을 스캔하면 AI(Gemini)가 "안 먹은 항목" 등 특이사항을 반영해 1/N 정산을 계산해주는 모임 비용 정산 앱.
이 저장소는 **Android 클라이언트만** 포함한다. 백엔드(FastAPI + Supabase)는 별도 저장소.

이 파일은 이 저장소에서 작업하는 모든 서브에이전트(기획자/개발자/리뷰어/보안)가 공통으로 따라야 하는 규칙이다.
역할별 세부 지침은 `.claude/agents/planner.md`, `.claude/agents/developer.md`, `.claude/agents/reviewer.md`, `.claude/agents/security.md` 참고.
- planner/developer는 사용자가 명시적으로 요청할 때만 호출한다.
- **developer가 새 기능(단순 버그 수정/오탈자 제외) 구현을 마치면 reviewer와 security를 함께 자동 호출한다** — 사용자에게 매번 다시 요청받지 않아도 됨. 두 에이전트는 역할이 겹치지 않는다: reviewer는 정확성/제품결정/아키텍처만, security는 인증·인가·시크릿·입력검증 등 보안만 본다(백엔드 저장소도 포함해서 봄).

## 기술 스택
- Kotlin, ViewBinding, Material 3, Navigation Component (단일 Activity + Fragment)
- 아키텍처: MVVM + 레이어드(`data` / `domain` / `ui`), DI는 Hilt
- 비동기: Coroutines / Flow
- 네트워크: Retrofit + OkHttp + kotlinx.serialization (`AuthInterceptor` Bearer 부착 + `TokenAuthenticator` 401→refresh→재시도)
- 로컬 저장: DataStore (토큰/닉네임)
- 카메라/스캔: CameraX, ML Kit Barcode(QR 인식), ZXing(QR 생성), Coil(이미지 로딩)
- 로그인: Kakao SDK (네이버는 미구현, 버튼만 "준비중" 토스트)
- 화면 간 공유 상태는 nav_graph 스코프 ViewModel 사용: `ScanFlowViewModel`(스캔/OCR 플로우), `RoomViewModel`(정산방 입장 이후)

## 패키지 구조
```
app/src/main/java/com/android/bilzy
├── data/{remote,repository,local,auth}
├── domain/{model,repository}
├── di          # Hilt 모듈
├── util
└── ui/{auth,home,scan,settlement,room,history,mypage}
```

## 빌드 / 실행 / 테스트
```bash
./gradlew :app:assembleDebug          # 디버그 APK 빌드
./gradlew :app:compileDebugKotlin     # 컴파일만 (빠른 검증)
./gradlew :app:installDebug           # 에뮬레이터/기기 설치
adb install -r app/build/outputs/apk/debug/app-debug.apk
./gradlew test                        # JVM 유닛 테스트 (app/src/test, 현재 거의 비어있음)
./gradlew connectedAndroidTest        # 계측 테스트 (app/src/androidTest, 현재 거의 비어있음)
```
- 로컬 백엔드 필요: `local.properties`에 `KAKAO_NATIVE_APP_KEY` 설정, debug BASE_URL은 `http://10.0.2.2:8000/`(에뮬레이터→호스트 localhost).
- 코드 수정 후에는 최소 `./gradlew :app:compileDebugKotlin`으로 컴파일 검증할 것. UI가 걸린 변경은 에뮬레이터 설치 후 실제 화면에서 확인할 것 (스크린샷/adb 캡처 가능).
- git 저장소 아님(로컬 프로젝트 폴더) — 커밋/버전관리 관련 작업은 사용자에게 먼저 확인.

## 백엔드
- FastAPI + Supabase, 별도 저장소. 로컬 서버 실행/읽기용 사본: `/Users/jeongseorin/backend/bilzy` (2026-09-09 확인, 정상 접근됨 — `routers/`, `services/`, `models/`, `core/`, `main.py` 있음, git 저장소 아님). 원래 경로 `/Users/jeongseorin/Desktop/2026/26-1/디미 캡스톤/bilzy`는 macOS 권한(TCC) 문제로 자주 막힘(2026-09-09 기준 EPERM) — 막히면 재시도하지 말고 `backend/bilzy` 경로를 먼저 시도할 것. API 계약이 불확실하면 추측하지 말고 `routers/`, `services/`, `models/schemas.py`를 직접 읽어 확인할 것. 단, 파일 수정은 이 프로젝트 범위 밖 — 백엔드 코드 변경이 필요하면 사용자에게 확인 후 진행.
- 인증: 카카오 OAuth 토큰 → `POST /auth/social` → 백엔드 JWT(access+refresh) 발급 → 이후 `Authorization: Bearer`.
- 핵심 흐름: `POST /settlements`(정산방 생성, 제목은 임시값으로 먼저 생성 후 OCR 확정 시 PATCH) → `POST /ocr/scan?settlement_id=`(멀티파트, Gemini OCR) → `POST /ocr/confirm`(status=waiting) → 멤버 참여(`POST /settlements/{id}/join`, QR 딥링크 `bilzy://join/{id}`) → `POST /settlements/{id}/calculate`(AI 정산, ai_note로 "안 먹은 항목" 전달) → `POST /settlements/{id}/done`.
- 영수증 이미지는 private 버킷 + signed URL(1시간 만료)로 응답됨 — 앱은 URL을 그대로 Coil에 넘기면 됨, 별도 처리 불필요.
- 보안: 계좌번호는 서버에서 AES-256-GCM 암호화 저장 후 평문 응답, rate limit 적용됨(`/auth/social` 10/min 등), 업로드는 실제 이미지 디코딩 검증.

## ⛔ 반드시 지킬 제품 결정 (다시 넣지 말 것)
- **스캔을 건너뛰는 직접 입력 진입점 없음.** ScanPermission 화면 등에서 카메라 스캔 자체를 건너뛰고 처음부터 수동으로 영수증을 입력하게 하는 진입점은 금지. (2026-09-09 업데이트: 단, OCR 인식 실패 폴백은 예외 — 촬영은 반드시 거친 뒤 AI 인식이 실패했을 때만 `RecognizingFragment`에 "다시 촬영하기"/"직접 입력할게요" 버튼을 보여주고 `ManualInputFragment`로 보내는 것은 허용. 사용자 결정 확인 완료, 새 프로토타입(`prototype_ref/`) 기준.)
- **저장 영수증 보관함 없음.** 마이페이지에 "저장한 영수증" 같은 별도 아카이브 기능을 추가하지 말 것. 영수증은 항상 정산건(settlement)에 종속되며 정산내역에서만 확인한다.
- **권한 안내 화면은 최초 1회만.** 카메라 등 권한을 이미 보유하고 있으면 rationale 화면을 건너뛰고 목적 화면으로 바로 진입시킬 것 (`ContextCompat.checkSelfPermission`로 분기).
- 프로토타입 HTML(`prototype_ref/`)에 있는 화면이라도 위 결정과 충돌하면 프로토타입보다 이 문서를 우선한다.

## 캐릭터 이미지 매핑 (`res/drawable/character_*.png`)
- 홈, 멤버 대기 = `character_side`
- 카메라 권한 화면 = `character_camera`
- 마이페이지/로그인로딩/정산내역 없음 = `character_smile`
- 정산내역 요약 = `character_couple`
- 정산 완료 = `ic_done_3d` (캐릭터 아님, 3D 체크)
- 그 외(참여자입력, 인원설정, 입장중, 로그인, 회원가입, 온보딩) = `character_front`

## 현재 진행 상황 (스냅샷 — 최신 상태는 코드로 재확인할 것)
**완료**: 카카오 로그인+토큰 자동 리프레시, 홈/마이페이지/계좌관리, 스캔→OCR→항목수정→확정, QR 생성/스캔/딥링크 입장, 정산방 흐름 전체(입장~완료), AI 정산 연동, 정산내역 목록/상세, 이미지 압축 + 인메모리 캐시(SWR) 응답속도 최적화, 보안 감사(IDOR/CORS/rate limit/이미지 검증) 1회 완료.
**2026-09-09 추가**: 새 프로토타입(`prototype_ver2/bilzy-prototype.jsx`) 반영 — ① OCR 인식 실패 시 "다시 촬영하기"/"직접 입력할게요" 폴백(ManualInputFragment 재연결, 스캔 필수 통과 조건 유지), ② 다차 정산(n차) UI 뼈대: 온보딩 4슬라이드, `ReceiptListFragment`/`RoundPickFragment` 신규, OcrResult "완료하기"/"추가 스캔하기" 분기, 금액조정/결과화면 라운드별 표시 구조. **주의**: 다차 정산은 UI만 완성됨 — 백엔드가 "정산방 1개=영수증 1장"만 지원해서 실제로는 마지막 영수증만 서버에 confirm됨(코드에 TODO로 명시). 실사용 가능한 기능화는 백엔드 재설계 필요(기존 확정 설계 `meetups` 테이블/스테퍼 방식과 새 프로토타입의 "누적 후 선택" 흐름이 서로 달라 재조율 필요, `feature-multi-round-settlement` 메모리 참고).
**미완**: 네이버 로그인, 초대 링크의 서명·만료 토큰화(현재 UUID 추측불가성에만 의존), 카카오 닉네임 미동의 유저 폴백("사용자") 개선 여지, 다차 정산 백엔드 연동(위 참고).
