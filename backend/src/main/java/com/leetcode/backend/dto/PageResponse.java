package com.leetcode.backend.dto;

import java.util.List;

/** Stable REST pagination envelope for management and activity views. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements,
                              int totalPages, boolean first, boolean last) {
    public static <T> PageResponse<T> from(List<T> values, int requestedPage, int requestedSize) {
        int size = Math.max(1, Math.min(requestedSize, 100));
        int totalPages = values.isEmpty() ? 0 : (int) Math.ceil(values.size() / (double) size);
        int page = totalPages == 0 ? 0 : Math.max(0, Math.min(requestedPage, totalPages - 1));
        int start = Math.min(page * size, values.size());
        int end = Math.min(start + size, values.size());
        return new PageResponse<>(values.subList(start, end), page, size, values.size(), totalPages,
                page == 0, totalPages == 0 || page == totalPages - 1);
    }
}
