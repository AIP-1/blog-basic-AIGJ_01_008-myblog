package com.example.blog.repository;

/**
 * 개인 블로그 화면의 카테고리 한 줄. id 가 {@link #UNCATEGORIZED_ID} 이면 '미분류'.
 * personal 은 그 회원이 만든 개인 카테고리인지 (아니면 공용 카테고리).
 */
public record BlogCategory(long id, String name, long postCount, boolean personal) {

    public static final long UNCATEGORIZED_ID = 0L;
}
