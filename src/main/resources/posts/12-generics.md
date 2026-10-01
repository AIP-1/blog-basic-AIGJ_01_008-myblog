---
title: 12. 제네릭
category: INTERMEDIATE
seq: 12
---
제네릭은 **타입을 매개변수처럼** 다루는 기능입니다. 컴파일 시점에 타입을 검사해서 형 변환 실수를 막아줘요.

## 제네릭이 없다면?

```java
List list = new ArrayList();
list.add("hello");
Integer n = (Integer) list.get(0); // 실행 중 ClassCastException!
```

```java
List<String> list = new ArrayList<>();
list.add("hello");
// list.add(1);  → 컴파일 에러로 미리 발견
String s = list.get(0);  // 형 변환 불필요
```

## 제네릭 클래스 만들기

```java
public class Box<T> {
    private T item;

    public void put(T item) { this.item = item; }
    public T get() { return item; }
}

Box<String> box = new Box<>();
box.put("사과");
String fruit = box.get();
```

## 제네릭 메서드

```java
static <T> T first(List<T> list) {
    return list.get(0);
}

String a = first(List.of("x", "y"));   // String
Integer b = first(List.of(1, 2));      // Integer
```

## 타입 제한 (bounded type)

```java
static <T extends Number> double sum(List<T> nums) {
    double total = 0;
    for (T n : nums) total += n.doubleValue();
    return total;
}
```

## 와일드카드

| 표기 | 의미 |
|---|---|
| `<?>` | 아무 타입 |
| `<? extends Number>` | Number 또는 그 자식 (읽기용) |
| `<? super Integer>` | Integer 또는 그 부모 (쓰기용) |
