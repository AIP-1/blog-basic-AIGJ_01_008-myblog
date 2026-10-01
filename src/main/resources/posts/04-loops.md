---
title: 4. 반복문
category: BASIC
seq: 4
---
## for 문

반복 횟수가 정해져 있을 때 씁니다.

```java
for (int i = 1; i <= 5; i++) {
    System.out.println(i);
}
```

## 향상된 for 문 (for-each)

배열이나 리스트의 모든 요소를 순서대로 꺼낼 때 편합니다.

```java
String[] fruits = {"사과", "바나나", "포도"};
for (String fruit : fruits) {
    System.out.println(fruit);
}
```

## while 문

조건이 참인 동안 반복합니다.

```java
int n = 3;
while (n > 0) {
    System.out.println(n);
    n--;
}
```

`do-while`은 **최소 한 번은 실행**됩니다.

```java
do {
    System.out.println("한 번은 실행");
} while (false);
```

## break 와 continue

```java
for (int i = 1; i <= 10; i++) {
    if (i % 2 == 0) continue; // 짝수는 건너뛰기
    if (i > 7) break;         // 7보다 크면 반복 종료
    System.out.println(i);    // 1 3 5 7
}
```

## 예제: 구구단 2단

```java
for (int i = 1; i <= 9; i++) {
    System.out.println("2 x " + i + " = " + (2 * i));
}
```
