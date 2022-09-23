# Mission2

# 구성
- aop
  - AOP로 중복 거래 방지 Lock을 걸 때 사용될 Annotation 등.
- config
  - Redis 관련 설정 및 클라이언트 빈 등록, JPA 관련 설정 등록
- controller
  - API의 endpoint를 등록하고, 요청/응답의 형식을 갖는 클래스 패키지
- domain
  - JPA Entity
- dto
  - Controller에서 요청/응답에 사용할 클래스
  - 로직 내부에서 데이터 전송에 사용할 클래스
- exception
  - 커스텀 Exception, Exception Handler 클래스 패키지
- repository
  - Repository(DB에 연결할 때 사용하는 인터페이스)가 위치하는 패키지
- service
  - 비즈니스 로직을 담는 서비스 클래스 패키지
- type
  - 상태타입, 에러코드, 거래종류 등의 다양한 enum class들의 패키지

# 사용한 기능과 정보
- IntelliJ IDE, Gradle
- Spring boot 2.7.3, JAVA (JDK 11)
- Junit5
- H2 Database
- Spring data jpa
- Embedded redis
- Mockito
- Lombok

# API
- 계좌 API
  - 계좌 생성 
    - 랜덤한 10자리 계좌 번호 생성, 계좌 번호 중복 체크 기능
  - 계좌 해지
  - 계좌 확인
    - 계좌 번호와 잔액 정보를 응답
- 거래 API
  - 잔액 사용
    - 중복 거래 방지 기능
  - 잔액 사용 취소
    - 중복 취소 방지 기능
  - 거래 확인
