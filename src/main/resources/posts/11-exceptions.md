---
title: 11. 예외 처리
category: INTERMEDIATE
seq: 11
---
프로그램 실행 중 발생하는 오류를 **예외(Exception)**라고 합니다. 예외를 처리하지 않으면 프로그램이 종료돼요.

## try - catch - finally

```java
try {
    int result = 10 / 0;
} catch (ArithmeticException e) {
    System.out.println("0으로 나눌 수 없습니다: " + e.getMessage());
} finally {
    System.out.println("항상 실행");
}
```

여러 예외를 한 번에 잡을 수도 있습니다.

```java
try {
    String s = null;
    s.length();
} catch (NullPointerException | IllegalArgumentException e) {
    System.out.println("오류: " + e);
}
```

## Checked vs Unchecked

| 구분 | 예 | 특징 |
|---|---|---|
| Checked | `IOException` | 반드시 처리하거나 `throws` 선언 |
| Unchecked | `NullPointerException`, `IllegalArgumentException` | `RuntimeException` 자식, 처리 강제 X |

## throws 와 throw

```java
void readFile(String path) throws IOException {   // 호출한 쪽에 처리를 넘김
    Files.readString(Path.of(path));
}

void setAge(int age) {
    if (age < 0) {
        throw new IllegalArgumentException("나이는 0 이상이어야 합니다.");
    }
}
```

## try-with-resources

파일, DB 연결처럼 **닫아야 하는 자원**은 자동으로 닫히게 쓰세요.

```java
try (BufferedReader br = new BufferedReader(new FileReader("a.txt"))) {
    System.out.println(br.readLine());
} catch (IOException e) {
    e.printStackTrace();
}
```

## 사용자 정의 예외

```java
class InsufficientBalanceException extends RuntimeException {
    InsufficientBalanceException(String message) {
        super(message);
    }
}
```
