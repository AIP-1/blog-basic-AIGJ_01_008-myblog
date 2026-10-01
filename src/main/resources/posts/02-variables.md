---
title: 2. 변수와 자료형
category: BASIC
seq: 2
---
변수는 값을 저장하는 이름 붙은 공간입니다. Java는 **타입을 반드시 지정**하는 언어예요.

## 기본형 (Primitive Type)

| 타입 | 크기 | 예시 |
|---|---|---|
| `int` | 4byte | `int age = 20;` |
| `long` | 8byte | `long big = 10_000_000_000L;` |
| `double` | 8byte | `double pi = 3.14;` |
| `boolean` | 1bit | `boolean ok = true;` |
| `char` | 2byte | `char c = 'A';` |

## 참조형 (Reference Type)

기본형이 아닌 모든 타입은 참조형입니다. 대표적으로 `String`이 있어요.

```java
String name = "홍길동";
System.out.println(name.length());   // 3
System.out.println("이름: " + name); // 문자열 연결
```

## var (Java 10+)

오른쪽 값으로 타입을 추론할 수 있으면 `var`를 쓸 수 있습니다.

```java
var count = 10;        // int
var message = "안녕";  // String
```

## 상수

`final`을 붙이면 값을 바꿀 수 없습니다. 이름은 대문자로 쓰는 게 관례예요.

```java
final int MAX_SIZE = 100;
```

## 형 변환

```java
int i = 10;
double d = i;          // 자동 변환 (작은 → 큰)
int j = (int) 3.99;    // 강제 변환 → 3 (소수점 버림)
int n = Integer.parseInt("123");    // 문자열 → 숫자
String s = String.valueOf(123);     // 숫자 → 문자열
```
