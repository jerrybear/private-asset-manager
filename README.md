# PAM (Private Asset Manager)

개인 자산 관리 및 구글 시트 연동을 위한 자산 관리 도구입니다.

## 🚀 구동 방법

이 프로젝트는 백엔드(Spring Boot)와 프론트엔드(Vite + React)가 함께 구성된 프로젝트입니다.

### 1단계: 환경 설정

#### Google Sheets API 설정
1. [Google Cloud Console](https://console.cloud.google.com/)에서 프로젝트를 생성하고 Sheets API를 활성화합니다.
2. 서비스 계정 키를 생성하여 `google-credentials.json` 파일로 다운로드합니다.
3. 해당 파일을 아래 경로에 위치시킵니다:
   - `src/main/resources/google-credentials.json`

#### 로컬 개발용 설정 파일 생성
로컬 개발 시에는 환경 변수를 수동으로 등록하는 번거로움을 피하기 위해 `application.properties` 또는 `application-local.properties` 파일을 사용합니다.
1. `src/main/resources/application.properties.example` 파일을 참고하여 필요한 설정을 입력합니다.
2. **인증 비밀번호 설정**: `APP_AUTH_PASSCODE` 환경 변수 또는 프로퍼티를 통해 로그인 비밀번호를 설정할 수 있습니다 (기본값: `1234`).

### 2단계: 애플리케이션 실행

루트 디렉토리에서 아래 명령어를 실행하면 백엔드와 프론트엔드가 동시에 구동됩니다.

```bash
# 의존성 설치 (최초 1회)
npm install

# 서비스 통합 실행
npm run dev
```

- **백엔드**: [http://localhost:8080](http://localhost:8080)
- **프론트엔드**: [http://localhost:5173](http://localhost:5173)

---

## ☁️ 배포 안내

이 프로젝트는 보안을 위해 모든 민감 정보가 환경 변수화 되어 있습니다.

### 배포 환경(상용) 설정
배포 플랫폼(Vercel, Railway, AWS 등)의 **Environment Variables** 설정 섹션에 아래 변수들을 등록해야 합니다:

- `APP_AUTH_PASSCODE`: 접속 비밀번호 (필수)
- `GOOGLE_SHEET_ID`: 연동할 구글 시트 ID (필수)
- `PUBLIC_DATA_API_KEY`: 공공데이터 포털 API 키 (필수)
- `VITE_API_URL`: 백엔드 API 서버 주소 (예: `http://your-api-server.com`) - **주의: 끝에 `/api`를 붙이지 마세요.**
- `SESSION_COOKIE_SAME_SITE`: (선택) 크로스 도메인 테스트 시 `lax` 또는 `none` 설정
- `SESSION_COOKIE_SECURE`: (선택) HTTP 환경 테스트 시 `false` 설정

---

## ✨ 주요 기능
- **자산 통합 관리**: 여러 계좌의 자산을 한곳에서 관리 및 실시간 평가액 조회
- **구글 시트 연동**: 구글 시트와의 양방향 동기화(Import/Export) 지원
- **시세 자동 갱신**: 공공데이터 API를 통한 국내 주식/ETF 실시간 시세 반영
- **배당금 분석**: 계좌 유형별(일반/기타) 예상 연간 배당금 구분 표시 (금융종합소득 관리 최적화)
- **뉴스 인사이트**: 보유 종목 관련 최신 뉴스 자동 수집 및 제공

## 🛠 기술 스택
- **Backend**: Java 17, Spring Boot 3.2.1, Spring Data JPA, H2 (In-memory)
- **Frontend**: React, Vite, Axios, Lucide React
- **Integration**: Google Sheets API v4
