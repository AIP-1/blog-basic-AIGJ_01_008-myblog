---
title: 6. 메서드
category: BASIC
seq: 6
---
메서드는 **특정 작업을 수행하는 코드 묶음**입니다. 같은 코드를 반복하지 않고 재사용할 수 있어요.

## 기본 형태

```java
// 반환타입 메서드이름(매개변수) { ... }
static int add(int a, int b) {
    return a + b;
}

public static void main(String[] args) {
    int result = add(3, 5);
    System.out.println(result); // 8
}
```

- 반환값이 없으면 반환 타입에 `void`를 씁니다.
- `static` 메서드는 객체 없이 바로 호출할 수 있어요. (클래스 편에서 자세히)

```java
static void greet(String name) {
    System.out.println("안녕하세요, " + name + "님");
}
```

## 오버로딩

이름이 같아도 **매개변수가 다르면** 여러 개 만들 수 있습니다.

```java
static int add(int a, int b) { return a + b; }
static double add(double a, double b) { return a + b; }
static int add(int a, int b, int c) { return a + b + c; }
```

## 가변 인자

```java
static int sum(int... numbers) {
    int total = 0;
    for (int n : numbers) total += n;
    return total;
}

sum(1, 2, 3);       // 6
sum(1, 2, 3, 4, 5); // 15
```
