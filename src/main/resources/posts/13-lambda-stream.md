---
title: 13. 람다와 스트림
category: INTERMEDIATE
seq: 13
---
## 람다식

메서드를 **하나의 식**으로 간단히 표현한 것입니다. 메서드가 하나뿐인 인터페이스(함수형 인터페이스) 자리에 쓸 수 있어요.

```java
// 기존 방식 (익명 클래스)
Comparator<String> c1 = new Comparator<>() {
    @Override
    public int compare(String a, String b) {
        return a.length() - b.length();
    }
};

// 람다
Comparator<String> c2 = (a, b) -> a.length() - b.length();
```

자주 쓰는 함수형 인터페이스:

| 인터페이스 | 형태 | 예 |
|---|---|---|
| `Function<T,R>` | T → R | `s -> s.length()` |
| `Predicate<T>` | T → boolean | `n -> n > 0` |
| `Consumer<T>` | T → void | `s -> System.out.println(s)` |
| `Supplier<T>` | () → T | `() -> new ArrayList<>()` |

## 메서드 참조

```java
list.forEach(System.out::println);   // s -> System.out.println(s)
```

## 스트림 API

컬렉션 데이터를 **선언적으로** 가공합니다. `생성 → 중간 연산 → 최종 연산` 순서예요.

```java
List<String> names = List.of("kim", "lee", "park", "choi", "jung");

List<String> result = names.stream()
        .filter(n -> n.length() >= 4)   // 4글자 이상
        .map(String::toUpperCase)       // 대문자로
        .sorted()                       // 정렬
        .toList();                      // 리스트로 수집 (Java 16+)

System.out.println(result); // [CHOI, JUNG, PARK]
```

## 자주 쓰는 연산

```java
List<Integer> nums = List.of(1, 2, 3, 4, 5);

int sum = nums.stream().mapToInt(Integer::intValue).sum();       // 15
double avg = nums.stream().mapToInt(i -> i).average().orElse(0); // 3.0
boolean hasEven = nums.stream().anyMatch(n -> n % 2 == 0);        // true
long count = nums.stream().filter(n -> n > 2).count();            // 3
```

## 그룹핑

```java
import java.util.stream.Collectors;

Map<Integer, List<String>> byLength = names.stream()
        .collect(Collectors.groupingBy(String::length));
// {3=[kim, lee], 4=[park, choi, jung]}
```

> 스트림은 **원본 데이터를 바꾸지 않으며**, 한 번 사용하면 재사용할 수 없습니다.
