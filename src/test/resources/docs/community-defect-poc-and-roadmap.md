# 커뮤니티 결함 대응 로그/PoC 로드맵 (추적본)

작성일: 2026-02-15

## 1. P1 정합성 추적

### 1) 댓글 API 접근 제어 정합성
- 대상: `BoardCommentService`
- 보완 항목
  - `getComments`
  - `createComment`
  - `updateComment`
  - `deleteComment`
  - `likeComment`
  - `unlikeComment`
- 적용 정책
  - `validateItemInBoard(boardId, itemId)`
  - 동네 게시판 여부 시 `validateNeighborAccess(board, member, item)`
  - `validateCommentInItem(commentId, itemId)`
- 기대 코드
  - `BOARD_014`: 이웃 미스매치
  - `BOARD_005`: 댓글-게시글 불일치
  - `BOARD_003`: 게시글-보드 불일치

### 2) 파일 API 접근 제어 정합성
- 대상: `BoardFileService`
- 보완 항목
  - `uploadFiles`
  - `getFiles`
  - `getDownloadUrl`
  - `deleteFile`
- 적용 정책
  - `validateItemInBoard(boardId, itemId)` 선검증
  - 동네 게시판 `validateNeighborAccess`
  - 파일-게시글 일치성 검증(미일치 시 `FILE_NOT_FOUND`)

### 3) 게시글 API 접근 제어 정합성
- 대상: `BoardItemService`
- 보완 항목
  - `getItem`, `updateItem`, `deleteItem`, `likeItem`, `unlikeItem`
- 적용 정책
  - `validateItemInBoard(board.getBoSeq(), itemId)` 선검증
  - 읽기/작성/수정/삭제 권한 + 동네 게시판 조건 일관 처리

## 2. P2 표시 정합성
- 대상: `childcare-assistant-frontend/src/pages/PostDetailPage.jsx`
- 반영 포인트
  - `isAuthor` / `isOwnedComment`에서 `user.role === 'ADMIN'` fallback 반영
  - 세션 변경 시 쿼리 무효화 키 동기화

## 3. P3 문서화/운영
- 결함/재현 로그
  - `BOARD_003`, `BOARD_014`, `BOARD_005` 시나리오별 재현 경로와 기대 응답
- 수동 검증 순서
  - `/boards/{id}/items/{itemId}/comments`(이웃 mismatch)
  - `/boards/{id}/items/{itemId}/comments/{commentId}/like`(댓글 mismatch)
  - `/boards/{id}/items/{itemId}/like`(item-board mismatch)
  - slug 기반 조회(`/boards/{slug}/items/{itemId}`) 대체 조회 경로 확인

## 4. 타입 검증/환경 정합 실행 로그 (2026-02-15 기준)

### A. 환경 정합 체크
- `java -version`: `openjdk version "17.0.16"` 확인
- `./gradlew -version` (프로젝트 기본 실행)
  - 실패: `~/.gradle/wrapper/dists/.../gradle-8.14-bin.zip.lck` 접근 불가 (`Operation not permitted`)
- `GRADLE_USER_HOME=/tmp/gradle`로 재실행
  - 실패: `services.gradle.org` UnknownHost (네트워크 제한)
- `./gradlew clean --no-daemon` (GRADLE_USER_HOME=/tmp/gradle)
  - 동일하게 Gradle 분배판 다운로드 실패(UnknownHost)

### B. 타입 검증 상태
- `BoardCommentService`는 Lombok DTO/Entity가 `@Data/@Builder`를 사용하고 있어, 진단 메시지상 보이는 다수 `getXxx`/`setXxx`/`builder()` 누락은 IDE/LSP의 Lombok 처리기 미작동 징후로 판단됨.
- 수동 소스 점검 결과:
  - `Board`, `BoardItem`, `Board`, `BoardComment`, `BoardCommentLike`, `Member`, `BoardCommentRequest`, `BoardCommentDto`, `BoardCommentListDto`의 접근자/빌더 메서드 시그니처가 서비스 호출부와 구조적으로 일치.
  - 즉시 보이는 코드 레벨 API 누락은 확인되지 않음.

### C. 분류(예정)
- 현재 에러는 **1차 분류: 환경/분석기 계열(컴파일러/JDK/Lombok)**로 표기
- `./gradlew` 기반 실제 컴파일(compileJava/compileTestJava/POC 테스트)은 현재 네트워크·캐시 경로 제약 해소 전까지 보류

### D. 로컬 실행 우회 검증 결과(현재 세션)
- `.../gradle-8.14/bin/gradle --version`: Java 17 기반 실행 확인
- 직접 Gradle 실행으로 `compileJava` 시도
  - 실패: `FileLockContentionHandler` 초기화 단계에서 `java.net.SocketException: Operation not permitted`
  - 원인: 샌드박스가 내부 소켓/락 통신을 제한
- 결론: 실제 타입 검증은 현재 세션 제약 때문에 미완료. IDE/LSP 정합성 복구 + 네트워크/소켓 제약 없는 환경에서 재실행 필요
