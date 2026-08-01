package com.trt.contentengagement.content.application;

import java.util.UUID;

public class ContentNotFoundException extends RuntimeException {

    public ContentNotFoundException(UUID contentId) {
        super("Content was not found: " + contentId);
    }
}
