---
title: 7. 클래스와 객체
category: Java 입문
seq: 7
---
**클래스**는 설계도, **객체(인스턴스)**는 설계도로 만든 실체입니다.

## 클래스 정의

```java
public class Person {
    // 필드 (상태)
    private String name;
    private int age;

    // 생성자
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 메서드 (행동)
    public void introduce() {
        System.out.println("저는 " + name + ", " + age + "살입니다.");
    }

    // getter
    public String getName() {
        return name;
    }
}
```

## 객체 생성과 사용

```java
Person p = new Person("김자바", 25);
p.introduce();                  // 저는 김자바, 25살입니다.
System.out.println(p.getName()); // 김자바
```

## 접근 제어자

| 제어자 | 접근 범위 |
|---|---|
| `public` | 어디서나 |
| `protected` | 같은 패키지 + 자식 클래스 |
| (default) | 같은 패키지 |
| `private` | 클래스 내부만 |

필드는 `private`으로 숨기고 메서드로만 접근하게 하는 것을 **캡슐화**라고 합니다.

## static

`static` 멤버는 객체가 아니라 **클래스에 하나만** 존재합니다.

```java
public class Counter {
    static int count = 0;
    Counter() { count++; }
}

new Counter();
new Counter();
System.out.println(Counter.count); // 2
```

## record (Java 16+)

데이터만 담는 클래스는 `record`로 짧게 만들 수 있어요. 생성자, getter, `equals`, `toString`이 자동 생성됩니다.

```java
public record Point(int x, int y) {}

Point pt = new Point(1, 2);
System.out.println(pt.x()); // 1
```
