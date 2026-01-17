# Powerful Private Asset Manager

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
로컬 개발 시에는 환경 변수를 수동으로 등록하는 번거로움을 피하기 위해 `application-local.properties` 파일을 사용합니다.
1. `src/main/resources/application.properties.example` 파일을 복사하여 `src/main/resources/application-local.properties` 파일을 생성합니다.
2. 생성한 파일에 본인의 `google.sheet.id`와 `public-data.api.key`를 입력합니다.

### 2단계: 애플리케이션 실행

루트 디렉토리에서 아래 명령어를 실행하면 백엔드와 프론트엔드가 동시에 구동됩니다.

```bash
# 의존성 설치 (최초 1회)
npm install

# 서비스 통합 실행 (백엔드 local 프로필 자동 적용)
npm run dev
```

- **백엔드**: [http://localhost:8080](http://localhost:8080)
- **프론트엔드**: [http://localhost:5173](http://localhost:5173)

---

## ☁️ 배포 안내 (GitHub 게시 및 상용 배포)

이 프로젝트는 상용 배포를 고려하여 모든 민감 정보가 환경 변수화 되어 있습니다.

### GitHub 게시 시 주의사항
- `google-credentials.json`, `application-local.properties`, `.env` 파일은 `.gitignore`에 의해 자동으로 제외됩니다. 절대 강제로 추가하여 푸시하지 마십시오.

### 배포 환경(상용) 설정
배포 플랫폼(Railway, AWS, Heroku 등)의 **Environment Variables** 설정 섹션에 아래 변수들을 등록해야 합니다:

- `GOOGLE_SHEET_ID`: 연동할 구글 시트 ID
- `PUBLIC_DATA_API_KEY`: 공공데이터 포털 API 키
- `SPRING_DATASOURCE_URL`: (선택) 외부 DB 사용 시 DB 접속 URL
- `SPRING_DATASOURCE_USERNAME`: (선택) DB 사용자 이름
- `SPRING_DATASOURCE_PASSWORD`: (선택) DB 비밀번호
- `VITE_API_URL`: 프론트엔드 빌드 시 백엔드 API 주소 (예: `https://api.yourdomain.com/api`)

---

## 🛠 기술 스택
- **Backend**: Java 17, Spring Boot 3.2.1, Spring Data JPA, H2 (In-memory)
- **Frontend**: React, Vite, Axios, Tailwind CSS
- **Integration**: Google Sheets API v4
