---
title: 10. 컬렉션 (List, Set, Map)
category: Java 중급
seq: 10
---
배열은 크기가 고정이지만, **컬렉션**은 크기가 자유롭게 늘어나고 다양한 기능을 제공합니다.

## List — 순서 O, 중복 O

```java
import java.util.*;

List<String> list = new ArrayList<>();
list.add("자바");
list.add("스프링");
list.add("자바");

System.out.println(list.get(0));    // 자바
System.out.println(list.size());    // 3
list.remove("스프링");
System.out.println(list);           // [자바, 자바]
```

## Set — 순서 X, 중복 X

```java
Set<String> set = new HashSet<>();
set.add("A");
set.add("B");
set.add("A");                        // 무시됨
System.out.println(set.size());      // 2
System.out.println(set.contains("B")); // true
```

## Map — 키-값 쌍

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("철수", 90);
scores.put("영희", 85);

System.out.println(scores.get("철수"));              // 90
System.out.println(scores.getOrDefault("민수", 0));  // 0

for (Map.Entry<String, Integer> e : scores.entrySet()) {
    System.out.println(e.getKey() + " = " + e.getValue());
}
```

## 불변 컬렉션 만들기 (Java 9+)

```java
List<Integer> nums = List.of(1, 2, 3);
Map<String, Integer> m = Map.of("a", 1, "b", 2);
// nums.add(4); → UnsupportedOperationException
```

## 정리

| 인터페이스 | 대표 구현 | 특징 |
|---|---|---|
| `List` | `ArrayList` | 인덱스 접근 빠름 |
| `Set` | `HashSet`, `TreeSet` | 중복 제거, TreeSet은 정렬 |
| `Map` | `HashMap`, `TreeMap` | 키로 빠른 조회 |

> 변수 타입은 `ArrayList`가 아니라 **인터페이스 `List`로** 선언하는 습관을 들이세요.
