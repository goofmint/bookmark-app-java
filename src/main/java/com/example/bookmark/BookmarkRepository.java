package com.example.bookmark;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookmarkRepository {
    private final JdbcTemplate jdbcTemplate;

    public BookmarkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Bookmark> findAll() {
        return jdbcTemplate.query("""
                SELECT id, title, url, description, tags, ogp_image_url, created_at, updated_at
                FROM bookmarks
                ORDER BY id DESC
                """, (rs, rowNum) -> new Bookmark(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("url"),
                rs.getString("description"),
                rs.getString("tags"),
                rs.getString("ogp_image_url"),
                rs.getString("created_at"),
                rs.getString("updated_at")
        ));
    }

    public void add(String title, String url, String description, String tags, String ogpImageUrl) {
        jdbcTemplate.update("""
                INSERT INTO bookmarks (title, url, description, tags, ogp_image_url)
                VALUES (?, ?, ?, ?, ?)
                """, title, url, description, tags, ogpImageUrl);
    }

    public Bookmark findById(long id) {
        var bookmarks = jdbcTemplate.query("""
                SELECT id, title, url, description, tags, ogp_image_url, created_at, updated_at
                FROM bookmarks
                WHERE id = ?
                """, (rs, rowNum) -> new Bookmark(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("url"),
                rs.getString("description"),
                rs.getString("tags"),
                rs.getString("ogp_image_url"),
                rs.getString("created_at"),
                rs.getString("updated_at")
        ), id);
        return bookmarks.isEmpty() ? null : bookmarks.get(0);
    }

    public void update(long id, String title, String description, String tags) {
        jdbcTemplate.update("""
                UPDATE bookmarks
                SET title = ?, description = ?, tags = ?, updated_at = datetime('now')
                WHERE id = ?
                """, title, description, tags, id);
    }

    public void delete(long id) {
        jdbcTemplate.update("""
                DELETE FROM bookmarks
                WHERE id = ?
                """, id);
    }
}
