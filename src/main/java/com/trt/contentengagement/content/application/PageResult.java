package com.trt.contentengagement.content.application;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {

    public PageResult {
        items = List.copyOf(items);
    }
}
