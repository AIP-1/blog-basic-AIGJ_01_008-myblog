---
title: 1. Java 설치와 Hello World
category: Java 입문
seq: 1
---
Java는 **한 번 작성하면 어디서나 실행되는(Write Once, Run Anywhere)** 객체지향 언어입니다. 소스 코드(`.java`)를 컴파일하면 바이트코드(`.class`)가 되고, 이것을 **JVM**이 실행합니다.

## JDK 설치

- **macOS**: `brew install openjdk@17`
- **Windows**: [Adoptium](https://adoptium.net)에서 JDK 17 이상 설치
- 설치 확인:

```bash
java -version
javac -version
```

## 첫 번째 프로그램

`Hello.java` 파일을 만들고 아래 코드를 입력하세요. **파일 이름과 public 클래스 이름이 같아야 합니다.**

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

## 컴파일과 실행

```bash
javac Hello.java   # Hello.class 생성
java Hello         # 실행 → Hello, World!
```

> Java 11부터는 `java Hello.java` 한 줄로 컴파일과 실행을 동시에 할 수 있어요.

## 정리

| 용어 | 의미 |
|---|---|
| JDK | 개발 도구(컴파일러 포함) |
| JRE | 실행 환경 |
| JVM | 바이트코드를 실행하는 가상 머신 |
| `main` | 프로그램 시작점 |
