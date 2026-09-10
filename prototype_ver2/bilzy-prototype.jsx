import React, { useState, useEffect, useRef, useCallback, useMemo } from "react";

/* ==================================================================
   Bilzy 프로토타입
   온보딩 → 회원가입 → 홈 → 카메라 권한 → 영수증 스캔(n차)
   → 인식(성공/실패) → OCR 결과 편집 → 영수증 목록 → 저장
   → 정산 인원 설정 → QR 정산방 초대
================================================================== */

const C = {
  bg: "#080B24",
  purple: "#6C5CE7",
  kakao: "#FEE500",
  naver: "#03C75A",
};

const won = (n) => `${Number(n || 0).toLocaleString("ko-KR")}원`;
const ROUND_KO = ["1차", "2차", "3차", "4차", "5차", "6차"];

/* ============================ 마스코트 ============================ */

function Blob({ id = "m" }) {
  return (
    <g>
      <defs>
        <radialGradient id={`body-${id}`} cx="38%" cy="28%" r="78%">
          <stop offset="0%" stopColor="#E9FFD9" />
          <stop offset="32%" stopColor="#A9F585" />
          <stop offset="70%" stopColor="#5FDD5F" />
          <stop offset="100%" stopColor="#2FA84A" />
        </radialGradient>
        <linearGradient id={`ring-${id}`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#D6FFC9" />
          <stop offset="50%" stopColor="#7DE87D" />
          <stop offset="100%" stopColor="#CFFFE0" />
        </linearGradient>
      </defs>
      <g transform="rotate(-13 100 116)">
        <ellipse cx="100" cy="116" rx="86" ry="23" fill="none" stroke={`url(#ring-${id})`} strokeWidth="5" opacity="0.95" />
      </g>
      <path
        d="M100,32 C136,32 164,60 164,96 C164,128 138,148 114,167 C106,174 100,177 100,177
           C100,177 94,174 86,167 C62,148 36,128 36,96 C36,60 64,32 100,32 Z"
        fill={`url(#body-${id})`}
      />
      <ellipse cx="76" cy="66" rx="20" ry="13" fill="#fff" opacity="0.5" transform="rotate(-25 76 66)" />
      <ellipse cx="128" cy="132" rx="16" ry="9" fill="#fff" opacity="0.18" transform="rotate(20 128 132)" />
      <g className="bz-eyes">
        <ellipse cx="84" cy="94" rx="8.5" ry="15" fill="#111018" />
        <ellipse cx="117" cy="94" rx="8.5" ry="15" fill="#111018" />
      </g>
      <g transform="rotate(-13 100 116)">
        <path d="M14,116 A86,23 0 0 0 186,116" fill="none" stroke={`url(#ring-${id})`} strokeWidth="5" strokeLinecap="round" />
      </g>
    </g>
  );
}

function MascotHero({ variant, size = 220, pad = false }) {
  return (
    <div className="bz-mascot-wrap" style={{ width: size + 16, height: size + 16 }}>
      <div className="bz-glow" style={{ width: size * 1.22, height: size * 1.22 }} />
      <svg viewBox="0 0 200 200" className="bz-mascot" style={{ width: size, height: size }} aria-hidden="true">
        {variant === "duo" ? (
          <g>
            <g transform="translate(-6,26) scale(0.62) rotate(-9 100 100)"><Blob id="d1" /></g>
            <g transform="translate(84,26) scale(0.62) rotate(9 100 100)"><Blob id="d2" /></g>
            <g className="bz-pop" fill="#7DE87D">
              <rect x="92" y="20" width="5" height="15" rx="2.5" transform="rotate(-16 94 27)" />
              <rect x="106" y="16" width="5" height="15" rx="2.5" />
              <rect x="120" y="20" width="5" height="15" rx="2.5" transform="rotate(16 122 27)" />
            </g>
          </g>
        ) : (
          <g>
            {pad && <ellipse cx="100" cy="182" rx="70" ry="15" fill="#5B48C8" opacity="0.55" />}
            <Blob id={variant} />
            {variant === "camera" && (
              <g className="bz-camera">
                <rect x="66" y="118" width="70" height="48" rx="13" fill="#8C7BEE" />
                <rect x="66" y="118" width="70" height="16" rx="8" fill="#A79BF6" />
                <circle cx="101" cy="143" r="16" fill="#5F4FC7" />
                <circle cx="101" cy="143" r="9" fill="#C9C0FF" />
                <circle cx="126" cy="128" r="4" fill="#FFF6A8" />
                <path d="M58,120 q-14,10 -4,26" stroke="#5FDD5F" strokeWidth="13" strokeLinecap="round" fill="none" />
                <path d="M144,120 q14,10 4,26" stroke="#5FDD5F" strokeWidth="13" strokeLinecap="round" fill="none" />
                <g className="bz-sparks" fill="#EDEAFF">
                  <path d="M166,52 l5,-14 l5,14 l14,5 l-14,5 l-5,14 l-5,-14 l-14,-5 z" />
                  <path d="M150,76 l3,-8 l3,8 l8,3 l-8,3 l-3,8 l-3,-8 l-8,-3 z" />
                </g>
              </g>
            )}
            {variant === "spark" && (
              <g className="bz-sparks" fill="#8DF08D">
                <path d="M32,84 l4,-11 l4,11 l11,4 l-11,4 l-4,11 l-4,-11 l-11,-4 z" />
                <path d="M170,142 l3.5,-9 l3.5,9 l9,3.5 l-9,3.5 l-3.5,9 l-3.5,-9 l-9,-3.5 z" />
                <circle cx="164" cy="72" r="3.5" />
                <circle cx="40" cy="152" r="3" />
              </g>
            )}
            {variant === "receipt" && (
              <g className="bz-heldpaper">
                <path d="M44,18 h40 v70 l-8,-5 -8,5 -8,-5 -8,5 -8,-5 v-65 z" fill="#F2F0FF" stroke="#CFC9F5" strokeWidth="2" strokeLinejoin="round" />
                <g stroke="#B9B2E8" strokeWidth="3.4" strokeLinecap="round">
                  <path d="M53,32 h22M53,43 h26M53,54 h16M53,65 h24" />
                </g>
              </g>
            )}
            {variant === "hello" && (
              <g className="bz-pop" fill="#7DE87D">
                <rect x="150" y="46" width="5" height="14" rx="2.5" transform="rotate(18 152 53)" />
                <rect x="164" y="58" width="5" height="12" rx="2.5" transform="rotate(30 166 64)" />
              </g>
            )}
          </g>
        )}
      </svg>
    </div>
  );
}

const MascotMini = ({ size = 56 }) => (
  <svg viewBox="0 0 200 200" width={size} height={size} className="bz-mini" aria-hidden="true">
    <Blob id={`mini-${size}`} />
  </svg>
);

/* 저장 완료 영수증 일러스트 */
const ReceiptArt = () => (
  <svg viewBox="0 0 260 260" width="240" height="240" className="bz-receipt-art" aria-hidden="true">
    <defs>
      <linearGradient id="rc" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="#E6E2FF" />
        <stop offset="55%" stopColor="#C6BDFB" />
        <stop offset="100%" stopColor="#9C8FF0" />
      </linearGradient>
      <linearGradient id="cn" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="#DAD3FF" />
        <stop offset="100%" stopColor="#9384EE" />
      </linearGradient>
    </defs>
    <g transform="rotate(-11 130 130)">
      <path d="M74,52 h108 a16,16 0 0 1 16,16 v128 l-15,-9 -15,9 -15,-9 -15,9 -15,-9 -15,9 -15,-9 -15,9 V68 a16,16 0 0 1 16,-16 Z" fill="url(#rc)" />
      <path d="M182,52 a16,16 0 0 1 16,16 a16,16 0 0 1 -16,16 z" fill="#8878E8" opacity=".55" />
      <g fill="#8D7FE9" opacity=".55">
        <rect x="98" y="80" width="62" height="11" rx="5.5" />
        <rect x="90" y="104" width="44" height="11" rx="5.5" />
        <rect x="150" y="104" width="22" height="11" rx="5.5" />
        <rect x="90" y="128" width="52" height="11" rx="5.5" />
        <rect x="156" y="128" width="16" height="11" rx="5.5" />
        <rect x="112" y="164" width="60" height="11" rx="5.5" />
      </g>
      <text x="96" y="166" fontSize="42" fontWeight="800" fill="#8D7FE9" opacity=".7">₩</text>
    </g>
    <g className="bz-coin-a">
      <circle cx="46" cy="104" r="19" fill="url(#cn)" />
      <text x="46" y="112" fontSize="18" fontWeight="800" fill="#6E5DDA" textAnchor="middle">₩</text>
    </g>
    <g className="bz-coin-b">
      <circle cx="216" cy="186" r="15" fill="url(#cn)" />
      <text x="216" y="192" fontSize="14" fontWeight="800" fill="#6E5DDA" textAnchor="middle">₩</text>
    </g>
  </svg>
);

/* QR 코드 (의사 생성) */
function QrCode({ value = "bilzy", size = 190 }) {
  const N = 25;
  const cells = useMemo(() => {
    let h = 2166136261;
    for (let i = 0; i < value.length; i++) { h ^= value.charCodeAt(i); h = Math.imul(h, 16777619); }
    const rnd = () => { h ^= h << 13; h ^= h >>> 17; h ^= h << 5; return ((h >>> 0) % 1000) / 1000; };
    const g = Array.from({ length: N }, () => Array(N).fill(0));
    const finder = (r, c) => {
      for (let i = 0; i < 7; i++) for (let j = 0; j < 7; j++) {
        const edge = i === 0 || i === 6 || j === 0 || j === 6;
        const core = i >= 2 && i <= 4 && j >= 2 && j <= 4;
        g[r + i][c + j] = edge || core ? 1 : 0;
      }
    };
    const inFinder = (r, c) =>
      (r < 8 && c < 8) || (r < 8 && c > N - 9) || (r > N - 9 && c < 8);
    for (let r = 0; r < N; r++) for (let c = 0; c < N; c++) if (!inFinder(r, c)) g[r][c] = rnd() > 0.5 ? 1 : 0;
    finder(0, 0); finder(0, N - 7); finder(N - 7, 0);
    return g;
  }, [value]);
  const u = size / N;
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="bz-qr" aria-label="정산방 QR 코드">
      <rect width={size} height={size} fill="#fff" />
      {cells.map((row, r) =>
        row.map((v, c) => (v ? <rect key={`${r}-${c}`} x={c * u} y={r * u} width={u} height={u} fill="#0B0B18" /> : null))
      )}
    </svg>
  );
}

/* ============================ 공통 UI ============================ */

const Logo = ({ size = 44 }) => <span className="bz-logo" style={{ fontSize: size }}>Bilzy</span>;

function StatusBar() {
  return (
    <div className="bz-status">
      <span className="bz-time">15:00</span>
      <div className="bz-status-icons">
        <svg width="17" height="11" viewBox="0 0 17 11" fill="none">
          {[0, 1, 2, 3].map((i) => <rect key={i} x={i * 4.4} y={8 - i * 2.6} width="3" height={3 + i * 2.6} rx="1" fill="#fff" />)}
        </svg>
        <svg width="16" height="12" viewBox="0 0 16 12" fill="none">
          <path d="M8 10.2 L5.2 7.2 a4 4 0 0 1 5.6 0 Z" fill="#fff" />
          <path d="M2.6 4.6 a7.6 7.6 0 0 1 10.8 0" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" />
          <path d="M4.9 7 a4.4 4.4 0 0 1 6.2 0" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" />
        </svg>
        <svg width="25" height="12" viewBox="0 0 25 12" fill="none">
          <rect x="0.5" y="0.5" width="21" height="11" rx="3.2" stroke="#fff" strokeOpacity="0.5" />
          <rect x="2" y="2" width="18" height="8" rx="2" fill="#fff" />
          <path d="M23 4.2v3.6a2 2 0 0 0 0-3.6Z" fill="#fff" fillOpacity="0.5" />
        </svg>
      </div>
    </div>
  );
}

const BackArrow = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
    <path d="M19 12H5m0 0 6-6m-6 6 6 6" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const TopBar = ({ title, onBack, badge }) => (
  <div className="bz-topbar">
    <button className="bz-back" onClick={onBack} aria-label="뒤로가기"><BackArrow /></button>
    <span className="bz-topbar-title">{title}</span>
    {badge && <span className="bz-round-badge"><i />{badge}</span>}
  </div>
);

/* 로고 + 뒤로가기가 함께 있는 헤더 (영수증 목록 / OCR / 인원설정 / QR) */
const BrandBar = ({ title, onBack, badge }) => (
  <div className="bz-brandbar">
    <div className="bz-brandbar-logo"><Logo size={26} /></div>
    <div className="bz-brandbar-row">
      <button className="bz-back" onClick={onBack} aria-label="뒤로가기"><BackArrow /></button>
      <span>{title}</span>
      {badge && <span className="bz-round-badge"><i />{badge}</span>}
    </div>
  </div>
);

const CheckCircle = ({ on, size = 22 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className="bz-check">
    <circle cx="12" cy="12" r="10" stroke={on ? C.purple : "rgba(255,255,255,0.28)"} strokeWidth="1.8" />
    <path d="M7.6 12.3l3 3 5.8-6" stroke={on ? C.purple : "rgba(255,255,255,0.28)"} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const Chevron = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
    <path d="m9 5 7 7-7 7" stroke="rgba(255,255,255,0.55)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const KakaoIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="#3B1E1E">
    <path d="M12 3C6.9 3 2.8 6.2 2.8 10.2c0 2.6 1.7 4.8 4.3 6.1l-1 3.6c-.1.4.3.7.6.5l4.3-2.8c.3 0 .7.1 1 .1 5.1 0 9.2-3.2 9.2-7.5S17.1 3 12 3Z" />
  </svg>
);
const NaverIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="#fff"><path d="M14.2 12.5 9.5 5.5H5v13h4.6v-7l4.7 7H19v-13h-4.8v7Z" /></svg>
);

const ScanGlyph = ({ s = 30, sw = 1.8, color = "currentColor" }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={sw} strokeLinecap="round">
    <path d="M3 8V5a2 2 0 0 1 2-2h3M16 3h3a2 2 0 0 1 2 2v3M21 16v3a2 2 0 0 1-2 2h-3M8 21H5a2 2 0 0 1-2-2v-3M3 12h18" />
  </svg>
);
const QrGlyph = ({ s = 30, color = "currentColor" }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="1.8" strokeLinecap="round">
    <path d="M3 8V5a2 2 0 0 1 2-2h3M16 3h3a2 2 0 0 1 2 2v3M21 16v3a2 2 0 0 1-2 2h-3M8 21H5a2 2 0 0 1-2-2v-3" />
    <rect x="7" y="7" width="4" height="4" rx="1" /><rect x="13" y="7" width="4" height="4" rx="1" />
    <rect x="7" y="13" width="4" height="4" rx="1" /><rect x="13" y="13" width="4" height="4" rx="1" />
  </svg>
);
const DocGlyph = ({ s = 26, color = "currentColor" }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M6 3h12v18l-2-1.4-2 1.4-2-1.4-2 1.4-2-1.4L6 21V3Z" /><path d="M9 8h6M9 12h6M9 16h3" />
  </svg>
);
const OkGlyph = ({ s = 26, color = "currentColor" }) => (
  <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="9" /><path d="m8.2 12.3 2.6 2.6 5-5.4" />
  </svg>
);

const Corners = ({ className = "" }) => (
  <div className={`bz-corners ${className}`}>
    <span className="tl" /><span className="tr" /><span className="bl" /><span className="br" />
  </div>
);

/* ============================ 온보딩 ============================ */

const SLIDES = [
  { v: "planet", title: ["영수증 스캔부터 맞춤 정산까지,", "Bilzy 하나면 충분해요!"], sub: ["복잡한 계산 없이 정산을 간편하게 끝내보세요"] },
  { v: "camera", title: ["일일이 입력하지 않아도,", "정산이 쉬워져요"], sub: ["메뉴와 금액을 AI가 알아서 정리해", "복잡한 정산을 간편하게 시작할 수 있어요."] },
  { v: "duo", title: ["참여한 것만 골라서", "한 번에 정산해요"], sub: ["1차 식사부터 2차 카페, 3차 모임까지", "내가 참여한 정산만 선택하면 끝!"] },
  { v: "spark", title: ["모임이 끝났을 때,", "정산도 끝!"], sub: ["모임 후 번거로운 정산 스트레스를 줄여보세요"] },
];

function Onboarding({ onSignup, onLogin }) {
  const [i, setI] = useState(0);
  const [drag, setDrag] = useState(0);
  const [auto, setAuto] = useState(true);
  const start = useRef(null);
  const W = 390;

  useEffect(() => {
    if (!auto) return;
    const t = setTimeout(() => setI((p) => (p + 1) % SLIDES.length), 4200);
    return () => clearTimeout(t);
  }, [i, auto]);

  const down = (x) => { start.current = x; setAuto(false); };
  const move = (x) => {
    if (start.current === null) return;
    let d = x - start.current;
    if ((i === 0 && d > 0) || (i === SLIDES.length - 1 && d < 0)) d *= 0.3;
    setDrag(d);
  };
  const up = () => {
    if (start.current === null) return;
    if (drag < -55 && i < SLIDES.length - 1) setI(i + 1);
    else if (drag > 55 && i > 0) setI(i - 1);
    start.current = null; setDrag(0);
  };
  const go = useCallback((n) => { setAuto(false); setI(n); }, []);

  useEffect(() => {
    const k = (e) => {
      if (e.key === "ArrowRight") go(Math.min(i + 1, SLIDES.length - 1));
      if (e.key === "ArrowLeft") go(Math.max(i - 1, 0));
    };
    window.addEventListener("keydown", k);
    return () => window.removeEventListener("keydown", k);
  }, [i, go]);

  return (
    <div className="bz-screen bz-onb">
      <StatusBar />
      <div className="bz-dots">
        {SLIDES.map((_, n) => (
          <button key={n} onClick={() => go(n)} className={`bz-dot ${n === i ? "on" : ""}`} aria-label={`${n + 1}번째 화면`} />
        ))}
      </div>
      <div
        className="bz-swipe"
        onMouseDown={(e) => down(e.clientX)} onMouseMove={(e) => move(e.clientX)}
        onMouseUp={up} onMouseLeave={up}
        onTouchStart={(e) => down(e.touches[0].clientX)} onTouchMove={(e) => move(e.touches[0].clientX)} onTouchEnd={up}
      >
        <div className="bz-track" style={{
          width: W * SLIDES.length,
          transform: `translateX(${-i * W + drag}px)`,
          transition: start.current === null ? "transform .55s cubic-bezier(.22,1,.36,1)" : "none",
        }}>
          {SLIDES.map((s, n) => (
            <div className="bz-slide" key={n} style={{ width: W }}>
              <div className="bz-copy" key={`${n}-${i === n}`}>
                <h1 className={i === n ? "bz-in-1" : ""}>{s.title.map((t, k) => <span key={k}>{t}</span>)}</h1>
                <p className={i === n ? "bz-in-2" : ""}>{s.sub.map((t, k) => <span key={k}>{t}</span>)}</p>
              </div>
              <div className={`bz-art ${i === n ? "bz-in-3" : ""}`}>{i === n && <MascotHero variant={s.v} />}</div>
            </div>
          ))}
        </div>
      </div>
      <div className="bz-onb-foot">
        <p className="bz-have">이미 계정이 있으신가요? <button className="bz-link" onClick={onLogin}>로그인</button></p>
        <button className="bz-btn bz-btn-outline" onClick={onSignup}>회원가입</button>
      </div>
    </div>
  );
}

/* ============================ 회원가입 ============================ */

const P = {
  kakao: { name: "카카오", label: "Kakao", cta: "동의하고 계속하기", cls: "bz-kakao", icon: <KakaoIcon /> },
  naver: { name: "네이버", label: "Naver", cta: "네이버로 시작하기", cls: "bz-naver", icon: <NaverIcon /> },
};

