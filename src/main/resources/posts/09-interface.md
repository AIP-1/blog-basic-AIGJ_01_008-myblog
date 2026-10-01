---
title: 9. 인터페이스와 추상 클래스
category: Java 중급
seq: 9
---
## 추상 클래스

일부 메서드를 구현 없이 선언만 해두고, 자식이 반드시 구현하도록 강제합니다. **직접 객체를 만들 수 없어요.**

```java
abstract class Shape {
    abstract double area(); // 구현 없음

    void print() {
        System.out.println("넓이: " + area());
    }
}

class Circle extends Shape {
    private final double r;
    Circle(double r) { this.r = r; }

    @Override
    double area() { return Math.PI * r * r; }
}

new Circle(2).print(); // 넓이: 12.566...
```

## 인터페이스

**"무엇을 할 수 있는가"**라는 약속(규약)입니다. 클래스는 여러 인터페이스를 동시에 구현할 수 있어요.

```java
interface Flyable {
    void fly();
}

interface Swimmable {
    void swim();

    default void rest() {          // 기본 구현 제공 가능 (Java 8+)
        System.out.println("쉬는 중");
    }
}

class Duck implements Flyable, Swimmable {
    public void fly()  { System.out.println("날아요"); }
    public void swim() { System.out.println("헤엄쳐요"); }
}
```

## 언제 무엇을 쓰나?

| 구분 | 추상 클래스 | 인터페이스 |
|---|---|---|
| 상속 개수 | 하나만 | 여러 개 |
| 필드 | 가질 수 있음 | 상수만 |
| 용도 | 공통 코드 공유 (is-a) | 기능 규약 정의 (can-do) |

> 실무에서는 **인터페이스를 먼저 고려**하는 것이 일반적입니다. `List`, `Comparable`, `Runnable` 등이 모두 인터페이스예요.
