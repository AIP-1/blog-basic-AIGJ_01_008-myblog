---
title: 5. 배열
category: Java 입문
seq: 5
---
배열은 **같은 타입의 값 여러 개**를 하나로 묶은 것입니다. 크기는 만들 때 정해지고 바꿀 수 없어요.

## 선언과 생성

```java
int[] scores = new int[3];   // [0, 0, 0]
scores[0] = 90;
scores[1] = 80;

int[] nums = {1, 2, 3, 4, 5}; // 선언과 동시에 초기화
System.out.println(nums.length); // 5
```

> 인덱스는 **0부터** 시작합니다. 범위를 벗어나면 `ArrayIndexOutOfBoundsException`이 발생해요.

## 배열 순회

```java
int sum = 0;
for (int n : nums) {
    sum += n;
}
System.out.println("합계: " + sum); // 15
```

## Arrays 유틸리티

```java
import java.util.Arrays;

int[] arr = {5, 3, 1, 4};
Arrays.sort(arr);                          // 정렬
System.out.println(Arrays.toString(arr));  // [1, 3, 4, 5]
```

## 2차원 배열

```java
int[][] matrix = {
    {1, 2, 3},
    {4, 5, 6}
};
System.out.println(matrix[1][2]); // 6
```
