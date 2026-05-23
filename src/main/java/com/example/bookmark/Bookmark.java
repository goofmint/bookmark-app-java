package com.example.bookmark;

public record Bookmark(
        long id,
        String title,
        String url,
        String description,
        String tags,
        String ogpImageUrl,
        String createdAt,
        String updatedAt
) {
}