function Social({ onPick, onBack }) {
  return (
    <div className="bz-screen bz-center-glow">
      <StatusBar />
      <div className="bz-social-body">
        <div className="bz-social-logo">
          <div className="bz-social-mascot"><MascotMini size={72} /></div>
          <Logo size={56} />
        </div>
        <p className="bz-social-sub"><span>Bilzy와 함께</span><span>정산을 시작해보세요</span></p>
        <div className="bz-social-btns">
          <button className="bz-btn bz-kakao" onClick={() => onPick("kakao")}><KakaoIcon /> 카카오로 3초 만에 로그인</button>
          <button className="bz-btn bz-naver" onClick={() => onPick("naver")}><NaverIcon /> 네이버로 3초 만에 로그인</button>
        </div>
      </div>
      <button className="bz-ghost-back" onClick={onBack}>← 온보딩으로</button>
    </div>
  );
}

function Consent({ provider, onNext, onBack }) {
  const p = P[provider];
  const items = [
    { t: "이름", d: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-4 0-7 2-7 4.5V20h14v-1.5C19 16 16 14 12 14Z" },
    { t: "프로필 이미지", d: "M4 5h16v14H4V5Zm2.5 11.5h11l-3.5-4.5-2.6 3.3-1.8-2.2-3.1 3.4ZM9 9.5a1.3 1.3 0 1 1-2.6 0 1.3 1.3 0 0 1 2.6 0Z" },
    { t: "이메일", d: "M4 6h16v12H4V6Zm1.6 1.6 6.4 4.6 6.4-4.6H5.6Z" },
  ];
  return (
    <div className="bz-screen">
      <StatusBar />
      <TopBar title={p.name} onBack={onBack} />
      <div className="bz-consent-body">
        <div className="bz-consent-head"><Logo size={46} /><p>{p.name}로 간편하게 시작하세요</p></div>
        <div className="bz-card-consent">
          <div className="bz-profile">
            <div className="bz-avatar" />
            <div><strong>정민지</strong><span>{p.label} 계정 정보 활용 동의</span></div>
          </div>
          <div className="bz-divider" />
          <h3 className="bz-req-title">요청 항목</h3>
          <ul className="bz-req">
            {items.map((it, n) => (
              <li key={n} style={{ animationDelay: `${0.1 + n * 0.08}s` }}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="rgba(255,255,255,0.75)"><path d={it.d} /></svg>
                <span>{it.t}</span><CheckCircle on size={21} />
              </li>
            ))}
          </ul>
          <div className="bz-note"><span>계정 연동을 진행하면</span><span>이용약관 및 개인정보처리방침에 동의하게 됩니다.</span></div>
        </div>
      </div>
      <div className="bz-foot"><button className={`bz-btn ${p.cls}`} onClick={onNext}>{p.icon} {p.cta}</button></div>
    </div>
  );
}

const TERMS = [
  { k: "tos", t: "이용약관 (필수)", req: true },
  { k: "privacy", t: "개인정보 수집 및 이용 (필수)", req: true },
  { k: "loc", t: "위치기반 서비스 이용약관 (선택)", req: false },
  { k: "mkt", t: "마케팅 정보 수신 동의 (선택)", req: false },
];

function Terms({ provider, onNext, onBack }) {
  const p = P[provider];
  const [on, setOn] = useState({ tos: false, privacy: false, loc: false, mkt: false });
  const [peek, setPeek] = useState(null);
  const all = TERMS.every((t) => on[t.k]);
  const ok = TERMS.filter((t) => t.req).every((t) => on[t.k]);

  return (
    <div className="bz-screen">
      <StatusBar />
      <TopBar title={p.name} onBack={onBack} />
      <div className="bz-terms-body">
        <div className={`bz-terms-head ${ok ? "lit" : ""}`}>
          <Logo size={46} />
          <p><span>Bilzy 이용을 위한</span><span>필수 약관에 동의해주세요</span></p>
        </div>
        <button className={`bz-all ${all ? "on" : ""}`} onClick={() => {
          const v = !all; setOn({ tos: v, privacy: v, loc: v, mkt: v });
        }}>
          <span className={`bz-all-check ${all ? "on" : ""}`}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M6 12.5l4 4 8-9" stroke={all ? C.purple : "rgba(255,255,255,.45)"} strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
          모두 확인하였으며 동의합니다.
        </button>
        <ul className="bz-term-list">
          {TERMS.map((t) => (
            <li key={t.k}>
              <button className="bz-term-toggle" onClick={() => setOn({ ...on, [t.k]: !on[t.k] })}>
                <CheckCircle on={on[t.k]} /><span className={on[t.k] ? "on" : ""}>{t.t}</span>
              </button>
              <button className="bz-term-more" onClick={() => setPeek(t)} aria-label={`${t.t} 자세히 보기`}><Chevron /></button>
            </li>
          ))}
        </ul>
      </div>
      <div className="bz-foot">
        <button className={`bz-btn ${p.cls} ${ok ? "" : "bz-disabled"}`} onClick={() => ok && onNext()} disabled={!ok}>
          {p.icon} {p.cta}
        </button>
      </div>
      {peek && (
        <div className="bz-sheet-bg" onClick={() => setPeek(null)}>
          <div className="bz-sheet" onClick={(e) => e.stopPropagation()}>
            <div className="bz-sheet-grab" />
            <h3>{peek.t}</h3>
            <div className="bz-sheet-text">
              <p>제1조 (목적) 본 약관은 Bilzy가 제공하는 정산 서비스의 이용 조건과 절차, 회원과 회사의 권리·의무를 정합니다.</p>
              <p>제2조 (정의) “정산방”이란 영수증 스캔 또는 QR 참여를 통해 생성된 모임 단위의 정산 공간을 말합니다.</p>
              <p>제3조 (이용) 회원은 모임 참여 내역을 선택해 정산 금액을 확인하고, 참여자에게 정산을 요청할 수 있습니다.</p>
              <p>제4조 (데이터) 회사는 영수증 이미지에서 메뉴와 금액을 인식하기 위해 필요한 범위에서 데이터를 처리합니다.</p>
            </div>
            <button className="bz-btn bz-btn-purple" onClick={() => { setOn({ ...on, [peek.k]: true }); setPeek(null); }}>확인하고 동의</button>
          </div>
        </div>
      )}
    </div>
  );
}

function Loading({ onDone }) {
  const [pct, setPct] = useState(6);
  useEffect(() => {
    const iv = setInterval(() => setPct((p) => Math.min(p + Math.random() * 14 + 5, 100)), 260);
    const t = setTimeout(onDone, 2600);
    return () => { clearInterval(iv); clearTimeout(t); };
  }, [onDone]);
  return (
    <div className="bz-screen bz-center-glow">
      <StatusBar />
      <div className="bz-loading">
        <Logo size={52} />
        <p className="bz-loading-copy"><span>반가워요!</span><span>잠시만 기다려주세요</span></p>
        <div className="bz-loading-art"><MascotHero variant="hello" size={190} /></div>
        <div className="bz-bar"><div className="bz-bar-fill" style={{ width: `${pct}%` }} /></div>
        <span className="bz-loading-label">로그인 중입니다</span>
      </div>
    </div>
  );
}

/* ============================ 홈 ============================ */

function BottomNav({ tab, onTab }) {
  const items = [
    { k: "home", t: "홈", d: "M4 11 12 4l8 7v8a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1v-8Z" },
    { k: "scan", t: "스캔", d: "M3 8V5a2 2 0 0 1 2-2h3M16 3h3a2 2 0 0 1 2 2v3M21 16v3a2 2 0 0 1-2 2h-3M8 21H5a2 2 0 0 1-2-2v-3M3 12h18" },
    { k: "hist", t: "정산내역", d: "M3 12a9 9 0 1 0 3-6.7M3 4v4h4M12 8v4l3 2" },
    { k: "my", t: "마이페이지", d: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-8 9c0-3.3 3.6-5.5 8-5.5s8 2.2 8 5.5" },
  ];
  return (
    <nav className="bz-nav">
      {items.map((n) => (
        <button key={n.k} className={`bz-nav-item ${tab === n.k ? "on" : ""}`} onClick={() => onTab(n.k)}>
          <span className={`bz-nav-icon ${tab === n.k ? "on" : ""}`}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <path d={n.d} />
            </svg>
          </span>
          <span>{n.t}</span>
        </button>
      ))}
    </nav>
  );
}

function Home({ history, onStart, onJoin, onTab, toast }) {
  return (
    <div className="bz-screen bz-home">
      <StatusBar />
      <div className="bz-home-scroll">
        <div className="bz-home-top"><Logo size={26} /></div>
        <div className="bz-hero">
          <h2><span>오늘 모임 정산,</span><span>자리에서 바로 끝내요</span></h2>
          <div className="bz-hero-mascot"><MascotMini size={78} /></div>
        </div>
        <div className="bz-actions">
          <button className="bz-action bz-action-payer" onClick={onStart}>
            <span className="bz-tag">결제자</span>
            <ScanGlyph s={34} color="#fff" />
            <strong>정산 시작하기</strong>
            <em>영수증 스캔으로<br />정산방 만들기</em>
          </button>
          <button className="bz-action bz-action-joiner" onClick={onJoin}>
            <span className="bz-tag">참여자</span>
            <QrGlyph s={34} color="#fff" />
            <strong>정산 참여하기</strong>
            <em>QR 스캔으로<br />정산방 입장</em>
          </button>
        </div>
        <div className="bz-card-history">
          <div className="bz-history-head">
            <h3>최근 정산 내역</h3>
            <button onClick={() => toast("정산 내역 전체보기")}>전체보기 ›</button>
          </div>
          <ul className="bz-history">
            {history.map((h, n) => (
              <li key={n} onClick={() => toast(`${h.t} 상세 보기`)} style={{ animationDelay: `${0.05 * n}s` }}>
                <div className="bz-avatar-sm" />
                <div className="bz-history-txt"><strong>{h.t}</strong><span>{h.d}</span></div>
                <b>{h.a}</b>
              </li>
            ))}
          </ul>
        </div>
      </div>
      <BottomNav tab="home" onTab={onTab} />
    </div>
  );
}

/* ============================ 카메라 권한 ============================ */

function CamPermission({ onAllow, onTab }) {
  return (
    <div className="bz-screen bz-center-glow">
      <StatusBar />
      <div className="bz-home-top" style={{ padding: "10px 20px 0" }}><Logo size={26} /></div>
      <div className="bz-perm-body">
        <MascotHero variant="camera" size={210} pad />
        <h2 className="bz-perm-title">영수증 스캔에 카메라 권한이 필요해요</h2>
        <p className="bz-perm-sub"><span>영수증의 상세 내역을</span><span>자동으로 채워드릴게요</span></p>
        <button className="bz-btn bz-btn-outline" onClick={onAllow}>권한 허용하기</button>
      </div>
      <BottomNav tab="scan" onTab={onTab} />
    </div>
  );
}

/* ============================ 스캔 뷰파인더 ============================ */

function Scanner({ round, initialMode = "receipt", onShoot, onQrShoot, onBack, toast }) {
  const [mode, setMode] = useState(initialMode);
  const [askPhoto, setAskPhoto] = useState(false);
  const shoot = () => (mode === "receipt" ? onShoot() : onQrShoot());

  return (
    <div className="bz-screen">
      <StatusBar />
      <TopBar title="스캔" onBack={onBack} badge={mode === "receipt" && round ? ROUND_KO[round - 1] : null} />
      <div className="bz-viewfinder">
        <Corners />
        <div className="bz-vf-center">
          <div className="bz-vf-glyph">
            {mode === "receipt"
              ? <ScanGlyph s={62} sw={2.4} color="rgba(255,255,255,.42)" />
              : <QrGlyph s={62} color="rgba(255,255,255,.42)" />}
          </div>
          <p>
            {mode === "receipt"
              ? <><span>정산해야 하는 영수증의</span><span>결제 정보가 나오게 찍어주세요</span></>
              : <><span>초대 받은 정산방의</span><span>QR코드가 나오게 찍어주세요</span></>}
          </p>
        </div>
        <div className="bz-scanline" />
        {askPhoto && (
          <div className="bz-ios-bg" onClick={() => setAskPhoto(false)}>
            <div className="bz-ios-dialog" onClick={(e) => e.stopPropagation()}>
              <div className="bz-ios-icon">
                <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#8B87F5" strokeWidth="1.8" strokeLinejoin="round">
                  <rect x="3" y="4" width="18" height="16" rx="3" />
                  <path d="m5.5 17 4-4.6 2.6 3 2.4-2.8L19 17" /><circle cx="8.4" cy="9.2" r="1.4" />
                </svg>
              </div>
              <h4><span>‘Bilzy’가 사진에</span><span>접근하도록 허용하시겠습니까?</span></h4>
              <p><span>영수증 이미지를 선택하여</span><span>스캔할 수 있습니다.</span></p>
              <div className="bz-ios-actions">
                <button onClick={() => { setAskPhoto(false); shoot(); }}>사진 선택...</button>
                <button onClick={() => { setAskPhoto(false); shoot(); }}>모든 사진에 접근 허용</button>
                <button onClick={() => { setAskPhoto(false); toast("사진 접근을 허용하지 않았어요"); }}>허용 안 함</button>
              </div>
            </div>
          </div>
        )}
      </div>
      <div className="bz-shutter-row">
        <button className="bz-album" onClick={() => setAskPhoto(true)} aria-label="앨범에서 선택" />
        <button className="bz-shutter" onClick={shoot} aria-label="촬영" />
        <div className="bz-shutter-spacer" />
      </div>
      <div className="bz-mode-row">
        {(initialMode === "receipt" ? ["qr", "receipt"] : ["receipt", "qr"]).map((m) => (
          <button key={m} className={mode === m ? "on" : ""} onClick={() => setMode(m)}>
            {m === "qr" ? "QR 스캔" : "영수증 스캔"}<i />
          </button>
        ))}
      </div>
    </div>
  );
}

/* ============================ 인식 진행 / 실패 ============================ */

const STEPS = [
  { k: 0, t: "영수증 인식", icon: ScanGlyph },
  { k: 1, t: "정보 추출", icon: DocGlyph },
  { k: 2, t: "내역 확인", icon: OkGlyph },
];

function Processing({ round, willFail, onDone, onFail, onRetry, onManual, onBack }) {
  const [stage, setStage] = useState(0);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    if (failed) return;
    if (willFail) {
      const t = setTimeout(() => { setFailed(true); onFail(); }, 1700);
      return () => clearTimeout(t);
    }
    if (stage < 2) {
      const t = setTimeout(() => setStage((s) => s + 1), 1200);
      return () => clearTimeout(t);
    }
    const t = setTimeout(onDone, 1300);
    return () => clearTimeout(t);
  }, [stage, willFail, failed, onDone, onFail]);

  const title = failed
    ? "영수증을 인식하지 못했어요"
    : ["영수증을 인식하는 중이에요", "영수증 정보를 추출하고 있어요", "영수증 내역 확인을 완료했어요"][stage];
  const sub = failed ? "빛 반사나 흐림 없이 다시 촬영해 주세요" : "잠시만 기다려 주세요";

  return (
    <div className="bz-screen">
      <StatusBar />
      <TopBar title="스캔" onBack={onBack} badge={ROUND_KO[round - 1]} />
      <div className="bz-proc">
        <Corners className="soft" />
        <div className="bz-proc-head" key={title}>
          <h2>{title}</h2>
          <p className={failed ? "err" : ""}>{sub}</p>
        </div>

        <div className="bz-steps">
          {STEPS.map((s, n) => {
            const Icon = s.icon;
            const active = !failed && stage === n;
            const done = !failed && stage > n;
            const bad = failed && n === 0;
            return (
              <React.Fragment key={s.k}>
                {n > 0 && <span className={`bz-step-dash ${!failed && stage >= n ? "on" : ""}`}>- - -</span>}
                <div className="bz-step">
                  <div className={`bz-step-box ${active ? "active" : ""} ${done ? "done" : ""} ${bad ? "bad" : ""}`}>
                    <Icon s={26} color={bad ? "#FF7A6E" : active || done ? "#B9B3FF" : "rgba(255,255,255,.35)"} />
                  </div>
                  <span className={active || done ? "on" : bad ? "bad" : ""}>{s.t}</span>
                </div>
              </React.Fragment>
            );
          })}
        </div>
      </div>

      {failed ? (
        <div className="bz-foot">
          <button className="bz-btn bz-btn-outline" onClick={onRetry}>다시 촬영하기</button>
          <button className="bz-textbtn" onClick={onManual}>직접 입력할게요</button>
        </div>
      ) : (
        <div className="bz-tipwrap">
          <div className="bz-tip">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#F2E9A6" strokeWidth="1.7" strokeLinecap="round">
              <path d="M12 3v2M5 6l1.4 1.4M19 6l-1.4 1.4M4 12h2M18 12h2" />
              <path d="M9 17a4.5 4.5 0 1 1 6 0v1.6H9V17ZM10 21h4" />
            </svg>
            <div><strong>TIP</strong><p>영수증의 글자가 잘 보이도록 밝은 곳에서 촬영하면 인식률이 올라가요.</p></div>
          </div>
        </div>
      )}
    </div>
  );
}

/* ============================ OCR 결과 편집 ============================ */

const SAMPLES = [
  { store: "정담 한식당", items: [
    ["삼겹살", 8, 160000], ["냉면", 5, 45000], ["공기밥", 6, 6000], ["계란찜", 2, 12000],
    ["된장찌개", 2, 12000], ["사이다", 3, 9000], ["소주", 5, 30000], ["맥주", 6, 36000]] },
  { store: "노원 포차", items: [
    ["오돌뼈", 2, 32000], ["모둠소세지", 1, 20000], ["콘치즈", 2, 16000], ["황도", 1, 10000],
    ["소주", 5, 30000], ["맥주", 6, 36000], ["사이다", 3, 9000]] },
  { store: "노원 노래방", items: [
    ["룸 이용료", 1, 25000], ["과일 화채", 1, 28500], ["모듬감자튀김", 1, 18000],
    ["소주", 1, 6000], ["맥주", 2, 12000]] },
];

let uid = 0;
const makeDraft = (round, manual = false) => {
  const s = SAMPLES[round - 1];
  return {
    round,
    store: manual || !s ? "" : s.store,
    items: manual || !s ? [] : s.items.map(([n, q, p]) => ({ id: ++uid, n, q, p })),
  };
};

