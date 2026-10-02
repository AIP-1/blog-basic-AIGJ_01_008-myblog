# ☕ Java 블로그

**2팀 AIGJ_01_008 박민수**

Spring Boot로 만든 블로그 서비스입니다. 처음 실행하면 **Java 입문·중급 강좌 글 13개**가 자동으로 등록됩니다.

## 기술 스택

- Java 17, Spring Boot 3.3
- Spring MVC + Thymeleaf (화면)
- Spring Data JPA + H2 (파일 DB, `./data/blog.mv.db`)
- Spring Security (회원가입, 로그인)
- commonmark (마크다운 → HTML), highlight.js (코드 하이라이팅)

## 실행 방법

```bash
# JDK 17, Maven 설치 (macOS)
brew install openjdk@17 maven

# 실행
git clone https://github.com/AIP-1/blog-basic-AIGJ_01_008-myblog.git
cd blog-basic-AIGJ_01_008-myblog
mvn spring-boot:run
```

브라우저에서 http://localhost:8080 에 접속하세요.

- 관리자 계정: `admin` / `admin1234` (로컬 기본값, 모든 글·댓글 수정/삭제 가능)
- 새 계정은 상단의 **회원가입**에서 만들 수 있습니다.

## 기능

| 기능 | 설명 |
|---|---|
| 회원 | 회원가입, 로그인, 로그아웃 |
| 게시글 | 작성/조회/수정/삭제, 마크다운 작성, 카테고리(입문/중급/자유) |
| 목록 | 카테고리 필터, 제목·내용 검색, 페이지네이션 |
| 강좌 | 사이드바 목차, 이전/다음 강좌 이동 |
| 댓글 | 작성, 삭제 (본인 또는 관리자) |
| 코드 실행기 | `/playground` 에서 Java 코드 작성·실행, 강좌 코드 블록의 ▶ 실행해보기 버튼 |

## 코드 실행기 안전장치

로그인한 사용자만 실행할 수 있고, 별도 JVM 프로세스에서 다음 제한을 걸고 실행합니다.

- 실행 시간 5초, 메모리 64MB, 출력 20KB, 동시 실행 2개
- Security Manager 로 파일·네트워크 접근과 외부 프로그램 실행 차단

## 프로젝트 구조

```
src/main/java/com/example/blog
├── domain/        엔티티 (User, Post, Comment, Category)
├── repository/    JPA 리포지토리
├── service/       비즈니스 로직 (PostService, UserService, MarkdownService)
├── web/           컨트롤러와 폼
└── config/        보안 설정, 초기 데이터 등록
src/main/resources
├── posts/         강좌 글 마크다운 원본 (최초 실행 시 DB에 등록)
├── templates/     Thymeleaf 화면
└── static/css/    스타일
```

## 강좌 글 추가·수정하기

`src/main/resources/posts/`에 아래 형식의 `.md` 파일을 추가하면 됩니다.

```markdown
---
title: 14. 새 강좌 제목
category: INTERMEDIATE
seq: 14
---
본문 (마크다운)
```

강좌 글은 **DB가 비어 있을 때만** 등록됩니다. 다시 등록하려면 서버를 끄고 `data/` 폴더를 지운 뒤 재실행하세요. (직접 쓴 글도 함께 지워집니다.)

## 배포 (Render)

`Dockerfile` 과 `render.yaml` 이 포함되어 있어 Render 무료 플랜에 그대로 배포할 수 있습니다.

1. https://render.com 에 GitHub 계정으로 로그인
2. **New → Blueprint** → 이 저장소 선택
3. `ADMIN_PASSWORD` 에 관리자 비밀번호 입력 → **Apply**
4. 빌드가 끝나면 `https://java-blog-xxxx.onrender.com` 주소로 접속

| 환경변수 | 설명 | 기본값 |
|---|---|---|
| `ADMIN_PASSWORD` | 관리자(`admin`) 비밀번호 | `admin1234` |
| `PORT` | 서버 포트 (Render 가 자동 설정) | `8080` |

> 무료 플랜은 15분간 접속이 없으면 잠들었다가, 다음 접속 때 30초~1분 정도 걸려 깨어납니다.
> 또한 디스크가 유지되지 않아 재배포·재시작 시 직접 쓴 글과 회원 정보가 초기화되고 강좌 글은 다시 등록됩니다.

## 테스트

```bash
mvn test
```
