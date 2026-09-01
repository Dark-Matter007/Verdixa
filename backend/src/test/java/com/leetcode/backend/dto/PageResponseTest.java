package com.leetcode.backend.dto;
import java.util.List; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class PageResponseTest {
 @Test void exposesFirstMiddleLastAndEmptyPages(){var first=PageResponse.from(List.of(1,2,3,4,5),0,2);assertEquals(List.of(1,2),first.content());assertEquals(3,first.totalPages());assertTrue(first.first());assertFalse(first.last());var middle=PageResponse.from(List.of(1,2,3,4,5),1,2);assertEquals(List.of(3,4),middle.content());assertFalse(middle.first());assertFalse(middle.last());var last=PageResponse.from(List.of(1,2,3,4,5),2,2);assertEquals(List.of(5),last.content());assertTrue(last.last());var empty=PageResponse.from(List.<Integer>of(),4,20);assertEquals(0,empty.totalElements());assertTrue(empty.first());assertTrue(empty.last());}
}