function OcrResult({ draft, meeting, onMeeting, onChange, onComplete, onMore, onRetake, onBack }) {
  const total = draft.items.reduce((a, b) => a + Number(b.p || 0), 0);
  const empty = draft.items.length === 0;
  const set = (id, key, v) => onChange({ ...draft, items: draft.items.map((it) => (it.id === id ? { ...it, [key]: v } : it)) });
  const del = (id) => onChange({ ...draft, items: draft.items.filter((it) => it.id !== id) });
  const add = () => onChange({ ...draft, items: [...draft.items, { id: ++uid, n: "", q: 1, p: 0 }] });

  return (
    <div className="bz-screen">
      <StatusBar />
      <BrandBar title="OCR 결과" onBack={onBack} badge={ROUND_KO[draft.round - 1]} />
      <div className="bz-ocr-body">
        <label className="bz-field-label">모임 이름</label>
        <input className="bz-field" value={meeting} placeholder="예: 5월 회식" onChange={(e) => onMeeting(e.target.value)} />

        <label className="bz-field-label">가게 이름</label>
        <input className="bz-field" value={draft.store} placeholder="가게명" onChange={(e) => onChange({ ...draft, store: e.target.value })} />

        <label className="bz-field-label">영수증 내역</label>
        <div className="bz-ocr-card">
          {empty ? (
            <p className="bz-ocr-empty">인식된 품목이 없어요. 아래에서 직접 추가해 주세요.</p>
          ) : (
            <ul className="bz-items">
              {draft.items.map((it, n) => (
                <li key={it.id} style={{ animationDelay: `${n * 0.045}s` }}>
                  <input className="bz-cell bz-cell-name" value={it.n} placeholder="품목" onChange={(e) => set(it.id, "n", e.target.value)} />
                  <input className="bz-cell bz-cell-qty" inputMode="numeric" value={it.q}
                    onChange={(e) => set(it.id, "q", e.target.value.replace(/\D/g, ""))} />
                  <div className="bz-cell-price">
                    <input className="bz-cell" inputMode="numeric" value={Number(it.p || 0).toLocaleString("ko-KR")}
                      onChange={(e) => set(it.id, "p", Number(e.target.value.replace(/\D/g, "")) || 0)} />
                    <span>원</span>
                  </div>
                  <button className="bz-del" onClick={() => del(it.id)} aria-label={`${it.n} 삭제`}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.6)" strokeWidth="2" strokeLinecap="round">
                      <path d="M6 6l12 12M18 6L6 18" />
                    </svg>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <button className="bz-add-item" onClick={add}>+ 품목 추가</button>
          <div className="bz-total"><span>합계</span><b>{won(total)}</b></div>
        </div>
      </div>
      <div className="bz-foot">
        {empty ? (
          <button className="bz-btn bz-btn-outline" onClick={onRetake}>다시 찍기</button>
        ) : (
          <div className="bz-btn-row">
            <button className="bz-btn bz-btn-ghost" onClick={onComplete}>완료하기</button>
            <button className="bz-btn bz-btn-purple" onClick={onMore}>추가 스캔하기</button>
          </div>
        )}
      </div>
    </div>
  );
}

/* ============================ 영수증 목록 ============================ */

function ReceiptList({ receipts, saved, onSave, onPeople, onBack, onDelete }) {
  const total = receipts.reduce((a, r) => a + r.items.reduce((x, y) => x + Number(y.p || 0), 0), 0);
  return (
    <div className="bz-screen">
      <StatusBar />
      <BrandBar title="영수증 목록" onBack={onBack} />
      <div className="bz-list-body">
        <h3 className="bz-list-title">스캔된 영수증 내역</h3>
        <ul className="bz-receipts">
          {receipts.map((r, n) => (
            <li key={r.round} style={{ animationDelay: `${n * 0.07}s` }}>
              <span className="bz-round-chip">{ROUND_KO[r.round - 1]}</span>
              <strong>{r.store || "이름 없는 영수증"}</strong>
              <b>{won(r.items.reduce((a, b) => a + Number(b.p || 0), 0))}</b>
              <button className="bz-del bz-del-r" onClick={() => onDelete(r.round)} aria-label="삭제">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.5)" strokeWidth="2" strokeLinecap="round"><path d="M6 6l12 12M18 6L6 18" /></svg>
              </button>
            </li>
          ))}
        </ul>
        <div className="bz-sum-card">
          <span className="bz-sum-chip"><DocGlyph s={14} color="#fff" /> 영수증 총 {receipts.length}건</span>
          <p><b>{Number(total).toLocaleString("ko-KR")}</b> <i>원</i></p>
        </div>
      </div>
      <div className="bz-foot">
        <div className="bz-btn-row">
          <button className={`bz-btn bz-btn-ghost ${saved ? "bz-saved-btn" : ""}`} onClick={onSave}>
            {saved ? (
              <>
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#7DE87D" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                  <path d="m5 12.6 4.2 4.2L19 7" />
                </svg>
                저장 완료
              </>
            ) : "영수증 저장하기"}
          </button>
          <button className="bz-btn bz-btn-purple" onClick={onPeople}>정산 인원 설정하기</button>
        </div>
      </div>
    </div>
  );
}

function Saved({ onDone }) {
  useEffect(() => { const t = setTimeout(onDone, 2400); return () => clearTimeout(t); }, [onDone]);
  return (
    <div className="bz-screen">
      <StatusBar />
      <div className="bz-home-top" style={{ padding: "10px 20px 0" }}><Logo size={26} /></div>
      <div className="bz-saved">
        <ReceiptArt />
        <h2>영수증이 저장되었어요</h2>
        <p>정산내역에서 언제든 확인할 수 있어요</p>
      </div>
    </div>
  );
}

/* ============================ 정산 인원 ============================ */

function PeopleCount({ count, setCount, onNext, onBack }) {
  const [manual, setManual] = useState(false);
  const [txt, setTxt] = useState(String(count));
  const apply = () => {
    const v = Math.min(50, Math.max(2, Number(txt.replace(/\D/g, "")) || 2));
    setCount(v); setTxt(String(v)); setManual(false);
  };
  return (
    <div className="bz-screen">
      <StatusBar />
      <BrandBar title="정산 인원 설정" onBack={onBack} />
      <div className="bz-people">
        <MascotHero variant="planet" size={170} />
        <h2>총 몇 명과 나눌까요?</h2>
        <p>영수증을 함께 나눌 인원을 입력해 주세요</p>
        <div className="bz-counter">
          <button className="bz-cbtn minus" onClick={() => setCount(Math.max(2, count - 1))} disabled={count <= 2} aria-label="인원 줄이기">−</button>
          <span className="bz-cnum" key={count}>{count}</span>
          <button className="bz-cbtn plus" onClick={() => setCount(Math.min(50, count + 1))} aria-label="인원 늘리기">+</button>
        </div>
        {manual ? (
          <div className="bz-manual">
            <input autoFocus inputMode="numeric" value={txt} onChange={(e) => setTxt(e.target.value.replace(/\D/g, ""))}
              onKeyDown={(e) => e.key === "Enter" && apply()} />
            <button onClick={apply}>적용</button>
          </div>
        ) : (
          <button className="bz-textbtn" onClick={() => { setTxt(String(count)); setManual(true); }}>직접 숫자 입력하기</button>
        )}
      </div>
      <div className="bz-foot"><button className="bz-btn bz-btn-outline" onClick={onNext}>정산방 만들기</button></div>
    </div>
  );
}

/* ============================ QR 정산방 ============================ */

function RoomQr({ meeting, count, total, onEnter, onBack, toast }) {
  const date = new Date();
  const ds = `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, "0")}.${String(date.getDate()).padStart(2, "0")}`;
  return (
    <div className="bz-screen">
      <StatusBar />
      <BrandBar title="정산방 초대" onBack={onBack} />
      <div className="bz-qr-body">
        <div className="bz-qr-card">
          <div className="bz-qr-notch" />
          <p className="bz-qr-guide"><span>QR코드를 스캔하여</span><span>정산방에 들어가세요</span></p>
          <div className="bz-qr-frame">
            <Corners className="qr" />
            <QrCode value={`bilzy://room/${meeting}/${count}/${total}`} />
          </div>
          <h3 className="bz-qr-name">{meeting || "이름 없는 모임"}</h3>
          <span className="bz-qr-date">{ds}</span>
          <div className="bz-qr-meta">{count}명 · {won(total)} · 1인당 약 {won(Math.round(total / count))}</div>
          <div className="bz-qr-btns">
            <button className="bz-qr-btn ghost" onClick={() => toast("QR 이미지를 앨범에 저장했어요")}>
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 4v10m0 0 4-4m-4 4-4-4M5 18h14" />
              </svg> 이미지 저장하기
            </button>
            <button className="bz-qr-btn solid" onClick={() => toast("공유 시트를 열었어요")}>
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="18" cy="5" r="2.6" /><circle cx="6" cy="12" r="2.6" /><circle cx="18" cy="19" r="2.6" />
                <path d="m8.4 10.8 7.2-4.2M8.4 13.2l7.2 4.2" />
              </svg> QR코드 공유하기
            </button>
          </div>
        </div>
      </div>
      <div className="bz-foot"><button className="bz-btn bz-btn-outline" onClick={onEnter}>정산방 입장하기</button></div>
    </div>
  );
}

/* ==================================================================
   정산방 (참여자 / 결제자 공통)
================================================================== */

const PEOPLE = ["정민지", "김민준", "이서연", "박현우", "최지수", "강승호"];
const AV = ["#6C5CE7", "#5B77D6", "#4FA9C7", "#7A5BD6", "#5C6BC0", "#4E8BD1"];

const memberList = (n, myName) =>
  Array.from({ length: n }, (_, i) => ({
    id: i === 0 ? "me" : `p${i + 1}`,
    name: i === 0 ? myName : PEOPLE[i] || `참여자 ${i + 1}`,
    me: i === 0,
  }));

/* 다른 참여자들의 기본 설정 (참여 차수 / 안 먹은 항목) */
const PRESET_SKIP = { p3: [2, 3], p4: [3] };
const PRESET_EXCL = {
  p2: { 1: ["소주", "맥주", "사이다"], 2: ["황도", "사이다"] },
  p3: { 1: ["사이다"] },
  p4: { 1: ["계란찜", "된장찌개", "사이다"] },
  p5: { 1: ["계란찜"] },
};

/* 정산 계산: 제외되지 않은 사람들끼리 항목을 나눠 가짐
   members = [{ id, name, me, rounds:[차수], excl:{차수:[품목명]} }] */
function settle({ receipts, members }) {
  const exclOf = (m, round) => (m.excl || {})[round] || [];

  const rounds = receipts.map((r) => {
    const total = r.items.reduce((a, b) => a + Number(b.p || 0), 0);
    const present = members.filter((m) => (m.rounds || []).includes(r.round));
    const per = {};
    present.forEach((m) => (per[m.id] = { amount: 0, tags: [], base: present.length ? total / present.length : 0 }));
    r.items.forEach((it) => {
      const price = Number(it.p || 0);
      let eaters = present.filter((m) => !exclOf(m, r.round).includes(it.n));
      if (!eaters.length) eaters = present;
      eaters.forEach((m) => (per[m.id].amount += price / eaters.length));
    });
    present.forEach((m) => {
      exclOf(m, r.round).forEach((name) => {
        const it = r.items.find((x) => x.n === name);
        if (it) per[m.id].tags.push({ n: name, v: Math.round(Number(it.p || 0) / present.length) });
      });
    });
    return { ...r, total, present, per };
  });

  const totals = {};
  members.forEach((m) => {
    totals[m.id] = rounds.reduce((a, r) => a + (r.per[m.id] ? r.per[m.id].amount : 0), 0);
  });
  const grand = rounds.reduce((a, r) => a + r.total, 0);
  return { rounds, totals, grand };
}

/* --- 아바타 --- */
const Avatar = ({ name, i, on = true, size = 44 }) => (
  <div className={`bz-av ${on ? "on" : ""}`} style={{ width: size, height: size, background: on ? AV[i % AV.length] : "#1B1B3A" }}>
    <span>{on ? name.slice(0, 1) : ""}</span>
    <i className={on ? "on" : ""}>
      <svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round">
        <path d="m5 12.5 4.5 4.5L19 7" />
      </svg>
    </i>
  </div>
);

/* --- 정산방 입장 중 --- */
function EnteringRoom({ onDone }) {
  const [pct, setPct] = useState(8);
  useEffect(() => {
    const iv = setInterval(() => setPct((p) => Math.min(p + Math.random() * 15 + 6, 100)), 280);
    const t = setTimeout(onDone, 2500);
    return () => { clearInterval(iv); clearTimeout(t); };
  }, [onDone]);
  return (
    <div className="bz-screen bz-center-glow">
      <StatusBar />
      <div className="bz-home-top" style={{ padding: "10px 20px 0" }}><Logo size={26} /></div>
      <div className="bz-enter">
        <div className="bz-thinking"><i /><i /><i /></div>
        <MascotHero variant="planet" size={190} />
        <h2>정산방에 들어가는 중이에요</h2>
        <p>초대받은 정산방을 확인하고 있어요</p>
        <div className="bz-bar"><div className="bz-bar-fill" style={{ width: `${pct}%` }} /></div>
        <span className="bz-loading-label">잠시만 기다려 주세요</span>
      </div>
    </div>
  );
}

/* --- 참여자 이름 입력 --- */
function NameInput({ name, setName, onNext, onBack }) {
  const ok = name.trim().length > 0;
  return (
    <div className="bz-screen bz-center-glow">
      <StatusBar />
      <BrandBar title="참여자 입력" onBack={onBack} />
      <div className="bz-name">
        <MascotHero variant="spark" size={165} />
        <h2>이름을 입력해주세요</h2>
        <div className="bz-name-field">
          <input value={name} placeholder="이름 또는 닉네임" maxLength={12}
            onChange={(e) => setName(e.target.value)} onKeyDown={(e) => e.key === "Enter" && ok && onNext()} />
          {ok && (
            <button onClick={() => setName("")} aria-label="지우기">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round"><path d="M6 6l12 12M18 6L6 18" /></svg>
            </button>
          )}
        </div>
        <p>입력한 이름으로 다른 참여자에게 표시돼요</p>
      </div>
      <div className="bz-foot">
        <button className={`bz-btn bz-btn-outline ${ok ? "" : "bz-disabled"}`} disabled={!ok} onClick={onNext}>입력 완료</button>
      </div>
    </div>
  );
}

/* --- 나의 정산 목록 (참여 차수 선택) --- */
function RoundPick({ receipts, picked, setPicked, onNext, onBack }) {
  const toggle = (r) => setPicked(picked.includes(r) ? picked.filter((x) => x !== r) : [...picked, r].sort());
  return (
    <div className="bz-screen bz-center-glow">
      <StatusBar />
      <BrandBar title="나의 정산 목록" onBack={onBack} />
      <div className="bz-pick">
        <MascotHero variant="receipt" size={165} />
        <h2>참여한 차수만 정산할 수 있어요</h2>
        <p>차수를 선택하면 해당 금액만 정산이 진행돼요</p>
        <ul className="bz-pick-list">
          {receipts.map((r, n) => (
            <li key={r.round} style={{ animationDelay: `${n * 0.07}s` }}>
              <button className={picked.includes(r.round) ? "on" : ""} onClick={() => toggle(r.round)}>
                <span className="bz-round-chip">{ROUND_KO[r.round - 1]}</span>
                <strong>{r.store}</strong>
                <b>{won(r.items.reduce((a, x) => a + Number(x.p || 0), 0))}</b>
              </button>
            </li>
          ))}
        </ul>
      </div>
      <div className="bz-foot">
        <button className={`bz-btn bz-btn-outline ${picked.length ? "" : "bz-disabled"}`} disabled={!picked.length} onClick={onNext}>선택 완료</button>
      </div>
    </div>
  );
}

/* --- 멤버 합류 대기 --- */
function MemberWait({ members, onDone }) {
  const [n, setN] = useState(1);
  useEffect(() => {
    if (n >= members.length) { const t = setTimeout(onDone, 1100); return () => clearTimeout(t); }
    const t = setTimeout(() => setN(n + 1), 700);
    return () => clearTimeout(t);
  }, [n, members.length, onDone]);
  return (
    <div className="bz-screen bz-center-glow">
      <StatusBar />
      <div className="bz-home-top" style={{ padding: "10px 20px 0" }}><Logo size={26} /></div>
      <div className="bz-wait">
        <MascotHero variant="hello" size={190} />
        <h2>멤버 합류를 기다리고 있어요</h2>
        <p>모두가 모이면 자동으로 정산이 시작됩니다</p>
        <div className="bz-wait-row"><span>모인 멤버</span><b>{n} / {members.length}명</b></div>
        <div className="bz-bar"><div className="bz-bar-fill" style={{ width: `${(n / members.length) * 100}%` }} /></div>
        <div className="bz-av-row">
          {members.map((m, i) => (
            <div className="bz-av-cell" key={m.id}>
              <Avatar name={m.name} i={i} on={i < n} />
              <span className={i < n ? "on" : ""}>{i < n ? m.name : "···"}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* --- 금액 조정하기 --- */
function Adjust({ receipt, members, myExcl, setMyExcl, idx, last, onNext, onBack }) {
  const r = receipt;
  const total = r.items.reduce((a, b) => a + Number(b.p || 0), 0);
  const head = Math.max(1, members.filter((m) => (m.rounds || []).includes(r.round)).length);
  const base = Math.round(total / head);
  const sel = myExcl[r.round] || [];
  const cut = sel.reduce((a, n) => {
    const it = r.items.find((x) => x.n === n);
    return a + (it ? Number(it.p || 0) / head : 0);
  }, 0);
  const mine = Math.round(base - cut);

  const toggle = (n) => {
    const next = sel.includes(n) ? sel.filter((x) => x !== n) : [...sel, n];
    setMyExcl({ ...myExcl, [r.round]: next });
  };

  return (
    <div className="bz-screen">
      <StatusBar />
      <BrandBar title="금액 조정하기" onBack={onBack} badge={ROUND_KO[r.round - 1]} />
      <div className="bz-adj-body">
        <label className="bz-field-label">영수증 내역</label>
        <div className="bz-ocr-card bz-readonly">
          <ul className="bz-items">
            {r.items.map((it) => (
              <li key={it.id}>
                <span className="bz-ro-name">{it.n}</span>
                <span className="bz-ro-qty">{it.q}</span>
                <span className="bz-ro-price">{won(it.p)}</span>
              </li>
            ))}
          </ul>
          <div className="bz-total"><span>합계</span><b>{won(total)}</b></div>
        </div>

        <div className="bz-mine-card">
          <span className="bz-mine-chip">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round">
              <path d="M9 11a3.2 3.2 0 1 0 0-6.4A3.2 3.2 0 0 0 9 11Zm-6 8c0-2.9 2.8-4.6 6-4.6s6 1.7 6 4.6M17 6.2a3 3 0 0 1 0 5.6M18.5 19c0-2.2-1-3.5-2.5-4.2" />
            </svg>
            기본 1/N ({head}명)
          </span>
          <p key={mine}><b>{Number(mine).toLocaleString("ko-KR")}</b> <i>원</i></p>
          <span className={`bz-cut ${cut > 0 ? "on" : ""}`}>-{Number(Math.round(cut)).toLocaleString("ko-KR")}원</span>
        </div>

        <h3 className="bz-ai-title">AI가 영수증에서 찾은 항목</h3>
        <p className="bz-ai-sub">내가 안 먹은 메뉴를 선택하면 그만큼 차감돼요</p>
        <div className="bz-chips">
          {r.items.map((it) => (
            <button key={it.id} className={sel.includes(it.n) ? "on" : ""} onClick={() => toggle(it.n)}>{it.n}</button>
          ))}
        </div>

        <div className="bz-info">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.55)" strokeWidth="1.8" strokeLinecap="round">
            <circle cx="12" cy="12" r="9" /><path d="M12 11v5.5M12 7.8v.2" />
          </svg>
          <p><span>선택한 항목은 내 금액에서 1인분 가격이 빠져요</span><span>(주류 제외)</span></p>
        </div>
      </div>
      <div className="bz-foot">
        <button className="bz-btn bz-btn-outline" onClick={onNext}>
          {last ? "정산 시작하기" : `${ROUND_KO[idx + 1]}로 넘어가기`}
        </button>
      </div>
    </div>
  );
}

/* --- 계산 대기 --- */
const ClockArt = () => (
  <svg viewBox="0 0 260 240" width="230" height="215" className="bz-clock-art" aria-hidden="true">
    <defs>
      <linearGradient id="ck1" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stopColor="#E4DEFF" /><stop offset="100%" stopColor="#A294F3" /></linearGradient>
      <linearGradient id="ck2" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stopColor="#C7BCFA" /><stop offset="100%" stopColor="#8676E8" /></linearGradient>
    </defs>
    <g transform="rotate(12 150 150)"><rect x="96" y="96" width="120" height="120" rx="30" fill="url(#ck2)" /><circle cx="130" cy="188" r="8" fill="#fff" opacity=".35" /><circle cx="196" cy="122" r="7" fill="#fff" opacity=".3" /></g>
    <g transform="rotate(-8 130 118)">
      <rect x="70" y="58" width="120" height="120" rx="30" fill="url(#ck1)" />
      <circle cx="130" cy="118" r="34" fill="#fff" opacity=".55" />
      <circle cx="130" cy="118" r="34" fill="none" stroke="#7C6BE0" strokeWidth="3" />
      <path d="M130 98v22h16" stroke="#5C4CC4" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" fill="none" />
    </g>
    <g className="bz-coin-a"><circle cx="42" cy="60" r="17" fill="url(#ck1)" /><text x="42" y="67" fontSize="16" fontWeight="800" fill="#6E5DDA" textAnchor="middle">₩</text></g>
    <g className="bz-coin-b"><circle cx="228" cy="52" r="13" fill="url(#ck1)" /><text x="228" y="58" fontSize="13" fontWeight="800" fill="#6E5DDA" textAnchor="middle">₩</text></g>
    <g className="bz-coin-a"><circle cx="238" cy="180" r="14" fill="url(#ck1)" /><text x="238" y="186" fontSize="13" fontWeight="800" fill="#6E5DDA" textAnchor="middle">₩</text></g>
  </svg>
);

function CalcWait({ members, onDone }) {
  const [n, setN] = useState(Math.min(4, members.length));
  useEffect(() => {
    if (n >= members.length) { const t = setTimeout(onDone, 1200); return () => clearTimeout(t); }
    const t = setTimeout(() => setN(n + 1), 1000);
    return () => clearTimeout(t);
  }, [n, members.length, onDone]);
  const pending = members.slice(n).map((m) => `${m.name[0]}*${m.name.slice(2)}`);
  return (
    <div className="bz-screen bz-center-glow">
      <StatusBar />
      <div className="bz-home-top" style={{ padding: "10px 20px 0" }}><Logo size={26} /></div>
      <div className="bz-wait">
        <ClockArt />
        <h2>대기 중이에요</h2>
        <p>나머지 인원의 입력을 기다리고 있어요</p>
        <div className="bz-wait-row"><span>정산 완료 현황</span><b>{n} / {members.length}명</b></div>
        <div className="bz-bar"><div className="bz-bar-fill" style={{ width: `${(n / members.length) * 100}%` }} /></div>
        <div className="bz-av-row">
          {members.map((m, i) => (
            <div className="bz-av-cell" key={m.id}>
              <Avatar name={m.name} i={i} on={i < n} />
              <span className={i < n ? "on" : ""}>{m.name}</span>
            </div>
          ))}
        </div>
        {pending.length > 0 && (
          <div className="bz-pending">
            <span>{pending.join(", ")}님이 아직 특이사항을 입력하지 않았어요</span>
            <span>모두 완료되면 자동으로 정산이 시작돼요</span>
          </div>
        )}
      </div>
    </div>
  );
}

/* --- 정산 결과 (공용 조각) --- */
function SummaryCard({ title, dateLabel, data, payer, receive }) {
  return (
    <div className="bz-res-summary">
      <span className="bz-res-meta">{title} · {dateLabel}</span>
      <p className="bz-res-total"><b>{Number(Math.round(data.grand)).toLocaleString("ko-KR")}</b> <i>원</i></p>
      <div className="bz-res-rounds">
        {data.rounds.map((r) => (
          <div key={r.round}>
            <span className="bz-res-chip">{ROUND_KO[r.round - 1]} · {r.present.length}명</span>
            <b>{won(r.total)}</b>
          </div>
        ))}
      </div>
      <div className="bz-res-payer">{payer} 전액 결제 · 받을 금액 <b>{won(Math.round(receive))}</b></div>
    </div>
  );
}

const RoundTags = ({ cell }) => (
  <div className="bz-tags">
    {!cell ? <span className="bz-tag-g dim">미참여</span>
      : cell.tags.length === 0 ? <span className="bz-tag-g">1/N 정산</span>
        : cell.tags.map((t) => <span className="bz-tag-g" key={t.n}>{t.n} -{Number(t.v).toLocaleString("ko-KR")}원</span>)}
  </div>
);

function PersonCard({ member, data, i }) {
  return (
    <div className="bz-person" style={{ animationDelay: `${i * 0.06}s` }}>
      <div className="bz-person-head">
        <strong>{member.name}</strong>
        {member.me && <span className="bz-me-badge">나</span>}
        <b>{won(Math.round(data.totals[member.id]))}</b>
      </div>
      {data.rounds.map((r) => {
        const cell = r.per[member.id];
        return (
          <div className="bz-person-row" key={r.round}>
            <p>{ROUND_KO[r.round - 1]} - {cell ? won(Math.round(cell.amount)) : "0원"}</p>
            <RoundTags cell={cell} />
          </div>
        );
      })}
    </div>
  );
}

function Result({ meeting, data, members, onNext, onBack }) {
  const me = members[0];
  const receive = data.grand - data.totals[me.id];
  const d = new Date();
  const ds = `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;

  return (
    <div className="bz-screen">
      <StatusBar />
      <BrandBar title="정산 결과" onBack={onBack} />
      <div className="bz-result-body">
        <SummaryCard title={meeting} dateLabel={ds} data={data} payer={me.name} receive={receive} />
        <h3 className="bz-list-title" style={{ margin: "22px 0 12px" }}>참여자 정산 내역</h3>
        {members.map((m, i) => <PersonCard key={m.id} member={m} data={data} i={i} />)}
      </div>
      <div className="bz-foot"><button className="bz-btn bz-btn-outline" onClick={onNext}>정산 완료하기</button></div>
    </div>
  );
}

/* --- 정산 완료 --- */
const CheckArt = () => (
  <svg viewBox="0 0 240 190" width="210" height="166" className="bz-check-art" aria-hidden="true">
    <defs>
      <linearGradient id="ca1" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stopColor="#E4DEFF" /><stop offset="100%" stopColor="#A294F3" /></linearGradient>
      <linearGradient id="ca2" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stopColor="#C0B4F8" /><stop offset="100%" stopColor="#7F6FE4" /></linearGradient>
    </defs>
    <g transform="rotate(14 118 108)"><rect x="72" y="60" width="96" height="96" rx="26" fill="url(#ca2)" /></g>
    <g transform="rotate(-6 108 92)">
      <rect x="64" y="42" width="96" height="96" rx="26" fill="url(#ca1)" />
      <path d="M88 92l16 16 30-34" stroke="#fff" strokeWidth="11" strokeLinecap="round" strokeLinejoin="round" fill="none" />
    </g>
    <g className="bz-coin-a"><circle cx="36" cy="40" r="15" fill="url(#ca1)" /><text x="36" y="46" fontSize="14" fontWeight="800" fill="#6E5DDA" textAnchor="middle">₩</text></g>
    <g className="bz-coin-b"><circle cx="204" cy="120" r="13" fill="url(#ca1)" /><text x="204" y="126" fontSize="13" fontWeight="800" fill="#6E5DDA" textAnchor="middle">₩</text></g>
  </svg>
);

function Done({ meeting, data, members, account, onCopy, onEnd }) {
  const me = members[0];
  const mine = Math.round(data.totals[me.id]);
  const acctText = `${account.bank} ${account.no} ${account.name}`;
  return (
    <div className="bz-screen">
      <StatusBar />
      <div className="bz-home-top" style={{ padding: "10px 20px 0" }}><Logo size={26} /></div>
      <div className="bz-done-body">
        <div className="bz-done-card">
          <CheckArt />
          <h2>정산 완료!</h2>
          <p className="bz-done-total">{Number(Math.round(data.grand)).toLocaleString("ko-KR")}원</p>
          <span className="bz-done-meta">{meeting} · {members.length}명</span>
          <div className="bz-perf"><span className="l" /><span className="r" /></div>
          <span className="bz-mine-chip bz-done-chip">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round">
              <path d="M9 11a3.2 3.2 0 1 0 0-6.4A3.2 3.2 0 0 0 9 11Zm-6 8c0-2.9 2.8-4.6 6-4.6s6 1.7 6 4.6M17 6.2a3 3 0 0 1 0 5.6M18.5 19c0-2.2-1-3.5-2.5-4.2" />
            </svg>
            내가 낼 금액 · 총 {Number(mine).toLocaleString("ko-KR")}원
          </span>
          <div className="bz-done-rounds">
            {data.rounds.map((r) => {
              const cell = r.per[me.id];
              return (
                <div key={r.round}>
                  <p>{ROUND_KO[r.round - 1]} <b>{cell ? won(Math.round(cell.amount)) : "0원"}</b></p>
                  <RoundTags cell={cell} />
                </div>
              );
            })}
          </div>
          <button className="bz-account" onClick={() => onCopy(acctText)}>
            <strong>송금 계좌</strong>
            <span>{acctText}</span>
            <i>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#C9C3FF" strokeWidth="1.9" strokeLinejoin="round">
                <rect x="9" y="9" width="11" height="12" rx="2.5" /><path d="M15 6H6a2 2 0 0 0-2 2v9" />
              </svg>
            </i>
          </button>
          <span className="bz-copy-hint">계좌를 눌러 복사할 수 있어요</span>
        </div>
      </div>
      <div className="bz-foot"><button className="bz-btn bz-btn-outline" onClick={onEnd}>정산 종료하기</button></div>
    </div>
  );
}

/* ==================================================================
   직접 입력 / 정산내역 / 마이페이지
================================================================== */

function ManualInput({ draft, meeting, onMeeting, onChange, onComplete, onMore, onBack }) {
  const total = draft.items.reduce((a, b) => a + Number(b.p || 0), 0);
  const set = (id, key, v) => onChange({ ...draft, items: draft.items.map((it) => (it.id === id ? { ...it, [key]: v } : it)) });
  const del = (id) => onChange({ ...draft, items: draft.items.filter((it) => it.id !== id) });
  const add = () => onChange({ ...draft, items: [...draft.items, { id: ++uid, n: "", q: 1, p: 0 }] });
  const ok = draft.items.length > 0;

  return (
    <div className="bz-screen">
      <StatusBar />
      <BrandBar title="직접 입력" onBack={onBack} badge={ROUND_KO[draft.round - 1]} />
      <div className="bz-ocr-body">
        <h2 className="bz-manual-title">모임 이름과 상세내역을 입력해주세요</h2>
        <p className="bz-manual-sub"><span>나누어야 할 금액과 항목을 정확히 입력하면</span><span>정산이 더 쉬워져요</span></p>

        <label className="bz-field-label">모임 이름</label>
        <input className="bz-field" value={meeting} placeholder="예: 5월 회식" onChange={(e) => onMeeting(e.target.value)} />

        <label className="bz-field-label">가게 이름</label>
        <input className="bz-field" value={draft.store} placeholder="가게명" onChange={(e) => onChange({ ...draft, store: e.target.value })} />

        <label className="bz-field-label">영수증 내역</label>
        <div className="bz-ocr-card">
          {draft.items.length > 0 && (
            <ul className="bz-items">
              {draft.items.map((it, n) => (
                <li key={it.id} style={{ animationDelay: `${n * 0.04}s` }}>
                  <input className="bz-cell bz-cell-name" value={it.n} placeholder="품목" onChange={(e) => set(it.id, "n", e.target.value)} />
                  <input className="bz-cell bz-cell-qty" inputMode="numeric" value={it.q} onChange={(e) => set(it.id, "q", e.target.value.replace(/\D/g, ""))} />
                  <div className="bz-cell-price">
                    <input className="bz-cell" inputMode="numeric" value={Number(it.p || 0).toLocaleString("ko-KR")}
                      onChange={(e) => set(it.id, "p", Number(e.target.value.replace(/\D/g, "")) || 0)} />
                    <span>원</span>
                  </div>
                  <button className="bz-del" onClick={() => del(it.id)} aria-label="삭제">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.6)" strokeWidth="2" strokeLinecap="round"><path d="M6 6l12 12M18 6L6 18" /></svg>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <button className="bz-add-item" onClick={add}>+ 품목 추가</button>
          <div className="bz-total"><span>합계</span><b>{won(total)}</b></div>
        </div>

        <div className="bz-info bz-why">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#8B87F5" strokeWidth="1.8" strokeLinecap="round">
            <circle cx="12" cy="12" r="9" /><path d="M12 11v5.5M12 7.8v.2" />
          </svg>
          <div>
            <strong>정확한 입력이 중요한 이유</strong>
            <p><span>개별 메뉴와 수량을 입력하면 친구들이</span><span>각자 먹은 것만 정확하게 나눠서 결제할 수 있어요</span></p>
          </div>
        </div>
      </div>
      <div className="bz-foot">
        <div className="bz-btn-row">
          <button className={`bz-btn bz-btn-ghost ${ok ? "" : "bz-disabled"}`} disabled={!ok} onClick={onComplete}>완료하기</button>
          <button className={`bz-btn bz-btn-purple ${ok ? "" : "bz-disabled"}`} disabled={!ok} onClick={onMore}>추가 입력하기</button>
        </div>
      </div>
    </div>
  );
}

/* --- 정산내역 목록 --- */
const recordTotal = (rec) => rec.receipts.reduce((a, r) => a + r.items.reduce((x, y) => x + Number(y.p || 0), 0), 0);

function HistoryList({ records, onOpen, onTab, onToggleDemo }) {
  const now = records.filter((r) => !r.past);
  const past = records.filter((r) => r.past);
  const sum = now.reduce((a, r) => a + recordTotal(r), 0);

  const Card = ({ rec }) => (
    <li onClick={() => onOpen(rec)}>
      <strong>{rec.title}</strong>
      <span>{rec.date} · {rec.time}</span>
      <div>
        <em>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.55)" strokeWidth="1.9" strokeLinecap="round">
            <path d="M9 11a3.2 3.2 0 1 0 0-6.4A3.2 3.2 0 0 0 9 11Zm-6 8c0-2.9 2.8-4.6 6-4.6s6 1.7 6 4.6M17 6.2a3 3 0 0 1 0 5.6M18.5 19c0-2.2-1-3.5-2.5-4.2" />
          </svg>
          {rec.members.length}명
        </em>
        <b>{won(recordTotal(rec))}</b>
      </div>
    </li>
  );

  return (
    <div className="bz-screen bz-home">
      <StatusBar />
      <div className="bz-hist-scroll">
        <div className="bz-home-top"><Logo size={26} /></div>
        {now.length ? (
          <div className="bz-month-card">
            <div className="bz-month-head">
              <h2><span>이번달 내역을</span><span>알아볼까요?</span></h2>
              <MascotHero variant="duo" size={110} />
            </div>
            <div className="bz-month-stats">
              <div><span>총 금액</span><b>{won(sum)}</b></div>
              <div><span>횟수</span><b>{now.length}회</b></div>
            </div>
          </div>
        ) : (
          <div className="bz-month-card bz-month-empty">
            <MascotHero variant="hello" size={130} />
            <p>이번 달 정산 내역이 없어요</p>
          </div>
        )}

        {now.length > 0 && (
          <>
            <h3 className="bz-hist-title">이번 달 정산 내역</h3>
            <ul className="bz-hist-list">{now.map((r) => <Card rec={r} key={r.id} />)}</ul>
          </>
        )}
        {past.length > 0 && (
          <>
            <h3 className="bz-hist-title">과거 정산 내역</h3>
            <ul className="bz-hist-list">{past.map((r) => <Card rec={r} key={r.id} />)}</ul>
          </>
        )}
        <button className="bz-demo-link" onClick={onToggleDemo}>
          {now.length ? "데모: 이번 달 내역 비우기" : "데모: 이번 달 내역 되돌리기"}
        </button>
      </div>
      <BottomNav tab="hist" onTab={onTab} />
    </div>
  );
}

/* --- 정산내역 상세 --- */
function HistoryDetail({ rec, onBack, onTab, onAddPhoto, onRemovePhoto }) {
  const data = settle({ receipts: rec.receipts, members: rec.members });
  const me = rec.members[0];
  const receive = data.grand - data.totals[me.id];
  const [askPhoto, setAskPhoto] = useState(false);

  return (
    <div className="bz-screen">
      <StatusBar />
      <BrandBar title={rec.title} onBack={onBack} />
      <div className="bz-result-body">
        <SummaryCard title={rec.title} dateLabel={rec.date.replace(/\. /g, ".").replace(/\.$/, "")} data={data} payer={me.name} receive={receive} />
        <h3 className="bz-list-title" style={{ margin: "22px 0 12px" }}>참여자 정산 내역</h3>
        {rec.members.map((m, i) => <PersonCard key={m.id} member={m} data={data} i={i} />)}

        {rec.photos.length > 0 && (
          <div className="bz-att-head">
            <h3 className="bz-list-title" style={{ margin: 0 }}>첨부된 영수증</h3>
            <span>{rec.photos.length}장</span>
          </div>
        )}
        <ul className="bz-att-list">
          {rec.photos.map((p) => (
            <li key={p.id}>
              <div className="bz-att-thumb" />
              <div><strong>{p.store}</strong><span>{won(p.amount)} · {p.date}</span></div>
              <button onClick={() => onRemovePhoto(rec.id, p.id)} aria-label="영수증 삭제">···</button>
            </li>
          ))}
        </ul>
        <button className="bz-att-add" onClick={() => setAskPhoto(true)}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#9C97FF" strokeWidth="1.7" strokeLinejoin="round">
            <rect x="3" y="5" width="18" height="14" rx="3" /><path d="M12 9.5v5M9.5 12h5" />
          </svg>
          영수증 사진 추가하기
        </button>
      </div>
      <BottomNav tab="hist" onTab={onTab} />
      {askPhoto && (
        <div className="bz-ios-bg" onClick={() => setAskPhoto(false)}>
          <div className="bz-ios-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="bz-ios-icon">
              <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#8B87F5" strokeWidth="1.8" strokeLinejoin="round">
                <rect x="3" y="4" width="18" height="16" rx="3" /><path d="m5.5 17 4-4.6 2.6 3 2.4-2.8L19 17" /><circle cx="8.4" cy="9.2" r="1.4" />
              </svg>
            </div>
            <h4><span>‘Bilzy’가 사진에</span><span>접근하도록 허용하시겠습니까?</span></h4>
            <p><span>영수증 이미지를 선택하여</span><span>내역에 첨부할 수 있습니다.</span></p>
            <div className="bz-ios-actions">
              <button onClick={() => { setAskPhoto(false); onAddPhoto(rec.id); }}>사진 선택...</button>
              <button onClick={() => { setAskPhoto(false); onAddPhoto(rec.id); }}>모든 사진에 접근 허용</button>
              <button onClick={() => setAskPhoto(false)}>허용 안 함</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* --- 마이페이지 --- */
function MyPage({ name, provider, onAccount, onMail, onLogout, onTab }) {
  const rows = [
    { t: "계좌 정보 관리", fn: onAccount },
    { t: "이메일 문의", fn: onMail },
    { t: "로그아웃", fn: onLogout },
  ];
  return (
    <div className="bz-screen bz-center-glow">
      <StatusBar />
      <div className="bz-my-body">
        <div className="bz-home-top"><Logo size={26} /></div>
        <div className="bz-profile-card">
          <div className="bz-profile-av"><MascotMini size={64} /></div>
          <div><strong>{name}</strong><span>{P[provider].name} 계정 로그인</span></div>
        </div>
        <h3 className="bz-my-label">설정</h3>
        <ul className="bz-my-list">
          {rows.map((r) => <li key={r.t}><button onClick={r.fn}>{r.t}</button></li>)}
        </ul>
      </div>
      <BottomNav tab="my" onTab={onTab} />
    </div>
  );
}

const BANKS = ["신한은행", "국민은행", "하나은행", "우리은행", "농협은행", "카카오뱅크", "토스뱅크", "기업은행"];

function AccountManage({ account, onSave, onBack }) {
  const [form, setForm] = useState(account);
  const dirty = JSON.stringify(form) !== JSON.stringify(account);
  return (
    <div className="bz-screen">
      <StatusBar />
      <BrandBar title="계좌 정보 관리" onBack={onBack} />
      <div className="bz-acc-body">
        <div className="bz-info">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.55)" strokeWidth="1.8" strokeLinecap="round">
            <circle cx="12" cy="12" r="9" /><path d="M12 11v5.5M12 7.8v.2" />
          </svg>
          <p>
            <span>대표 계좌는 정산 요청 시 상대방에게 노출되는</span>
            <span>기본 계좌입니다. 정확한 정보를 입력하지 않을 경우</span>
            <span>송금 지연이 발생할 수 있습니다.</span>
          </p>
        </div>

        <div className="bz-acc-card">
          <div><span>대표 계좌</span><b>{account.bank}</b></div>
          <div><span>계좌 번호</span><b>{account.no}</b></div>
          <div><span>계좌 명의</span><b>{account.name}</b></div>
        </div>

        <label className="bz-field-label">은행 정보</label>
        <div className="bz-select">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#9C97FF" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
            <path d="M4 10h16M5 10 12 5l7 5M6 10v7M10 10v7M14 10v7M18 10v7M3.5 20h17" />
          </svg>
          <select value={form.bank} onChange={(e) => setForm({ ...form, bank: e.target.value })}>
            {BANKS.map((b) => <option key={b} value={b}>{b}</option>)}
          </select>
          <Chevron />
        </div>

        <label className="bz-field-label">계좌 번호</label>
        <div className="bz-acc-input">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#9C97FF" strokeWidth="1.9" strokeLinecap="round">
            <path d="M5 9h14M5 15h14M10 4 8 20M16 4l-2 16" />
          </svg>
          <input value={form.no} inputMode="numeric" placeholder="계좌번호를 입력해주세요"
            onChange={(e) => setForm({ ...form, no: e.target.value })} />
        </div>

        <label className="bz-field-label">계좌 명의</label>
        <div className="bz-acc-input">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#9C97FF" strokeWidth="1.9" strokeLinecap="round">
            <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8c0-3.3 3.1-5.4 7-5.4s7 2.1 7 5.4" />
          </svg>
          <input value={form.name} placeholder="예금주명을 입력해주세요" onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </div>
      </div>
      <div className="bz-foot">
        <button className={`bz-btn bz-btn-outline ${dirty ? "" : "bz-disabled"}`} disabled={!dirty} onClick={() => onSave(form)}>저장하기</button>
      </div>
    </div>
  );
}

/* --- 프로토타입용 화면 바로가기 --- */
const JUMP_GROUPS = [
  { g: "온보딩 · 가입", items: [
    ["onboarding", "온보딩"], ["social", "소셜 로그인"], ["consent", "계정 동의"],
    ["terms", "약관 동의"], ["login", "로그인 중"], ["home", "홈"]] },
  { g: "영수증 스캔", items: [
    ["camperm", "카메라 권한"], ["scan", "스캔"], ["processing", "인식 중"],
    ["processingFail", "인식 실패"], ["ocr", "OCR 결과"], ["manual", "직접 입력"],
    ["receipts", "영수증 목록"], ["saved", "저장 완료"]] },
  { g: "정산방", items: [
    ["people", "인원 설정"], ["qr", "QR 정산방"], ["qrscan", "QR 스캔"],
    ["nameinput", "참여자 입력"], ["roundpick", "차수 선택"], ["entering", "입장 중"], ["waitmembers", "멤버 대기"],
    ["adjust", "금액 조정"], ["calc", "계산 대기"], ["result", "정산 결과"], ["done", "정산 완료"]] },
  { g: "내역 · 계정", items: [
    ["history", "정산내역"], ["historyDetail", "내역 상세"], ["mypage", "마이페이지"], ["account", "계좌 관리"]] },
];

function JumpBar({ current, onJump }) {
  return (
    <div className="bz-jump">
      {JUMP_GROUPS.map((grp) => (
        <div className="bz-jump-group" key={grp.g}>
          <span className="bz-jump-label">{grp.g}</span>
          <div className="bz-jump-chips">
            {grp.items.map(([k, t]) => (
              <button key={k} className={current === k ? "on" : ""} onClick={() => onJump(k)}>{t}</button>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

/* ============================ 앱 ============================ */

/* 정산내역 샘플 레코드 */
let rid = 0;
const mk = (names, rounds, skips = {}, excls = {}) => ({
  members: names.map((n, i) => ({
    id: `m${i}`, name: n, me: i === 0,
    rounds: rounds.map((r) => r.round).filter((r) => !(skips[i] || []).includes(r)),
    excl: excls[i] || {},
  })),
  receipts: rounds,
});

const R = (round, store, items) => ({ round, store, items: items.map(([n, q, p]) => ({ id: ++uid, n, q, p })) });

const rec = (title, date, time, past, built, photos = []) => ({
  id: ++rid, title, date, time, past, ...built,
  photos: photos.map((p, i) => ({ id: `${rid}-${i}`, ...p })),
});

const SEED_RECORDS = [
  rec("회사 점심 약속", "5. 24. 2026", "12:30 PM", false,
    mk(["정민지", "이재연", "한정우", "김지우"],
      [R(1, "버거킹 강남점", [["햄버거", 4, 22000], ["감자튀김", 4, 10000], ["핫도그", 2, 7000], ["음료", 4, 6300]]),
       R(2, "스타벅스 강남점", [["아메리카노", 3, 13500], ["케이크", 1, 9000]])],
      { 2: [2] },
      { 0: { 1: ["핫도그"] }, 1: { 1: ["감자튀김"] }, 2: { 1: ["음료"] }, 3: { 1: ["핫도그"] } }),
    [{ store: "버거킹 강남점", amount: 45300, date: "2026.05.24" }]),

  rec("동기 모임", "5. 15. 2026", "07:45 PM", false,
    mk(["정민지", "김민준", "이서연", "박현우", "최지수", "강승호", "윤하늘"],
      [R(1, "홍대 곱창", [["곱창모둠", 4, 72000], ["볶음밥", 5, 15000], ["소주", 6, 18000], ["음료", 4, 7000]])],
      {}, { 1: { 1: ["소주"] } })),

  rec("팀 빌딩 워크숍", "5. 20. 2026", "06:45 PM", false,
    mk(["정민지", "김민준", "이서연", "박현우", "최지수", "강승호", "윤하늘", "오세진", "장서준", "배수아"],
      [R(1, "판교 화로구이", [["삼겹살", 12, 156000], ["된장찌개", 4, 24000], ["냉면", 5, 40000], ["음료", 5, 10000]])],
      {}, { 2: { 1: ["냉면"] } })),

  rec("프로젝트 발표회", "5. 25. 2026", "02:45 PM", false,
    mk(["정민지", "김민준", "이서연", "박현우", "최지수", "강승호", "윤하늘", "오세진", "장서준", "배수아", "한지호", "문가영", "신도윤", "권예린", "임태오"],
      [R(1, "여의도 케이터링", [["샌드위치 세트", 15, 187500], ["샐러드", 10, 70000], ["커피", 15, 55100]])])),

  rec("정기 회의", "5. 30. 2026", "10:45 AM", false,
    mk(["정민지", "김민준", "이서연", "박현우", "최지수", "강승호", "윤하늘", "오세진"],
      [R(1, "광화문 도시락", [["도시락", 8, 72000], ["음료", 8, 20000]])])),

  rec("생일 파티", "4. 28. 2026", "05:30 PM", true,
    mk(["정민지", "김민준", "이서연", "박현우", "최지수", "강승호"],
      [R(1, "이태원 다이닝", [["코스 요리", 6, 78000], ["케이크", 1, 12500], ["와인", 1, 13000]])],
      {}, { 2: { 1: ["와인"] } })),
];

export default function App() {
  const [step, setStep] = useState("onboarding");
  const [fx, setFx] = useState("fwd");
  const [provider, setProvider] = useState("kakao");

  const [meeting, setMeeting] = useState("5월 정기 회식");
  const [receipts, setReceipts] = useState([]);
  const [draft, setDraft] = useState(null);
  const [round, setRound] = useState(1);
  const [failedRounds, setFailedRounds] = useState([]);   // 이미 한 번 실패한 차수
  const [camOk, setCamOk] = useState(false);
  const [saved, setSaved] = useState(false);
  const [count, setCount] = useState(2);
  const [records, setRecords] = useState(SEED_RECORDS);
  const [openRec, setOpenRec] = useState(null);
  const [account, setAccount] = useState({ bank: "신한은행", no: "110-123-456789", name: "정민지" });

  /* 정산방 */
  const [myName, setMyName] = useState("정민지");
  const [myRounds, setMyRounds] = useState([]);
  const [myExcl, setMyExcl] = useState({});
  const [adjIdx, setAdjIdx] = useState(0);
  const [toastMsg, setToastMsg] = useState(null);

  const toast = useCallback((m) => { setToastMsg(m); setTimeout(() => setToastMsg(null), 1700); }, []);
  const go = (next, dir = "fwd") => { setFx(dir); setStep(next); };

  const total = receipts.reduce((a, r) => a + r.items.reduce((x, y) => x + Number(y.p || 0), 0), 0);

  /* 2차 스캔은 첫 시도에서 인식 실패하도록 설정 (실패 UX 확인용) */
  const willFail = round === 2 && !failedRounds.includes(2);

  const startScan = () => {
    setRound(receipts.length + 1);
    go(camOk ? "scan" : "camperm");
  };

  const saveDraft = () => {
    if (!draft) return [];
    const next = [...receipts.filter((r) => r.round !== draft.round), draft].sort((a, b) => a.round - b.round);
    setReceipts(next);
    return next;
  };

  const members = useMemo(() =>
    memberList(Math.max(2, count), myName || "나").map((m) => ({
      ...m,
      rounds: m.me ? myRounds : receipts.map((r) => r.round).filter((r) => !(PRESET_SKIP[m.id] || []).includes(r)),
      excl: m.me ? myExcl : (PRESET_EXCL[m.id] || {}),
    })), [count, myName, myRounds, myExcl, receipts]);
  const myReceipts = useMemo(() => receipts.filter((r) => myRounds.includes(r.round)), [receipts, myRounds]);
  const data = useMemo(() => settle({ receipts, members }), [receipts, members]);

  /* 참여자로 정산방 입장 (영수증이 없으면 샘플로 채움) */
  const joinRoom = () => {
    if (!receipts.length) {
      setReceipts([1, 2, 3].map((r) => makeDraft(r)));
      setCount(6);
    }
    setMyName("");
    go("qrscan");
  };

  const enterAsPayer = () => {
    setMyRounds(receipts.map((r) => r.round));
    setMyExcl({});
    setAdjIdx(0);
    go("roundpick");
  };

  const nav = (tab) => {
    if (tab === "home") go("home", "back");
    else if (tab === "scan") startScan();
    else if (tab === "hist") go("history");
    else go("mypage");
  };

  const copy = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
    } catch (e) {
      const ta = document.createElement("textarea");
      ta.value = text; ta.style.position = "fixed"; ta.style.opacity = "0";
      document.body.appendChild(ta); ta.select();
      try { document.execCommand("copy"); } catch (err) { /* ignore */ }
      ta.remove();
    }
    toast("송금 계좌를 복사했어요");
  };

  const finishRoom = () => {
    const m = new Date();
    const built = {
      members: members.map((x) => ({ ...x })),
      receipts,
    };
    setRecords([
      {
        id: ++rid,
        title: meeting || "이름 없는 모임",
        date: `${m.getMonth() + 1}. ${m.getDate()}. ${m.getFullYear()}`,
        time: m.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" }),
        past: false,
        photos: receipts.map((r, i) => ({
          id: `new-${i}`, store: r.store || "영수증",
          amount: r.items.reduce((a, b) => a + Number(b.p || 0), 0),
          date: `${m.getFullYear()}.${String(m.getMonth() + 1).padStart(2, "0")}.${String(m.getDate()).padStart(2, "0")}`,
        })),
        ...built,
      },
      ...records,
    ]);
    go("home", "back");
    toast("정산을 완료했어요");
  };

  const addPhoto = (recId) => {
    setRecords((rs) => rs.map((r) => r.id === recId
      ? { ...r, photos: [...r.photos, { id: `${recId}-${Date.now()}`, store: "추가한 영수증", amount: 0, date: "방금 전" }] }
      : r));
    toast("영수증 사진을 첨부했어요");
  };
  const removePhoto = (recId, photoId) => {
    setRecords((rs) => rs.map((r) => r.id === recId ? { ...r, photos: r.photos.filter((p) => p.id !== photoId) } : r));
    toast("영수증을 삭제했어요");
  };

  const homeHistory = records.slice(0, 4).map((r) => ({
    t: r.title,
    d: `${r.date.split(". ")[0]}.${r.date.split(". ")[1]} · ${r.members.length}명 참여`,
    a: won(recordTotal(r)),
  }));

  /* 화면 바로가기: 해당 화면이 필요로 하는 데이터를 먼저 채운다 */
  const jump = (key) => {
    const seedReceipts = () => {
      if (receipts.length) return receipts;
      const rs = [1, 2, 3].map((r) => makeDraft(r));
      setReceipts(rs);
      setCount(6);
      return rs;
    };
    const seedRoom = () => {
      const rs = seedReceipts();
      setCount((c) => (c < 6 ? 6 : c));
      setMyRounds(rs.map((r) => r.round));
      setMyExcl({});
      setAdjIdx(0);
      if (!myName) setMyName("정민지");
    };

    switch (key) {
      case "camperm": setCamOk(false); break;
      case "scan": setCamOk(true); setRound(Math.max(1, receipts.length + 1)); break;
      case "processing": setCamOk(true); setRound(1); setFailedRounds([1]); break;
      case "processingFail": setCamOk(true); setRound(2); setFailedRounds([]); go("processing"); return;
      case "ocr": setDraft(makeDraft(Math.min(3, receipts.length + 1))); break;
      case "manual": setDraft(makeDraft(Math.min(3, receipts.length + 1), true)); break;
      case "receipts": seedReceipts(); setSaved(false); break;
      case "saved": seedReceipts(); break;
      case "people": case "qr": seedReceipts(); break;
      case "nameinput": setMyName(""); break;
      case "roundpick": seedRoom(); setMyRounds([]); break;
      case "entering": case "waitmembers": case "calc": case "result": case "done": seedRoom(); break;
      case "adjust": seedRoom(); setAdjIdx(0); break;
      case "historyDetail": setOpenRec(records[0].id); break;
      default: break;
    }
    go(key);
  };

  return (
    <div className="bz-root">
      <Styles />
      <div className="bz-device">
        <div className="bz-viewport">
          <div key={step} className={`bz-page ${fx}`}>
            {step === "onboarding" && <Onboarding onSignup={() => go("social")} onLogin={() => go("social")} />}
            {step === "social" && <Social onPick={(p) => { setProvider(p); go("consent"); }} onBack={() => go("onboarding", "back")} />}
            {step === "consent" && <Consent provider={provider} onNext={() => go("terms")} onBack={() => go("social", "back")} />}
            {step === "terms" && <Terms provider={provider} onNext={() => go("login")} onBack={() => go("consent", "back")} />}
            {step === "login" && <Loading onDone={() => go("home")} />}

            {step === "home" && (
              <Home history={homeHistory} onStart={startScan} onJoin={joinRoom} onTab={nav} toast={toast} />
            )}

            {step === "camperm" && <CamPermission onAllow={() => { setCamOk(true); go("scan"); }} onTab={nav} />}

            {step === "scan" && (
              <Scanner
                round={round}
                onShoot={() => go("processing")}
                onBack={() => (receipts.length ? go("receipts", "back") : go("home", "back"))}
                toast={toast}
              />
            )}

            {step === "processing" && (
              <Processing
                round={round}
                willFail={willFail}
                onFail={() => setFailedRounds((f) => (f.includes(round) ? f : [...f, round]))}
                onDone={() => { setDraft(makeDraft(round)); go("ocr"); }}
                onRetry={() => go("scan", "back")}
                onManual={() => { setDraft(makeDraft(round, true)); go("manual"); }}
                onBack={() => go("scan", "back")}
              />
            )}

            {step === "ocr" && draft && (
              <OcrResult
                draft={draft}
                meeting={meeting}
                onMeeting={setMeeting}
                onChange={setDraft}
                onComplete={() => { saveDraft(); go("receipts"); }}
                onMore={() => { const n = saveDraft(); setRound(n.length + 1); setDraft(null); go("scan"); }}
                onRetake={() => go("scan", "back")}
                onBack={() => (receipts.length ? go("receipts", "back") : go("scan", "back"))}
              />
            )}

            {step === "receipts" && (
              <ReceiptList
                receipts={receipts}
                saved={saved}
                onDelete={(r) => { setSaved(false); setReceipts(receipts.filter((x) => x.round !== r)); }}
                onSave={() => go("saved")}
                onPeople={() => go("people")}
                onBack={() => go("home", "back")}
              />
            )}

            {step === "saved" && (
              <Saved onDone={() => {
                setSaved(true);
                go("receipts", "back");
                toast("영수증을 저장했어요");
              }} />
            )}

            {step === "people" && (
              <PeopleCount count={count} setCount={setCount} onNext={() => go("qr")} onBack={() => go("receipts", "back")} />
            )}

            {step === "qr" && (
              <RoomQr meeting={meeting} count={count} total={total} toast={toast}
                onEnter={enterAsPayer} onBack={() => go("people", "back")} />
            )}

            {step === "qrscan" && (
              <Scanner initialMode="qr" onQrShoot={() => go("nameinput")} onShoot={() => go("nameinput")}
                onBack={() => go("home", "back")} toast={toast} />
            )}
            {step === "entering" && <EnteringRoom onDone={() => go("waitmembers")} />}
            {step === "nameinput" && (
              <NameInput name={myName} setName={setMyName}
                onNext={() => { setMyRounds([]); setMyExcl({}); setAdjIdx(0); go("roundpick"); }}
                onBack={() => go("qrscan", "back")} />
            )}
            {step === "roundpick" && (
              <RoundPick receipts={receipts} picked={myRounds} setPicked={setMyRounds}
                onNext={() => go("entering")} onBack={() => go("home", "back")} />
            )}
            {step === "waitmembers" && (
              <MemberWait members={members} onDone={() => { setAdjIdx(0); go("adjust"); }} />
            )}
            {step === "adjust" && myReceipts[adjIdx] && (
              <Adjust
                receipt={myReceipts[adjIdx]}
                members={members}
                myExcl={myExcl}
                setMyExcl={setMyExcl}
                idx={myReceipts[adjIdx].round - 1}
                last={adjIdx === myReceipts.length - 1}
                onNext={() => (adjIdx === myReceipts.length - 1 ? go("calc") : setAdjIdx(adjIdx + 1))}
                onBack={() => (adjIdx === 0 ? go("roundpick", "back") : setAdjIdx(adjIdx - 1))}
              />
            )}
            {step === "calc" && <CalcWait members={members} onDone={() => go("result")} />}
            {step === "result" && (
              <Result meeting={meeting} data={data} members={members}
                onNext={() => go("done")} onBack={() => { setAdjIdx(0); go("adjust", "back"); }} />
            )}
            {step === "done" && (
              <Done meeting={meeting} data={data} members={members} account={account} onCopy={copy} onEnd={finishRoom} />
            )}

            {step === "manual" && draft && (
              <ManualInput
                draft={draft}
                meeting={meeting}
                onMeeting={setMeeting}
                onChange={setDraft}
                onComplete={() => { saveDraft(); go("receipts"); }}
                onMore={() => { const n = saveDraft(); const nr = n.length + 1; setRound(nr); setDraft(makeDraft(nr, true)); }}
                onBack={() => go("scan", "back")}
              />
            )}

            {step === "history" && (
              <HistoryList
                records={records}
                onOpen={(r) => { setOpenRec(r.id); go("historyDetail"); }}
                onTab={nav}
                onToggleDemo={() => setRecords((rs) => rs.some((r) => !r.past)
                  ? rs.map((r) => ({ ...r, past: true }))
                  : SEED_RECORDS)}
              />
            )}
            {step === "historyDetail" && records.find((r) => r.id === openRec) && (
              <HistoryDetail
                rec={records.find((r) => r.id === openRec)}
                onBack={() => go("history", "back")}
                onTab={nav}
                onAddPhoto={addPhoto}
                onRemovePhoto={removePhoto}
              />
            )}

            {step === "mypage" && (
              <MyPage
                name={myName || "정민지"} provider={provider}
                onAccount={() => go("account")}
                onMail={() => toast("메일 앱으로 문의를 시작해요")}
                onLogout={() => { toast("로그아웃했어요"); go("onboarding", "back"); }}
                onTab={nav}
              />
            )}
            {step === "account" && (
              <AccountManage account={account}
                onSave={(f) => { setAccount(f); toast("계좌 정보를 저장했어요"); }}
                onBack={() => go("mypage", "back")} />
            )}
          </div>
          {toastMsg && <div className="bz-toast">{toastMsg}</div>}
        </div>
      </div>
      <p className="bz-hint">홈에서 ‘정산 시작하기’를 누르면 스캔 플로우가 시작됩니다 · 2차 스캔은 한 번 인식 실패합니다</p>
      <JumpBar current={step} onJump={jump} />
    </div>
  );
}

/* ============================ 스타일 ============================ */

function Styles() {
  return (
    <style>{`
@import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.css');

.bz-root{min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px;
  background:radial-gradient(120% 90% at 50% 0%, #17173a 0%, #0b0b1d 55%, #06060f 100%);padding:28px 12px;
  font-family:'Pretendard',-apple-system,BlinkMacSystemFont,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;
  -webkit-font-smoothing:antialiased;}
.bz-device{width:390px;height:844px;border-radius:44px;overflow:hidden;position:relative;background:${C.bg};
  box-shadow:0 40px 90px rgba(0,0,0,.6),0 0 0 10px #16162c,0 0 0 11px #2b2b4d;flex-shrink:0;}
.bz-viewport{position:absolute;inset:0;overflow:hidden;}
.bz-hint{color:rgba(255,255,255,.35);font-size:12.5px;margin:0;text-align:center;}

.bz-page{position:absolute;inset:0;}
.bz-page.fwd{animation:pgIn .36s cubic-bezier(.22,1,.36,1);}
.bz-page.back{animation:pgBack .32s cubic-bezier(.22,1,.36,1);}
@keyframes pgIn{from{opacity:0;transform:translateX(24px)}to{opacity:1;transform:none}}
@keyframes pgBack{from{opacity:0;transform:translateX(-20px)}to{opacity:1;transform:none}}

.bz-screen{position:absolute;inset:0;display:flex;flex-direction:column;color:#fff;overflow:hidden;background:${C.bg};}
.bz-onb,.bz-center-glow{background:radial-gradient(90% 55% at 50% 52%, rgba(74,58,168,.65) 0%, rgba(30,26,88,.35) 45%, rgba(8,11,36,0) 75%),${C.bg};}
input{font-family:inherit;}

.bz-status{display:flex;align-items:center;justify-content:space-between;padding:14px 22px 4px;flex-shrink:0;}
.bz-time{font-size:16px;font-weight:600;}
.bz-status-icons{display:flex;align-items:center;gap:6px;}

.bz-topbar{display:flex;align-items:center;gap:12px;padding:14px 20px 16px;flex-shrink:0;
  border-bottom:1px solid rgba(255,255,255,.08);font-size:17px;font-weight:600;}
.bz-topbar-title{flex:1;}
.bz-back{background:none;border:none;padding:0;cursor:pointer;display:flex;}
.bz-round-badge{display:inline-flex;align-items:center;gap:6px;font-size:13px;font-weight:700;color:#7DE87D;
  border:1.4px solid #4FCB6B;border-radius:99px;padding:5px 13px;background:rgba(60,190,110,.1);}
.bz-round-badge i{width:6px;height:6px;border-radius:50%;background:#7DE87D;box-shadow:0 0 8px #7DE87D;}

.bz-brandbar{flex-shrink:0;border-bottom:1px solid rgba(255,255,255,.08);}
.bz-brandbar-logo{padding:8px 20px 2px;}
.bz-brandbar-row{display:flex;align-items:center;gap:12px;padding:8px 20px 14px;font-size:17px;font-weight:600;}
.bz-brandbar-row>span:first-of-type{flex:1;}

.bz-logo{font-weight:900;letter-spacing:-1.5px;line-height:1;
  background:linear-gradient(180deg,#A9A5FF 0%,#7A73EF 100%);-webkit-background-clip:text;
  -webkit-text-fill-color:transparent;background-clip:text;filter:drop-shadow(0 0 18px rgba(120,110,255,.45));}

/* ---------- 온보딩 ---------- */
.bz-dots{display:flex;justify-content:center;gap:8px;padding:24px 0 6px;flex-shrink:0;}
.bz-dot{width:8px;height:8px;border-radius:99px;border:none;padding:0;cursor:pointer;background:rgba(255,255,255,.55);
  transition:width .45s cubic-bezier(.22,1,.36,1),background .3s;}
.bz-dot.on{width:30px;background:#6C63FF;}
.bz-swipe{flex:1;overflow:hidden;cursor:grab;user-select:none;}
.bz-swipe:active{cursor:grabbing;}
.bz-track{display:flex;height:100%;}
.bz-slide{display:flex;flex-direction:column;height:100%;padding-top:44px;}
.bz-copy{text-align:center;padding:0 24px;}
.bz-copy h1{font-size:23px;font-weight:800;line-height:1.42;margin:0 0 14px;letter-spacing:-.6px;}
.bz-copy h1 span,.bz-copy p span{display:block;}
.bz-copy p{margin:0;font-size:14px;line-height:1.55;color:#B9B3F0;font-weight:500;}
.bz-in-1{animation:up .6s cubic-bezier(.22,1,.36,1) both;}
.bz-in-2{animation:up .6s .1s cubic-bezier(.22,1,.36,1) both;}
.bz-in-3{animation:up .7s .16s cubic-bezier(.22,1,.36,1) both;}
@keyframes up{from{opacity:0;transform:translateY(24px)}to{opacity:1;transform:none}}
.bz-art{flex:1;display:flex;align-items:center;justify-content:center;}

.bz-mascot-wrap{position:relative;display:flex;align-items:center;justify-content:center;}
.bz-glow{position:absolute;border-radius:50%;
  background:radial-gradient(circle,rgba(120,255,150,.22) 0%,rgba(90,80,220,.16) 42%,transparent 68%);
  animation:pulse 3.6s ease-in-out infinite;}
@keyframes pulse{0%,100%{transform:scale(1);opacity:.85}50%{transform:scale(1.12);opacity:1}}
.bz-mascot{position:relative;animation:float 3.4s ease-in-out infinite;filter:drop-shadow(0 14px 30px rgba(60,220,120,.28));}
@keyframes float{0%,100%{transform:translateY(0) rotate(-1deg)}50%{transform:translateY(-14px) rotate(1.5deg)}}
.bz-mini{animation:float 3.8s ease-in-out infinite;filter:drop-shadow(0 8px 18px rgba(60,220,120,.3));}
.bz-eyes{animation:blink 4.6s infinite;transform-origin:100px 94px;}
@keyframes blink{0%,92%,100%{transform:scaleY(1)}95%{transform:scaleY(.1)}}
.bz-pop{animation:popUp 2.2s ease-in-out infinite;}
@keyframes popUp{0%,100%{opacity:.45;transform:translateY(4px) scale(.9)}50%{opacity:1;transform:translateY(-4px) scale(1.06)}}
.bz-sparks{animation:twinkle 2.4s ease-in-out infinite;}
@keyframes twinkle{0%,100%{opacity:.4;transform:scale(.88)}50%{opacity:1;transform:scale(1.08)}}
.bz-camera{animation:shutter 3s ease-in-out infinite;transform-origin:101px 143px;}
@keyframes shutter{0%,88%,100%{transform:scale(1)}92%{transform:scale(1.07) rotate(-2deg)}}

.bz-onb-foot{padding:0 24px 34px;flex-shrink:0;}
.bz-have{text-align:center;font-size:14px;color:rgba(255,255,255,.8);margin:0 0 16px;}
.bz-link{background:none;border:none;color:#fff;font-size:14px;font-weight:600;text-decoration:underline;
  text-underline-offset:3px;cursor:pointer;padding:0;font-family:inherit;}

/* 버튼 */
.bz-btn{width:100%;height:56px;border-radius:16px;border:none;font-size:16px;font-weight:700;
  display:flex;align-items:center;justify-content:center;gap:8px;cursor:pointer;font-family:inherit;
  transition:transform .16s,filter .2s;letter-spacing:-.3px;}
.bz-btn:active{transform:scale(.97);}
.bz-btn-outline{background:rgba(108,99,255,.14);border:1.5px solid rgba(150,140,255,.5);color:#fff;}
.bz-btn-outline:hover{background:rgba(108,99,255,.26);}
.bz-btn-ghost{background:rgba(40,52,110,.75);border:1px solid rgba(255,255,255,.12);color:#fff;}
.bz-btn-purple{background:linear-gradient(160deg,#6B5BD6 0%,#4C3FB0 100%);color:#fff;}
.bz-kakao{background:${C.kakao};color:#3B1E1E;}
.bz-naver{background:${C.naver};color:#fff;}
.bz-disabled{opacity:.35;cursor:not-allowed;}
.bz-disabled:active{transform:none;}
.bz-btn-row{display:grid;grid-template-columns:1fr 1fr;gap:12px;}
.bz-btn-row .bz-btn{font-size:15px;}
.bz-textbtn{display:block;margin:14px auto 0;background:none;border:none;color:rgba(255,255,255,.72);
  font-size:14px;cursor:pointer;font-family:inherit;text-underline-offset:3px;}
.bz-textbtn:hover{text-decoration:underline;}

/* ---------- 소셜/약관 ---------- */
.bz-social-body{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:0 24px 120px;}
.bz-social-logo{position:relative;animation:up .6s cubic-bezier(.22,1,.36,1) both;}
.bz-social-mascot{position:absolute;right:-22px;top:-52px;}
.bz-social-sub{margin:16px 0 32px;text-align:center;font-size:14px;color:#C6C1F5;line-height:1.6;animation:up .6s .1s cubic-bezier(.22,1,.36,1) both;}
.bz-social-sub span{display:block;}
.bz-social-btns{width:100%;display:flex;flex-direction:column;gap:12px;animation:up .6s .18s cubic-bezier(.22,1,.36,1) both;}
.bz-ghost-back{position:absolute;bottom:26px;left:0;right:0;margin:auto;width:fit-content;background:none;border:none;
  color:rgba(255,255,255,.32);font-size:13px;cursor:pointer;font-family:inherit;}

.bz-consent-body{flex:1;padding:34px 24px 0;overflow:auto;}
.bz-consent-head{text-align:center;margin-bottom:22px;animation:up .5s cubic-bezier(.22,1,.36,1) both;}
.bz-consent-head p{margin:10px 0 0;font-size:14px;color:#CFCAF7;}
.bz-card-consent{border-radius:22px;padding:24px 22px;animation:up .55s .08s cubic-bezier(.22,1,.36,1) both;
  background:linear-gradient(160deg,rgba(90,76,190,.42),rgba(46,38,120,.3) 55%,rgba(30,26,80,.22));
  border:1px solid rgba(160,150,255,.2);box-shadow:0 20px 50px rgba(0,0,0,.35),inset 0 1px 0 rgba(255,255,255,.08);}
.bz-profile{display:flex;align-items:center;gap:16px;}
.bz-avatar{width:56px;height:56px;border-radius:50%;background:#1E1B47;border:2px solid #8B7BEF;flex-shrink:0;}
.bz-profile strong{display:block;font-size:19px;font-weight:700;margin-bottom:4px;}
.bz-profile span{font-size:13px;color:rgba(255,255,255,.55);}
.bz-divider{height:1px;background:rgba(255,255,255,.16);margin:20px 0;}
.bz-req-title{font-size:15px;font-weight:700;margin:0 0 14px;}
.bz-req{list-style:none;margin:0 0 22px;padding:0;display:flex;flex-direction:column;gap:14px;}
.bz-req li{display:flex;align-items:center;gap:12px;font-size:15px;animation:up .45s cubic-bezier(.22,1,.36,1) both;}
.bz-req li span{flex:1;color:rgba(255,255,255,.92);}
.bz-note{background:rgba(255,255,255,.07);border-radius:12px;padding:14px;text-align:center;font-size:12.5px;
  line-height:1.6;color:rgba(255,255,255,.62);}
.bz-note span{display:block;}
.bz-foot{padding:20px 24px 34px;flex-shrink:0;}

.bz-terms-body{flex:1;padding:30px 24px 0;overflow:auto;}
.bz-terms-head{text-align:center;margin-bottom:26px;position:relative;}
.bz-terms-head p{margin:10px 0 0;font-size:14px;color:#CFCAF7;line-height:1.55;}
.bz-terms-head p span{display:block;}
.bz-terms-head::after{content:"";position:absolute;left:50%;top:60%;width:340px;height:280px;transform:translate(-50%,0);
  background:radial-gradient(circle,rgba(96,80,220,.5) 0%,transparent 66%);z-index:-1;opacity:.55;transition:opacity .5s;}
.bz-terms-head.lit::after{opacity:1;}
.bz-all{width:100%;display:flex;align-items:center;gap:14px;padding:18px;border-radius:16px;font-size:15px;font-weight:600;
  cursor:pointer;font-family:inherit;color:#fff;text-align:left;background:rgba(255,255,255,.09);
  border:1px solid rgba(255,255,255,.14);transition:background .3s,border-color .3s,box-shadow .3s;margin-bottom:20px;}
.bz-all.on{background:rgba(255,255,255,.16);border-color:rgba(180,170,255,.4);box-shadow:0 0 30px rgba(120,105,255,.28);}
.bz-all-check{width:26px;height:26px;border-radius:50%;display:flex;align-items:center;justify-content:center;flex-shrink:0;
  background:rgba(255,255,255,.16);transition:background .25s,transform .25s;}
.bz-all-check.on{background:#fff;transform:scale(1.06);}
.bz-term-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:4px;}
.bz-term-list li{display:flex;align-items:center;border-radius:12px;transition:background .2s;}
.bz-term-list li:hover{background:rgba(255,255,255,.04);}
.bz-term-toggle{flex:1;display:flex;align-items:center;gap:12px;background:none;border:none;cursor:pointer;
  padding:16px 6px 16px 12px;font-family:inherit;font-size:14.5px;color:rgba(255,255,255,.55);text-align:left;}
.bz-term-toggle span.on{color:#fff;font-weight:500;}
.bz-term-more{background:none;border:none;cursor:pointer;padding:16px 12px;display:flex;}
.bz-sheet-bg{position:absolute;inset:0;background:rgba(4,4,16,.62);display:flex;align-items:flex-end;z-index:20;animation:fade .25s both;}
@keyframes fade{from{opacity:0}to{opacity:1}}
.bz-sheet{width:100%;background:#141334;border-radius:26px 26px 0 0;padding:12px 24px 32px;
  animation:sheetUp .38s cubic-bezier(.22,1,.36,1) both;max-height:74%;display:flex;flex-direction:column;}
@keyframes sheetUp{from{transform:translateY(100%)}to{transform:none}}
.bz-sheet-grab{width:42px;height:4px;border-radius:99px;background:rgba(255,255,255,.25);margin:0 auto 18px;}
.bz-sheet h3{margin:0 0 14px;font-size:18px;font-weight:700;}
.bz-sheet-text{overflow:auto;flex:1;margin-bottom:18px;}
.bz-sheet-text p{font-size:13.5px;line-height:1.75;color:rgba(255,255,255,.62);margin:0 0 12px;}

/* ---------- 로딩 ---------- */
.bz-loading{flex:1;display:flex;flex-direction:column;align-items:center;padding:96px 24px 0;}
.bz-loading-copy{margin:14px 0 0;text-align:center;font-size:19px;font-weight:700;line-height:1.5;}
.bz-loading-copy span{display:block;}
.bz-bar{width:100%;height:8px;border-radius:99px;background:rgba(255,255,255,.14);overflow:hidden;margin-top:10px;}
.bz-bar-fill{height:100%;border-radius:99px;transition:width .4s ease;
  background:linear-gradient(90deg,#8B7BEF,#5EC8F0);box-shadow:0 0 14px rgba(110,150,255,.6);}
.bz-loading-label{margin-top:14px;font-size:14px;color:#B9B3F0;animation:breathe 1.6s ease-in-out infinite;}
@keyframes breathe{0%,100%{opacity:.55}50%{opacity:1}}

/* ---------- 홈 ---------- */
.bz-home{background:radial-gradient(80% 40% at 65% 12%, rgba(60,50,150,.45) 0%, transparent 60%),${C.bg};}
.bz-home-scroll{flex:1;overflow:auto;padding:6px 20px 24px;}
.bz-home-scroll::-webkit-scrollbar{width:0;}
.bz-home-top{padding:10px 0 4px;}
.bz-hero{display:flex;align-items:center;justify-content:space-between;gap:10px;margin:8px 0 20px;}
.bz-hero h2{margin:0;font-size:21px;font-weight:800;line-height:1.45;letter-spacing:-.6px;}
.bz-hero h2 span{display:block;}
.bz-actions{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:20px;}
.bz-action{border:none;border-radius:20px;padding:20px 16px;cursor:pointer;font-family:inherit;color:#fff;
  display:flex;flex-direction:column;align-items:center;gap:10px;text-align:center;transition:transform .18s;
  box-shadow:0 14px 34px rgba(0,0,0,.34);}
.bz-action:active{transform:scale(.97);}
.bz-action-payer{background:linear-gradient(160deg,#6B4FCB,#4B3AA8);}
.bz-action-joiner{background:linear-gradient(160deg,#4E63C9,#3B4B9E);}
.bz-tag{font-size:12px;font-weight:700;padding:5px 12px;border-radius:99px;background:#6EE86E;color:#123A12;}
.bz-action strong{font-size:16px;font-weight:700;}
.bz-action em{font-style:normal;font-size:12px;line-height:1.5;color:rgba(255,255,255,.62);}
.bz-card-history{border-radius:22px;padding:20px 18px;background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.09);}
.bz-history-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:6px;}
.bz-history-head h3{margin:0;font-size:16px;font-weight:700;}
.bz-history-head button{background:none;border:none;color:rgba(255,255,255,.5);font-size:13px;cursor:pointer;font-family:inherit;}
.bz-history{list-style:none;margin:0;padding:0;}
.bz-history li{display:flex;align-items:center;gap:14px;padding:14px 2px;cursor:pointer;border-radius:12px;
  animation:up .45s cubic-bezier(.22,1,.36,1) both;transition:background .2s;}
.bz-history li:hover{background:rgba(255,255,255,.05);}
.bz-avatar-sm{width:42px;height:42px;border-radius:50%;background:#fff;flex-shrink:0;}
.bz-history-txt{flex:1;min-width:0;}
.bz-history-txt strong{display:block;font-size:15px;font-weight:600;margin-bottom:3px;}
.bz-history-txt span{font-size:12.5px;color:rgba(255,255,255,.45);}
.bz-history b{font-size:16px;font-weight:700;white-space:nowrap;}

.bz-nav{display:flex;flex-shrink:0;padding:10px 6px 26px;border-top:1px solid rgba(255,255,255,.07);
  background:rgba(10,12,40,.9);backdrop-filter:blur(12px);}
.bz-nav-item{flex:1;background:none;border:none;cursor:pointer;font-family:inherit;display:flex;flex-direction:column;
  align-items:center;gap:4px;color:rgba(255,255,255,.42);font-size:11.5px;transition:color .2s;padding:2px 0;}
.bz-nav-item.on{color:#9C97FF;}
.bz-nav-icon{display:flex;padding:6px 14px;border-radius:12px;transition:background .25s;}
.bz-nav-icon.on{background:rgba(108,99,255,.2);}

/* ---------- 카메라 권한 ---------- */
.bz-perm-body{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:0 26px 30px;}
.bz-perm-title{font-size:19px;font-weight:800;margin:22px 0 10px;text-align:center;letter-spacing:-.5px;}
.bz-perm-sub{margin:0 0 30px;font-size:14px;line-height:1.6;color:#B9B3F0;text-align:center;}
.bz-perm-sub span{display:block;}

/* ---------- 스캐너 ---------- */
.bz-viewfinder{flex:1;position:relative;overflow:hidden;
  background:linear-gradient(180deg,#0A0D28 0%,#080B22 100%);}
.bz-corners{position:absolute;inset:22px 20px;pointer-events:none;}
.bz-corners span{position:absolute;width:44px;height:44px;border:2.5px solid #9C97FF;opacity:.85;}
.bz-corners.soft span{opacity:.5;}
.bz-corners.qr span{width:22px;height:22px;border-width:2px;opacity:.6;}
.bz-corners .tl{top:0;left:0;border-right:none;border-bottom:none;border-radius:8px 0 0 0;}
.bz-corners .tr{top:0;right:0;border-left:none;border-bottom:none;border-radius:0 8px 0 0;}
.bz-corners .bl{bottom:0;left:0;border-right:none;border-top:none;border-radius:0 0 0 8px;}
.bz-corners .br{bottom:0;right:0;border-left:none;border-top:none;border-radius:0 0 8px 0;}
.bz-vf-center{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:20px;}
.bz-vf-glyph{animation:pulseSoft 2.4s ease-in-out infinite;}
@keyframes pulseSoft{0%,100%{opacity:.6;transform:scale(1)}50%{opacity:1;transform:scale(1.05)}}
.bz-vf-center p{margin:0;text-align:center;font-size:14px;line-height:1.6;color:#B9B3F0;}
.bz-vf-center p span{display:block;}
.bz-scanline{position:absolute;left:24px;right:24px;height:2px;border-radius:2px;
  background:linear-gradient(90deg,transparent,#8B87F5,transparent);box-shadow:0 0 16px rgba(140,130,255,.8);
  animation:sweep 2.8s ease-in-out infinite;}
@keyframes sweep{0%{top:14%;opacity:0}12%{opacity:1}88%{opacity:1}100%{top:82%;opacity:0}}

.bz-shutter-row{display:flex;align-items:center;justify-content:space-between;padding:16px 24px 6px;flex-shrink:0;}
.bz-album{width:40px;height:40px;border-radius:8px;background:#fff;border:none;cursor:pointer;
  box-shadow:0 4px 14px rgba(0,0,0,.35);}
.bz-shutter{width:66px;height:66px;border-radius:50%;background:#fff;border:4px solid #6C63FF;cursor:pointer;
  box-shadow:0 0 0 4px rgba(108,99,255,.22),0 8px 22px rgba(0,0,0,.4);transition:transform .14s;}
.bz-shutter:active{transform:scale(.9);}
.bz-shutter-spacer{width:40px;}
.bz-mode-row{display:flex;justify-content:center;gap:44px;padding:6px 0 30px;flex-shrink:0;}
.bz-mode-row button{background:none;border:none;cursor:pointer;font-family:inherit;font-size:13.5px;
  color:rgba(255,255,255,.5);display:flex;flex-direction:column;align-items:center;gap:5px;transition:color .2s;}
.bz-mode-row button.on{color:#fff;font-weight:700;}
.bz-mode-row i{width:0;height:0;border-left:5px solid transparent;border-right:5px solid transparent;
  border-bottom:6px solid #fff;opacity:0;transition:opacity .2s;}
.bz-mode-row button.on i{opacity:1;}

/* iOS 권한 다이얼로그 */
.bz-ios-bg{position:absolute;inset:0;background:rgba(0,0,0,.35);display:flex;align-items:center;justify-content:center;
  z-index:15;animation:fade .2s both;}
.bz-ios-dialog{width:270px;border-radius:16px;background:rgba(48,48,52,.97);backdrop-filter:blur(20px);
  padding:20px 0 0;text-align:center;animation:popIn .26s cubic-bezier(.22,1,.36,1) both;overflow:hidden;}
@keyframes popIn{from{opacity:0;transform:scale(.9)}to{opacity:1;transform:none}}
.bz-ios-icon{display:flex;justify-content:center;margin-bottom:12px;}
.bz-ios-dialog h4{margin:0 0 6px;font-size:15.5px;font-weight:700;line-height:1.45;padding:0 18px;}
.bz-ios-dialog h4 span,.bz-ios-dialog p span{display:block;}
.bz-ios-dialog p{margin:0 0 16px;font-size:12.5px;line-height:1.5;color:rgba(255,255,255,.6);padding:0 18px;}
.bz-ios-actions{display:flex;flex-direction:column;border-top:.5px solid rgba(255,255,255,.16);}
.bz-ios-actions button{background:rgba(255,255,255,.04);border:none;border-bottom:.5px solid rgba(255,255,255,.12);
  padding:14px;font-size:15px;color:#fff;cursor:pointer;font-family:inherit;transition:background .15s;}
.bz-ios-actions button:last-child{border-bottom:none;}
.bz-ios-actions button:hover{background:rgba(255,255,255,.12);}

/* ---------- 인식 진행 ---------- */
.bz-proc{flex:1;position:relative;padding:0 20px;}
.bz-proc-head{position:absolute;top:44px;left:0;right:0;text-align:center;animation:up .4s cubic-bezier(.22,1,.36,1) both;}
.bz-proc-head h2{margin:0 0 8px;font-size:20px;font-weight:800;letter-spacing:-.5px;}
.bz-proc-head p{margin:0;font-size:14px;color:#B9B3F0;}
.bz-proc-head p.err{color:#FF8A7D;}
.bz-steps{position:absolute;left:0;right:0;top:52%;display:flex;align-items:flex-start;justify-content:center;gap:0;}
.bz-step{display:flex;flex-direction:column;align-items:center;gap:9px;width:96px;}
.bz-step-box{width:48px;height:48px;border-radius:14px;display:flex;align-items:center;justify-content:center;
  border:1.6px solid rgba(255,255,255,.18);transition:all .35s;}
.bz-step-box.active{border-color:#8B87F5;box-shadow:0 0 22px rgba(120,110,255,.45);background:rgba(108,99,255,.12);
  animation:stepPulse 1.2s ease-in-out infinite;}
@keyframes stepPulse{0%,100%{transform:scale(1)}50%{transform:scale(1.07)}}
.bz-step-box.done{border-color:rgba(150,140,255,.6);background:rgba(108,99,255,.08);}
.bz-step-box.bad{border-color:#FF7A6E;box-shadow:0 0 22px rgba(255,110,95,.35);background:rgba(255,110,95,.08);}
.bz-step span{font-size:12.5px;color:rgba(255,255,255,.4);transition:color .3s;}
.bz-step span.on{color:#fff;}
.bz-step span.bad{color:#FF8A7D;}
.bz-step-dash{align-self:flex-start;margin-top:20px;letter-spacing:2px;font-size:13px;color:rgba(255,255,255,.25);
  transition:color .35s;margin-left:-22px;margin-right:-22px;}
.bz-step-dash.on{color:rgba(160,150,255,.8);}
.bz-tipwrap{padding:0 20px 34px;flex-shrink:0;}
.bz-tip{display:flex;gap:12px;padding:16px;border-radius:16px;background:rgba(255,255,255,.05);
  border:1px solid rgba(255,255,255,.1);animation:up .5s .2s cubic-bezier(.22,1,.36,1) both;}
.bz-tip strong{display:block;font-size:14px;color:#8B87F5;margin-bottom:5px;}
.bz-tip p{margin:0;font-size:13px;line-height:1.55;color:rgba(255,255,255,.72);}

/* ---------- OCR 결과 ---------- */
.bz-ocr-body{flex:1;overflow:auto;padding:18px 20px 8px;}
.bz-ocr-body::-webkit-scrollbar{width:0;}
.bz-field-label{display:block;font-size:13.5px;color:rgba(255,255,255,.7);margin:0 0 8px;}
.bz-field{width:100%;height:52px;border-radius:12px;border:none;background:#fff;color:#15142E;font-size:15.5px;
  padding:0 16px;margin-bottom:20px;outline:none;box-shadow:0 2px 10px rgba(0,0,0,.25);}
.bz-field::placeholder{color:#B6B4C6;}
.bz-ocr-card{border-radius:16px;border:1.4px solid rgba(140,130,255,.45);background:rgba(30,30,80,.35);
  padding:8px 12px 14px;box-shadow:0 0 24px rgba(90,80,220,.12) inset;}
.bz-items{list-style:none;margin:0;padding:0;}
.bz-items li{display:flex;align-items:center;gap:6px;padding:5px 0;animation:up .4s cubic-bezier(.22,1,.36,1) both;}
.bz-cell{background:none;border:none;color:#fff;font-size:14.5px;outline:none;border-radius:6px;padding:6px 4px;
  transition:background .2s;min-width:0;}
.bz-cell:focus{background:rgba(255,255,255,.1);}
.bz-cell-name{flex:1;}
.bz-cell-qty{width:34px;text-align:center;color:rgba(255,255,255,.85);}
.bz-cell-price{display:flex;align-items:center;justify-content:flex-end;width:96px;}
.bz-cell-price .bz-cell{width:74px;text-align:right;font-weight:500;}
.bz-cell-price span{font-size:14.5px;padding-right:2px;}
.bz-del{background:none;border:none;cursor:pointer;padding:6px;display:flex;border-radius:6px;transition:background .2s;}
.bz-del:hover{background:rgba(255,255,255,.1);}
.bz-add-item{width:100%;margin:10px 0 12px;padding:14px;border-radius:12px;border:1.4px dashed rgba(255,255,255,.28);
  background:none;color:rgba(255,255,255,.72);font-size:14px;cursor:pointer;font-family:inherit;transition:all .2s;}
.bz-add-item:hover{border-color:rgba(160,150,255,.7);color:#fff;background:rgba(108,99,255,.08);}
.bz-total{display:flex;align-items:center;justify-content:space-between;padding-top:12px;
  border-top:1px solid rgba(255,255,255,.14);}
.bz-total span{font-size:16px;font-weight:700;}
.bz-total b{font-size:17px;font-weight:800;color:#A9A2FF;}
.bz-ocr-empty{margin:16px 4px;font-size:13.5px;color:rgba(255,255,255,.45);text-align:center;line-height:1.6;}

/* ---------- 영수증 목록 ---------- */
.bz-list-body{flex:1;overflow:auto;padding:20px 20px 8px;}
.bz-list-title{margin:0 0 14px;font-size:16px;font-weight:700;}
.bz-receipts{list-style:none;margin:0 0 22px;padding:0;display:flex;flex-direction:column;gap:12px;}
.bz-receipts li{display:flex;align-items:center;gap:14px;padding:20px 16px;border-radius:16px;
  background:linear-gradient(150deg,rgba(52,46,110,.85),rgba(36,32,84,.8));border:1px solid rgba(255,255,255,.07);
  animation:up .45s cubic-bezier(.22,1,.36,1) both;}
.bz-round-chip{font-size:12.5px;font-weight:700;color:#7DE87D;border:1.3px solid #4FCB6B;border-radius:99px;
  padding:4px 11px;flex-shrink:0;}
.bz-receipts strong{flex:1;font-size:15.5px;font-weight:600;}
.bz-receipts b{font-size:16px;font-weight:700;white-space:nowrap;}
.bz-del-r{opacity:.6;}
.bz-saved-btn{color:#7DE87D;border-color:rgba(125,232,125,.4);background:rgba(60,190,110,.12);}
.bz-sum-card{border-radius:18px;padding:20px;background:linear-gradient(150deg,#6B5BD6,#4C3FB0);
  box-shadow:0 16px 40px rgba(70,55,190,.3);animation:up .5s .2s cubic-bezier(.22,1,.36,1) both;}
.bz-sum-chip{display:inline-flex;align-items:center;gap:6px;font-size:12.5px;font-weight:600;padding:6px 12px;
  border-radius:99px;background:rgba(255,255,255,.18);}
.bz-sum-card p{margin:14px 0 0;display:flex;align-items:baseline;gap:5px;}
.bz-sum-card b{font-size:30px;font-weight:800;letter-spacing:-1px;}
.bz-sum-card i{font-style:normal;font-size:15px;color:rgba(255,255,255,.75);}

/* ---------- 저장 완료 ---------- */
.bz-saved{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding-bottom:120px;}
.bz-receipt-art{animation:float 4s ease-in-out infinite;filter:drop-shadow(0 20px 40px rgba(90,72,220,.35));}
.bz-coin-a{animation:coinA 3.2s ease-in-out infinite;}
.bz-coin-b{animation:coinB 3.6s ease-in-out infinite;}
@keyframes coinA{0%,100%{transform:translateY(0)}50%{transform:translateY(-9px)}}
@keyframes coinB{0%,100%{transform:translateY(0)}50%{transform:translateY(7px)}}
.bz-saved h2{margin:26px 0 10px;font-size:21px;font-weight:800;animation:up .5s .15s cubic-bezier(.22,1,.36,1) both;}
.bz-saved p{margin:0;font-size:14px;color:#B9B3F0;animation:up .5s .25s cubic-bezier(.22,1,.36,1) both;}

/* ---------- 정산 인원 ---------- */
.bz-people{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:0 24px 40px;}
.bz-people h2{margin:18px 0 8px;font-size:21px;font-weight:800;}
.bz-people>p{margin:0 0 30px;font-size:14px;color:#B9B3F0;}
.bz-counter{display:flex;align-items:center;gap:26px;margin-bottom:20px;}
.bz-cbtn{width:64px;height:64px;border-radius:16px;border:none;cursor:pointer;font-size:26px;color:#fff;
  font-family:inherit;transition:transform .14s,opacity .2s;display:flex;align-items:center;justify-content:center;}
.bz-cbtn:active{transform:scale(.92);}
.bz-cbtn.minus{background:#2C2C46;}
.bz-cbtn.plus{background:linear-gradient(160deg,#7565E4,#4F41B8);box-shadow:0 10px 26px rgba(90,72,220,.35);}
.bz-cbtn:disabled{opacity:.35;cursor:not-allowed;}
.bz-cnum{font-size:34px;font-weight:800;min-width:58px;text-align:center;animation:num .3s cubic-bezier(.22,1,.36,1);}
@keyframes num{from{opacity:0;transform:translateY(-8px) scale(.85)}to{opacity:1;transform:none}}
.bz-manual{display:flex;gap:8px;margin-top:4px;}
.bz-manual input{width:90px;height:44px;border-radius:10px;border:1.4px solid rgba(150,140,255,.5);
  background:rgba(255,255,255,.06);color:#fff;text-align:center;font-size:16px;outline:none;}
.bz-manual button{height:44px;padding:0 18px;border-radius:10px;border:none;background:${C.purple};color:#fff;
  font-family:inherit;font-size:14px;font-weight:600;cursor:pointer;}

/* ---------- QR ---------- */
.bz-qr-body{flex:1;padding:20px 20px 0;overflow:auto;}
.bz-qr-card{position:relative;border-radius:22px;padding:34px 20px 22px;text-align:center;
  background:linear-gradient(160deg,rgba(78,66,168,.75),rgba(40,34,96,.7) 60%,rgba(28,24,72,.65));
  border:1px solid rgba(160,150,255,.22);box-shadow:0 22px 50px rgba(0,0,0,.4);
  animation:up .5s cubic-bezier(.22,1,.36,1) both;}
.bz-qr-notch{position:absolute;top:0;left:50%;transform:translateX(-50%);width:96px;height:16px;
  border-radius:0 0 16px 16px;background:rgba(20,18,54,.9);}
.bz-qr-guide{margin:0 0 20px;font-size:16px;font-weight:600;line-height:1.5;}
.bz-qr-guide span{display:block;}
.bz-qr-frame{position:relative;width:212px;height:212px;margin:0 auto 16px;display:flex;align-items:center;justify-content:center;}
.bz-qr-frame .bz-corners{inset:0;}
.bz-qr{border-radius:6px;box-shadow:0 8px 26px rgba(0,0,0,.4);}
.bz-qr-name{margin:0 0 4px;font-size:15.5px;font-weight:600;color:#C9C3FF;}
.bz-qr-date{font-size:13.5px;color:rgba(255,255,255,.5);}
.bz-qr-meta{margin-top:8px;font-size:12.5px;color:rgba(255,255,255,.45);}
.bz-qr-btns{display:flex;gap:10px;justify-content:center;margin-top:18px;}
.bz-qr-btn{display:flex;align-items:center;gap:7px;padding:12px 16px;border-radius:12px;border:none;cursor:pointer;
  font-family:inherit;font-size:13.5px;font-weight:600;color:#fff;transition:transform .15s;}
.bz-qr-btn:active{transform:scale(.95);}
.bz-qr-btn.ghost{background:rgba(255,255,255,.14);}
.bz-qr-btn.solid{background:linear-gradient(160deg,#7565E4,#4F41B8);}

/* 토스트 */
.bz-toast{position:absolute;left:50%;bottom:104px;transform:translateX(-50%);z-index:40;
  background:rgba(28,26,66,.96);border:1px solid rgba(150,140,255,.28);color:#fff;padding:12px 20px;border-radius:99px;
  font-size:13.5px;white-space:nowrap;animation:toastIn .3s cubic-bezier(.22,1,.36,1) both;box-shadow:0 12px 30px rgba(0,0,0,.4);}
@keyframes toastIn{from{opacity:0;transform:translate(-50%,12px)}to{opacity:1;transform:translate(-50%,0)}}

/* ---------- 정산방 공통 ---------- */
.bz-heldpaper{animation:paperSway 3.6s ease-in-out infinite;transform-origin:64px 20px;}
@keyframes paperSway{0%,100%{transform:rotate(-3deg)}50%{transform:rotate(3deg)}}
.bz-thinking{display:flex;gap:8px;margin-bottom:-8px;}
.bz-thinking i{width:9px;height:9px;border-radius:50%;background:#7DE87D;animation:think 1.2s ease-in-out infinite;}
.bz-thinking i:nth-child(2){animation-delay:.15s}
.bz-thinking i:nth-child(3){animation-delay:.3s}
@keyframes think{0%,100%{opacity:.3;transform:translateY(0)}50%{opacity:1;transform:translateY(-5px)}}

.bz-enter{flex:1;display:flex;flex-direction:column;align-items:center;padding:78px 24px 0;}
.bz-enter h2{margin:16px 0 8px;font-size:20px;font-weight:800;}
.bz-enter p{margin:0 0 34px;font-size:14px;color:#B9B3F0;}

.bz-name{flex:1;display:flex;flex-direction:column;align-items:center;padding:56px 24px 0;}
.bz-name h2{margin:14px 0 22px;font-size:20px;font-weight:800;}
.bz-name-field{position:relative;width:100%;}
.bz-name-field input{width:100%;height:56px;border-radius:14px;border:1px solid rgba(160,150,255,.3);
  background:rgba(255,255,255,.09);color:#fff;font-size:16px;padding:0 46px 0 18px;outline:none;transition:border-color .2s,background .2s;}
.bz-name-field input:focus{border-color:rgba(170,160,255,.75);background:rgba(255,255,255,.14);}
.bz-name-field input::placeholder{color:rgba(255,255,255,.45);}
.bz-name-field button{position:absolute;right:12px;top:50%;transform:translateY(-50%);background:none;border:none;
  cursor:pointer;padding:6px;display:flex;opacity:.75;}
.bz-name>p{margin:12px 0 0;font-size:13px;color:rgba(255,255,255,.45);}

.bz-pick{flex:1;display:flex;flex-direction:column;align-items:center;padding:26px 20px 0;overflow:auto;}
.bz-pick h2{margin:10px 0 8px;font-size:20px;font-weight:800;text-align:center;letter-spacing:-.5px;}
.bz-pick>p{margin:0 0 24px;font-size:13.5px;color:#B9B3F0;text-align:center;}
.bz-pick-list{list-style:none;margin:0;padding:0;width:100%;display:flex;flex-direction:column;gap:12px;}
.bz-pick-list li{animation:up .45s cubic-bezier(.22,1,.36,1) both;}
.bz-pick-list button{width:100%;display:flex;align-items:center;gap:14px;padding:20px 16px;border-radius:16px;
  background:rgba(255,255,255,.05);border:1.6px solid rgba(255,255,255,.08);cursor:pointer;font-family:inherit;
  color:#fff;transition:all .22s;}
.bz-pick-list button:hover{background:rgba(255,255,255,.08);}
.bz-pick-list button.on{border-color:#8B87F5;background:rgba(108,99,255,.14);box-shadow:0 0 24px rgba(120,105,255,.28);}
.bz-pick-list strong{flex:1;text-align:left;font-size:15.5px;font-weight:600;}
.bz-pick-list b{font-size:16px;font-weight:700;white-space:nowrap;}

.bz-wait{flex:1;display:flex;flex-direction:column;align-items:center;padding:26px 24px 0;overflow:auto;}
.bz-wait h2{margin:12px 0 8px;font-size:20px;font-weight:800;}
.bz-wait>p{margin:0 0 28px;font-size:14px;color:#B9B3F0;}
.bz-wait-row{width:100%;display:flex;justify-content:space-between;align-items:baseline;margin-bottom:10px;}
.bz-wait-row span{font-size:15px;font-weight:600;}
.bz-wait-row b{font-size:15px;font-weight:700;color:#C7C1FF;}
.bz-av-row{width:100%;display:flex;justify-content:space-between;gap:4px;margin-top:20px;}
.bz-av-cell{display:flex;flex-direction:column;align-items:center;gap:7px;flex:1;min-width:0;}
.bz-av-cell span{font-size:11.5px;color:rgba(255,255,255,.35);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%;}
.bz-av-cell span.on{color:rgba(255,255,255,.8);}
.bz-av{position:relative;border-radius:50%;display:flex;align-items:center;justify-content:center;
  border:2px solid rgba(255,255,255,.12);transition:all .4s cubic-bezier(.22,1,.36,1);}
.bz-av.on{border-color:#9C97FF;box-shadow:0 0 16px rgba(120,110,255,.4);animation:avIn .45s cubic-bezier(.22,1,.36,1);}
@keyframes avIn{from{transform:scale(.6);opacity:0}to{transform:scale(1);opacity:1}}
.bz-av span{font-size:15px;font-weight:700;color:#fff;}
.bz-av i{position:absolute;right:-1px;bottom:-1px;width:16px;height:16px;border-radius:50%;background:#3A3A5E;
  display:flex;align-items:center;justify-content:center;border:2px solid #0A0D28;transition:background .3s;}
.bz-av i.on{background:#6C5CE7;}
.bz-clock-art{animation:float 4s ease-in-out infinite;filter:drop-shadow(0 20px 40px rgba(90,72,220,.35));}
.bz-pending{width:100%;margin-top:22px;padding:16px;border-radius:14px;background:rgba(255,255,255,.06);
  border:1px solid rgba(255,255,255,.1);text-align:center;}
.bz-pending span{display:block;font-size:13px;line-height:1.65;color:rgba(255,255,255,.7);}

/* ---------- 금액 조정 ---------- */
.bz-adj-body{flex:1;overflow:auto;padding:18px 20px 8px;}
.bz-adj-body::-webkit-scrollbar{width:0;}
.bz-readonly{padding:14px 16px;}
.bz-readonly .bz-items li{gap:10px;padding:7px 0;}
.bz-ro-name{flex:1;font-size:14.5px;}
.bz-ro-qty{width:30px;text-align:center;font-size:14.5px;color:rgba(255,255,255,.8);}
.bz-ro-price{width:96px;text-align:right;font-size:14.5px;}
.bz-mine-card{position:relative;margin:18px 0 24px;padding:18px 20px;border-radius:18px;
  background:linear-gradient(150deg,#5D4FC4,#3E3496);box-shadow:0 16px 40px rgba(70,55,190,.3);}
.bz-mine-chip{display:inline-flex;align-items:center;gap:6px;font-size:12.5px;font-weight:600;padding:6px 12px;
  border-radius:99px;background:rgba(255,255,255,.2);}
.bz-mine-card p{margin:12px 0 0;display:flex;align-items:baseline;gap:5px;}
.bz-mine-card b{font-size:29px;font-weight:800;letter-spacing:-1px;animation:num .3s cubic-bezier(.22,1,.36,1);}
.bz-mine-card i{font-style:normal;font-size:15px;color:rgba(255,255,255,.75);}
.bz-cut{position:absolute;right:20px;bottom:20px;font-size:13px;font-weight:600;padding:6px 13px;border-radius:99px;
  background:rgba(125,232,125,.16);color:#8FEE8F;border:1px solid rgba(125,232,125,.4);transition:all .25s;}
.bz-cut.on{background:rgba(125,232,125,.26);}
.bz-ai-title{margin:0 0 6px;font-size:16px;font-weight:800;}
.bz-ai-sub{margin:0 0 14px;font-size:13px;color:#9E97E8;}
.bz-chips{display:flex;flex-wrap:wrap;gap:9px;}
.bz-chips button{padding:11px 18px;border-radius:99px;border:1.4px solid rgba(255,255,255,.2);background:none;
  color:rgba(255,255,255,.8);font-family:inherit;font-size:14px;cursor:pointer;transition:all .2s;}
.bz-chips button:hover{border-color:rgba(160,150,255,.6);}
.bz-chips button.on{border-color:#8B87F5;background:rgba(108,99,255,.2);color:#fff;font-weight:600;
  box-shadow:0 0 18px rgba(120,105,255,.3);}
.bz-info{display:flex;gap:12px;align-items:center;margin:18px 0 4px;padding:16px;border-radius:14px;
  background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.09);}
.bz-info p{margin:0;font-size:12.5px;line-height:1.6;color:rgba(255,255,255,.6);}
.bz-info p span{display:block;}

/* ---------- 정산 결과 ---------- */
.bz-result-body{flex:1;overflow:auto;padding:18px 20px 8px;}
.bz-result-body::-webkit-scrollbar{width:0;}
.bz-res-summary{border-radius:20px;padding:20px;background:rgba(255,255,255,.06);border:1px solid rgba(255,255,255,.1);
  animation:up .45s cubic-bezier(.22,1,.36,1) both;}
.bz-res-meta{font-size:13px;color:rgba(255,255,255,.6);}
.bz-res-total{margin:8px 0 16px;display:flex;align-items:baseline;gap:5px;}
.bz-res-total b{font-size:30px;font-weight:800;letter-spacing:-1px;}
.bz-res-total i{font-style:normal;font-size:15px;color:rgba(255,255,255,.7);}
.bz-res-rounds{display:flex;gap:8px;padding-bottom:16px;border-bottom:1px solid rgba(255,255,255,.12);}
.bz-res-rounds>div{flex:1;text-align:center;}
.bz-res-chip{display:inline-block;font-size:11.5px;font-weight:600;padding:5px 10px;border-radius:99px;
  background:rgba(108,99,255,.4);margin-bottom:8px;white-space:nowrap;}
.bz-res-rounds b{display:block;font-size:14.5px;font-weight:700;}
.bz-res-payer{padding-top:14px;font-size:13.5px;color:rgba(255,255,255,.7);}
.bz-res-payer b{color:#B9B3FF;font-size:14.5px;}
.bz-person{margin-bottom:14px;padding:18px;border-radius:18px;background:rgba(255,255,255,.05);
  border:1px solid rgba(255,255,255,.08);animation:up .45s cubic-bezier(.22,1,.36,1) both;}
.bz-person-head{display:flex;align-items:center;gap:8px;margin-bottom:12px;}
.bz-person-head strong{font-size:17px;font-weight:700;}
.bz-person-head b{margin-left:auto;font-size:17px;font-weight:800;}
.bz-me-badge{font-size:11.5px;font-weight:700;padding:3px 9px;border-radius:99px;background:#6C5CE7;}
.bz-person-row{padding:11px 0;border-top:1px solid rgba(255,255,255,.08);}
.bz-person-row:first-of-type{border-top:none;padding-top:0;}
.bz-person-row p{margin:0 0 8px;font-size:14px;color:rgba(255,255,255,.85);}
.bz-tags{display:flex;flex-wrap:wrap;gap:6px;}
.bz-tag-g{font-size:12px;padding:5px 11px;border-radius:99px;background:rgba(90,200,120,.16);color:#8FE0A0;
  border:1px solid rgba(90,200,120,.28);}
.bz-tag-g.dim{background:rgba(255,255,255,.07);color:rgba(255,255,255,.5);border-color:rgba(255,255,255,.14);}

/* ---------- 정산 완료 ---------- */
.bz-done-body{flex:1;overflow:auto;padding:8px 20px 0;}
.bz-done-body::-webkit-scrollbar{width:0;}
.bz-done-card{position:relative;border-radius:24px;padding:24px 20px 22px;text-align:center;
  background:linear-gradient(165deg,rgba(70,60,150,.6),rgba(36,32,90,.7));border:1px solid rgba(160,150,255,.18);
  box-shadow:0 22px 50px rgba(0,0,0,.4);animation:up .5s cubic-bezier(.22,1,.36,1) both;overflow:hidden;}
.bz-check-art{animation:float 4s ease-in-out infinite;filter:drop-shadow(0 16px 32px rgba(90,72,220,.4));}
.bz-done-card h2{margin:6px 0 6px;font-size:19px;font-weight:800;}
.bz-done-total{margin:0 0 6px;font-size:32px;font-weight:800;letter-spacing:-1.2px;}
.bz-done-meta{font-size:13.5px;color:rgba(255,255,255,.6);}
.bz-perf{position:relative;height:22px;margin:16px -20px;border-top:2px dashed rgba(255,255,255,.28);}
.bz-perf .l,.bz-perf .r{position:absolute;top:-13px;width:24px;height:24px;border-radius:50%;background:#0B0E28;}
.bz-perf .l{left:-12px;} .bz-perf .r{right:-12px;}
.bz-done-chip{margin-bottom:14px;background:rgba(108,99,255,.55);}
.bz-done-rounds{text-align:left;}
.bz-done-rounds>div{margin-bottom:12px;}
.bz-done-rounds p{margin:0 0 7px;font-size:14.5px;color:rgba(255,255,255,.8);}
.bz-done-rounds p b{font-weight:700;color:#fff;margin-left:6px;}
.bz-account{display:flex;align-items:center;gap:12px;margin-top:6px;padding:14px 16px;border-radius:14px;
  background:rgba(255,255,255,.09);text-align:left;width:100%;border:none;cursor:pointer;font-family:inherit;
  color:#fff;transition:background .2s;}
.bz-account:hover{background:rgba(255,255,255,.16);}
.bz-account:active{transform:scale(.98);}
.bz-account strong{font-size:14px;font-weight:700;white-space:nowrap;}
.bz-account span{flex:1;font-size:13.5px;color:rgba(255,255,255,.7);border-left:1px solid rgba(255,255,255,.2);padding-left:12px;}
.bz-account i{display:flex;}
.bz-copy-hint{display:block;margin-top:10px;font-size:12px;color:rgba(255,255,255,.4);}

/* ---------- 직접 입력 ---------- */
.bz-manual-title{margin:6px 0 8px;font-size:19px;font-weight:800;text-align:center;letter-spacing:-.5px;}
.bz-manual-sub{margin:0 0 24px;font-size:13.5px;line-height:1.6;color:#9E97E8;text-align:center;}
.bz-manual-sub span{display:block;}
.bz-why{align-items:flex-start;background:rgba(108,99,255,.1);border-color:rgba(140,130,255,.3);}
.bz-why strong{display:block;font-size:14px;color:#A9A2FF;margin-bottom:5px;}
.bz-why p{margin:0;font-size:12.5px;line-height:1.6;color:rgba(255,255,255,.68);}
.bz-why p span{display:block;}

/* ---------- 정산내역 ---------- */
.bz-hist-scroll{flex:1;overflow:auto;padding:6px 20px 20px;}
.bz-hist-scroll::-webkit-scrollbar{width:0;}
.bz-month-card{margin:8px 0 22px;padding:20px;border-radius:20px;border:1.4px solid rgba(140,130,255,.45);
  background:linear-gradient(155deg,rgba(60,50,140,.5),rgba(30,26,80,.4));animation:up .45s cubic-bezier(.22,1,.36,1) both;}
.bz-month-head{display:flex;align-items:center;justify-content:space-between;gap:6px;}
.bz-month-head h2{margin:0;font-size:19px;font-weight:800;line-height:1.45;letter-spacing:-.5px;}
.bz-month-head h2 span{display:block;}
.bz-month-stats{display:flex;margin-top:16px;}
.bz-month-stats>div{flex:1;text-align:center;}
.bz-month-stats>div:first-child{border-right:1px solid rgba(255,255,255,.16);}
.bz-month-stats span{display:block;font-size:13px;color:rgba(255,255,255,.55);margin-bottom:6px;}
.bz-month-stats b{font-size:20px;font-weight:800;}
.bz-month-empty{display:flex;flex-direction:column;align-items:center;padding:14px 20px 22px;}
.bz-month-empty p{margin:6px 0 0;font-size:15.5px;font-weight:600;}
.bz-hist-title{margin:0 0 12px;font-size:14.5px;font-weight:600;color:rgba(255,255,255,.72);}
.bz-hist-list{list-style:none;margin:0 0 24px;padding:0;display:flex;flex-direction:column;gap:12px;}
.bz-hist-list li{padding:18px;border-radius:18px;background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.08);
  cursor:pointer;transition:all .2s;animation:up .45s cubic-bezier(.22,1,.36,1) both;}
.bz-hist-list li:hover{background:rgba(108,99,255,.12);border-color:rgba(140,130,255,.4);}
.bz-hist-list strong{display:block;font-size:16px;font-weight:700;margin-bottom:6px;}
.bz-hist-list>li>span{font-size:12.5px;color:rgba(255,255,255,.5);}
.bz-hist-list li>div{display:flex;align-items:center;justify-content:space-between;margin-top:14px;}
.bz-hist-list em{display:flex;align-items:center;gap:6px;font-style:normal;font-size:13px;color:rgba(255,255,255,.6);}
.bz-hist-list b{font-size:17px;font-weight:700;}
.bz-demo-link{display:block;width:100%;margin:4px 0 8px;background:none;border:none;color:rgba(255,255,255,.24);
  font-size:12px;cursor:pointer;font-family:inherit;padding:8px;}

.bz-att-head{display:flex;align-items:baseline;justify-content:space-between;margin:24px 0 12px;}
.bz-att-head span{font-size:13px;color:rgba(255,255,255,.5);}
.bz-att-list{list-style:none;margin:0 0 12px;padding:0;display:flex;flex-direction:column;gap:10px;}
.bz-att-list li{display:flex;align-items:center;gap:14px;padding:14px;border-radius:14px;background:rgba(255,255,255,.05);
  border:1px solid rgba(255,255,255,.08);}
.bz-att-thumb{width:56px;height:56px;border-radius:8px;background:#D9D9D9;flex-shrink:0;}
.bz-att-list strong{display:block;font-size:14.5px;font-weight:600;margin-bottom:4px;}
.bz-att-list li span{font-size:12.5px;color:rgba(255,255,255,.5);}
.bz-att-list li>div{flex:1;min-width:0;}
.bz-att-list button{background:none;border:none;color:rgba(255,255,255,.55);font-size:18px;cursor:pointer;
  padding:4px 8px;letter-spacing:1px;}
.bz-att-add{width:100%;margin-bottom:8px;padding:22px;border-radius:14px;border:1.6px dashed rgba(140,130,255,.5);
  background:none;color:rgba(255,255,255,.75);font-size:13.5px;font-family:inherit;cursor:pointer;
  display:flex;flex-direction:column;align-items:center;gap:8px;transition:all .2s;}
.bz-att-add:hover{background:rgba(108,99,255,.1);border-color:#8B87F5;color:#fff;}

/* ---------- 마이페이지 ---------- */
.bz-my-body{flex:1;padding:6px 20px 0;overflow:auto;}
.bz-profile-card{display:flex;align-items:center;gap:18px;margin:12px 0 30px;padding:22px 20px;border-radius:20px;
  background:linear-gradient(150deg,rgba(78,66,168,.6),rgba(40,34,96,.55));border:1px solid rgba(160,150,255,.22);
  animation:up .45s cubic-bezier(.22,1,.36,1) both;}
.bz-profile-av{width:88px;height:88px;border-radius:50%;background:#12142F;border:2.5px solid #8B7BEF;
  display:flex;align-items:center;justify-content:center;flex-shrink:0;}
.bz-profile-card strong{display:block;font-size:21px;font-weight:700;margin-bottom:5px;}
.bz-profile-card span{font-size:13.5px;color:rgba(255,255,255,.55);}
.bz-my-label{margin:0 0 6px;font-size:14px;font-weight:500;color:rgba(255,255,255,.55);}
.bz-my-list{list-style:none;margin:0;padding:0;}
.bz-my-list button{width:100%;text-align:left;background:none;border:none;color:#fff;font-family:inherit;
  font-size:16.5px;padding:15px 0;cursor:pointer;transition:color .2s;}
.bz-my-list button:hover{color:#B9B3FF;}

/* ---------- 계좌 관리 ---------- */
.bz-acc-body{flex:1;overflow:auto;padding:18px 20px 8px;}
.bz-acc-body::-webkit-scrollbar{width:0;}
.bz-acc-body .bz-info{margin:0 0 20px;align-items:flex-start;}
.bz-acc-card{padding:20px;border-radius:16px;margin-bottom:26px;
  background:linear-gradient(150deg,rgba(96,82,200,.55),rgba(44,38,110,.5));border:1px solid rgba(160,150,255,.22);
  animation:up .45s cubic-bezier(.22,1,.36,1) both;}
.bz-acc-card>div{display:flex;align-items:center;gap:12px;padding:7px 0;}
.bz-acc-card span{font-size:13.5px;color:rgba(255,255,255,.6);width:64px;}
.bz-acc-card b{font-size:16px;font-weight:700;border-left:1px solid rgba(255,255,255,.28);padding-left:12px;}
.bz-select,.bz-acc-input{display:flex;align-items:center;gap:12px;height:56px;padding:0 16px;border-radius:14px;
  margin-bottom:20px;background:rgba(255,255,255,.06);border:1px solid rgba(160,150,255,.24);transition:border-color .2s;}
.bz-select:focus-within,.bz-acc-input:focus-within{border-color:rgba(170,160,255,.7);}
.bz-select select{flex:1;background:none;border:none;color:#fff;font-size:15.5px;font-family:inherit;outline:none;
  appearance:none;cursor:pointer;}
.bz-select select option{background:#141334;color:#fff;}
.bz-acc-input input{flex:1;background:none;border:none;color:#fff;font-size:15.5px;outline:none;}
.bz-acc-input input::placeholder{color:rgba(255,255,255,.35);}

/* ---------- 화면 바로가기 ---------- */
.bz-jump{width:min(860px,100%);display:flex;flex-direction:column;gap:12px;padding:16px 18px 20px;
  border-radius:18px;background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);}
.bz-jump-group{display:flex;align-items:flex-start;gap:14px;}
.bz-jump-label{flex-shrink:0;width:88px;padding-top:7px;font-size:11.5px;font-weight:600;
  color:rgba(255,255,255,.4);letter-spacing:-.2px;}
.bz-jump-chips{display:flex;flex-wrap:wrap;gap:6px;}
.bz-jump-chips button{padding:7px 13px;border-radius:99px;border:1px solid rgba(255,255,255,.14);
  background:rgba(255,255,255,.05);color:rgba(255,255,255,.65);font-family:inherit;font-size:12.5px;
  cursor:pointer;transition:all .18s;white-space:nowrap;}
.bz-jump-chips button:hover{background:rgba(108,99,255,.2);border-color:rgba(150,140,255,.5);color:#fff;}
.bz-jump-chips button.on{background:#6C5CE7;border-color:#8B87F5;color:#fff;font-weight:600;
  box-shadow:0 0 16px rgba(120,105,255,.4);}

@media (prefers-reduced-motion:reduce){
  *{animation-duration:.01ms !important;animation-iteration-count:1 !important;transition-duration:.01ms !important;}
}
@media (max-width:430px){
  .bz-root{padding:0 0 8px;gap:8px;}
  .bz-device{width:100vw;height:76vh;border-radius:0;box-shadow:none;}
  .bz-hint{display:none;}
  .bz-jump{border-radius:0;border-left:none;border-right:none;padding:12px;gap:8px;
    max-height:24vh;overflow:auto;}
  .bz-jump-group{flex-direction:column;gap:6px;}
  .bz-jump-label{width:auto;padding-top:0;}
}
    `}</style>
  );
}
