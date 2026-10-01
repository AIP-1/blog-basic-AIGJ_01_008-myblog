---
title: 3. 연산자와 조건문
category: BASIC
seq: 3
---
## 주요 연산자

| 종류 | 연산자 |
|---|---|
| 산술 | `+ - * / %` |
| 비교 | `== != > < >= <=` |
| 논리 | `&&`(그리고) `\|\|`(또는) `!`(부정) |
| 증감 | `++ --` |
| 삼항 | `조건 ? 참일때 : 거짓일때` |

```java
int a = 7, b = 2;
System.out.println(a / b);  // 3 (정수 나눗셈)
System.out.println(a % b);  // 1 (나머지)
String result = a > b ? "a가 큼" : "b가 큼";
```

> 문자열 비교는 `==`가 아니라 **`equals()`**를 써야 합니다. `"java".equals(str)`

## if 문

```java
int score = 85;

if (score >= 90) {
    System.out.println("A");
} else if (score >= 80) {
    System.out.println("B");
} else {
    System.out.println("C");
}
```

## switch 문

Java 14부터는 화살표(`->`) 문법으로 깔끔하게 쓸 수 있어요.

```java
String day = "SAT";

String type = switch (day) {
    case "SAT", "SUN" -> "주말";
    default -> "평일";
};
System.out.println(type); // 주말
```
