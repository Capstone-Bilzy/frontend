# Bilzy — Android 클라이언트

영수증을 스캔하면 AI(Gemini)가 "안 먹은 항목" 등 특이사항을 반영해 1/N 정산을 계산해주는 모임 비용 정산 앱.
이 저장소는 **Android 클라이언트만** 포함한다. 백엔드(FastAPI + Supabase)는 별도 저장소.

이 파일은 이 저장소에서 작업하는 모든 서브에이전트(기획자/개발자/리뷰어/보안)가 공통으로 따라야 하는 규칙이다.
역할별 세부 지침은 `.claude/agents/planner.md`, `.claude/agents/developer.md`, `.claude/agents/reviewer.md`, `.claude/agents/security.md` 참고.
- planner/developer는 사용자가 명시적으로 요청할 때만 호출한다.
- **developer가 새 기능(단순 버그 수정/오탈자 제외) 구현을 마치면 reviewer와 security를 함께 자동 호출한다** — 사용자에게 매번 다시 요청받지 않아도 됨. 두 에이전트는 역할이 겹치지 않는다: reviewer는 정확성/제품결정/아키텍처만, security는 인증·인가·시크릿·입력검증 등 보안만 본다(백엔드 저장소도 포함해서 봄).
- **서브에이전트 비용/토큰 절감 규칙**: `developer`/`planner`/`reviewer`/`security`는 판단·품질이 중요해 `model: sonnet`으로 고정돼 있다(메인 세션이 다른 모델이어도 자동으로 물려받지 않음) — 낮추지 말 것. 반대로 "이 함수 어디서 호출돼?", "이 파일 어디 있어?" 같은 판단이 필요 없는 단순 탐색은 `lookup`(haiku 고정) 에이전트를 쓴다. 코드 작성/리뷰/보안 판단/기획 판단이 조금이라도 섞이면 lookup이 아니라 해당 역할 에이전트를 쓴다. 컨텍스트를 많이 차지하는 조사(파일 여러 개 훑기 등)는 fork를 우선 고려해 메인 대화 컨텍스트에 원본 tool 출력이 쌓이지 않게 한다.

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
**2026-09-09 추가**: 새 프로토타입(`prototype_ver2/bilzy-prototype.jsx`) 반영 — ① OCR 인식 실패 시 "다시 촬영하기"/"직접 입력할게요" 폴백(ManualInputFragment 재연결, 스캔 필수 통과 조건 유지), ② 다차 정산(n차) UI 뼈대: 온보딩 4슬라이드, `ReceiptListFragment`/`RoundPickFragment` 신규, OcrResult "완료하기"/"추가 스캔하기" 분기, 금액조정/결과화면 라운드별 표시 구조. 이 시점엔 UI만 있고 백엔드가 "정산방 1개=영수증 1장"만 지원해 실사용 불가능한 상태였음 — **2026-09-13에 아래대로 해결됨.**
**2026-09-11 추가**: 네이버 로그인 연동 완료(`NaverLoginManager` 신규, `LoginViewModel`/`SignupViewModel`/`SignupFragment` 카카오와 대칭 구조로 연결, `SignupTermsFragment`의 기존 네이버 분기 UI 재사용). 백엔드는 이미 완성돼 있었음. **실제 동작하려면 `local.properties`에 `NAVER_CLIENT_ID`/`NAVER_CLIENT_SECRET`(백엔드 `.env`와 동일 값)/`NAVER_CLIENT_NAME`을 사용자가 직접 추가해야 함**(값 없으면 로그인 버튼이 크래시 없이 안전하게 실패만 함, 검증됨).
**2026-09-11 추가**: 프로필 사진 표시 기능 완료 — 마이페이지/정산방 대기화면/정산결과화면에서 카카오·네이버 프로필 사진 표시(없으면 닉네임 이니셜 폴백). 백엔드 `get_settlement()`이 `users` 테이블 join으로 복호화된 `profile_image_url`을 멤버마다 내려주도록 수정(리뷰+보안점검 완료, 이슈 없음).
**2026-09-13 추가**: 다차 정산(n차) 백엔드+Android 실기능 연동 완료(영수증 1:N 재설계) — `receipts`(라운드별 영수증, `settlement_id`+`round` unique)/`settlement_member_rounds`(참여자별 라운드 참여·제외항목·금액) 테이블 신규, `/ocr/confirm`이 `round` 파라미터로 그 라운드만 갱신(예전엔 정산방 전체를 덮어써 다차 정산 자체가 불가능했던 핵심 버그 — 해결됨), `PATCH .../members/me/rounds`(RoundPick)·`.../rounds/{round}`(AmountAdjust 제외항목)·`.../ready`(조정 완료) 신규 엔드포인트, AI 계산이 라운드별 참여자/제외항목을 구조화해 반영. Android는 `ScanFlowViewModel.currentRound`/`RoundPickFragment`/`AmountAdjustFragment` 다차 루프로 실제 동작. 예전 "meetups 테이블+PeopleCount 스테퍼" 설계는 완전히 폐기, 프로토타입 ver2의 "누적 후 선택" UX(`ReceiptList`→"완료하기"/"추가 스캔하기"→`RoundPick`→`AmountAdjust`)로 확정(`feature-multi-round-settlement` 메모리 참고).
**2026-09-14 추가**: 호스트가 `RoundPickFragment`(참여 라운드 선택 화면)를 거치지 않고 `QrInviteFragment`에서 바로 입장하던 문제 수정 — 게스트(QrScan/JoinConfirm)는 원래도 RoundPick 전에 join을 먼저 완료했는데 호스트만 순서가 반대라 RoundPick의 라운드 저장이 멤버십 없음(404)으로 막힐 수 있었음(개발 중 security 점검에서 발견). `RoomViewModel.ensureMyMembershipAndAwait()` 신규(suspend, 409=이미 멤버는 성공 처리) 추가해 `QrInviteFragment`도 join 완료를 기다린 뒤에만 RoundPick으로 이동하도록 수정. 기존 `ensureMyMembership()`(fire-and-forget)은 `EnteringRoomFragment`에서 그대로 유지.
**2026-09-14 추가**: 초대 링크 서명·만료 토큰화 완료(백엔드+Android) — 딥링크가 `bilzy://join/{settlement_id}` → `bilzy://join/{settlement_id}?token={jwt}`로 변경. 백엔드: `POST /settlements/{id}/invite-token`(owner 전용, 24시간 만료 JWT, `type:"invite"`/`sid`/`epoch` 클레임, 기존 `JWT_SECRET` 재사용) 신규, `POST /settlements/{id}/join`이 `invite_token`을 받아 신규 참여자만 검증(owner·기존멤버는 면제 — 안 그러면 기존 자기참여 흐름이 깨짐). 초대 토큰 검증 실패는 401이 아니라 **403**으로 응답(401로 하면 Android `TokenAuthenticator`가 모든 401에 무조건 refresh+재시도를 걸어서 오작동 — 개발 중 리뷰에서 발견). 무효화(revocation): `settlements.invite_epoch` 컬럼(마이그레이션 `schema_invite_epoch.sql`) 추가, 토큰에 발급 당시 epoch을 심어 이후 epoch 불일치면 거부 — `QrInviteFragment`의 "QR 다시 만들기" 버튼이 epoch을 올려 기존에 뿌려진 QR을 전부 즉시 무효화(방장이 QR을 잘못 공유했을 때 대응용). 화면 진입/회전 시 자동 재발급은 epoch을 올리지 않아 기존 공유 링크를 깨지 않음. `JoinLink.kt`도 이번에 `Uri.parse()` 기반으로 재작성(기존엔 쿼리스트링 붙으면 id가 오염되는 버그가 있었음). 리뷰+보안점검 2라운드 완료, Critical/High 없음. `schema_multi_round.sql`/`schema_member_ready.sql`/`schema_invite_epoch.sql` 3개 마이그레이션 전부 Supabase에 적용 완료(사용자 확인, 2026-09-14) — 이제 다차 정산·ready 플래그·초대 토큰 무효화 전부 실 DB에서 동작.
**2026-09-14 추가**: 카카오/네이버 닉네임 미동의 게스트 폴백 개선 완료 — 기존엔 호스트만 `ParticipantInputFragment`(이름 입력 화면)를 거쳐서, 한 번도 호스트를 안 해본 게스트는 QR/딥링크로 들어올 때 이름을 고칠 기회 자체가 없어 영원히 "사용자"로 남았음(카카오 닉네임은 "선택 동의" 항목이라 사용자가 거부 가능 — 실제로 발생 가능한 케이스임을 확인). 이제 게스트도 닉네임이 기본값("사용자"/"참여자"/빈값)일 때만 `ParticipantInputFragment`를 거치도록 `QrScanFragment`/`JoinConfirmFragment`에 분기 추가(`QrScanViewModel.needsNicknamePrompt()`), 실명이 이미 있으면 기존처럼 마찰 없이 곧장 join. 리뷰+보안점검 완료, blocking 이슈 없음 — 다만 서버 닉네임 길이 제한(20자)에 맞춰 `fragment_participant_input.xml`의 `etName`에 `android:maxLength="20"` 추가(20자 초과 입력 시 서버 422를 애매한 에러 메시지로만 보여주던 문제 방지).
**미완**: 네이버 로그인 실기기 크리덴셜 테스트(사용자가 local.properties 설정 후 확인 필요), `GET /settlements/{id}/result` 엔드포인트는 프로필 이미지 join 안 됨(현재 클라이언트 미사용이라 안전, 죽은 엔드포인트 정리 또는 join 로직 공용화 검토 여지), 호스트가 스캔한 라운드에 본인이 안 꼈다고 착각할 수 있는 케이스는 RoundPick 게이팅(최소 1개 선택)만으로 방어 중(근본적으로는 UX 문구 보강 여지), 라운드가 1개뿐인 일반(비-다차) 정산에서도 RoundPick에서 수동으로 칩을 눌러야 다음으로 넘어감(자동 선택/스킵 고려 여지), 초대 토큰 "QR 다시 만들기"는 QrInviteFragment(정산방 생성 시 1회성 화면)에 있어야만 쓸 수 있음 — 화면을 나간 뒤(예: 며칠 뒤) 잘못 공유를 알아챈 경우엔 무효화할 UI 진입점이 없음(백엔드 능력은 있음, 재진입 화면/버튼 추가는 별도 작업), `POST /settlements/{id}/invite-token` 재발급 시 epoch 증가가 read-then-update라 완전히 원자적이지 않음(방장 1인·rate limit 30/min이라 실사용 영향은 낮다고 판단, 문제 리포트 들어오면 DB 표현식 기반 원자적 증가로 교체), 안 쓰는 `GET /settlements/{id}/qr` 엔드포인트에 rate limit 없음(죽은 엔드포인트, 정리 후보).
