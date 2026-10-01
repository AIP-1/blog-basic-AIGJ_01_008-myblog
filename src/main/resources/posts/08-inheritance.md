---
title: 8. 상속과 다형성
category: INTERMEDIATE
seq: 8
---
## 상속 (extends)

부모 클래스의 필드와 메서드를 자식 클래스가 물려받습니다.

```java
class Animal {
    protected String name;

    Animal(String name) {
        this.name = name;
    }

    void sound() {
        System.out.println("...");
    }
}

class Dog extends Animal {
    Dog(String name) {
        super(name); // 부모 생성자 호출
    }

    @Override
    void sound() {
        System.out.println(name + ": 멍멍");
    }
}
```

- Java는 **단일 상속**만 지원합니다. (부모는 하나)
- `@Override`는 부모 메서드를 재정의한다는 표시로, 오타를 컴파일러가 잡아줘요.

## 다형성

**부모 타입 변수로 자식 객체를 다룰 수 있는 것**이 다형성입니다.

```java
class Cat extends Animal {
    Cat(String name) { super(name); }

    @Override
    void sound() {
        System.out.println(name + ": 야옹");
    }
}

Animal[] animals = { new Dog("바둑이"), new Cat("나비") };
for (Animal a : animals) {
    a.sound(); // 실제 객체의 메서드가 호출됨
}
// 바둑이: 멍멍
// 나비: 야옹
```

## instanceof 와 형 변환

```java
Animal a = new Dog("바둑이");
if (a instanceof Dog dog) {   // Java 16+ 패턴 매칭
    dog.sound();
}
```

## Object 클래스

모든 클래스는 자동으로 `Object`를 상속합니다. 자주 재정의하는 메서드는 `toString()`, `equals()`, `hashCode()`예요.
