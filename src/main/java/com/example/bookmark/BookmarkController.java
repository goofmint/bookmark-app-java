package com.example.bookmark;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BookmarkController {
    private final BookmarkRepository bookmarkRepository;
    private final BookmarkMetadataFetcher metadataFetcher;

    public BookmarkController(BookmarkRepository bookmarkRepository, BookmarkMetadataFetcher metadataFetcher) {
        this.bookmarkRepository = bookmarkRepository;
        this.metadataFetcher = metadataFetcher;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("bookmarks", bookmarkRepository.findAll());
        return "index";
    }

    @PostMapping("/bookmarks")
    public String add(@RequestParam String url, RedirectAttributes redirectAttributes) {
        BookmarkMetadata metadata;
        try {
            metadata = metadataFetcher.fetch(url);
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", "URLを確認してください。");
            redirectAttributes.addFlashAttribute("url", url);
            return "redirect:/";
        }

        try {
            bookmarkRepository.add(
                    metadata.title(),
                    metadata.url(),
                    null,
                    null,
                    metadata.ogpImageUrl()
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "ブックマークを保存できませんでした。");
            redirectAttributes.addFlashAttribute("url", url);
            return "redirect:/";
        }
        return "redirect:/";
    }

    @GetMapping("/bookmarks/{id}/edit")
    public String edit(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
        Bookmark bookmark = bookmarkRepository.findById(id);
        if (bookmark == null) {
            redirectAttributes.addFlashAttribute("error", "ブックマークが見つかりませんでした。");
            return "redirect:/";
        }
        model.addAttribute("bookmark", bookmark);
        return "edit";
    }

    @PostMapping("/bookmarks/{id}/update")
    public String update(
            @PathVariable long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String tags,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Bookmark bookmark = bookmarkRepository.findById(id);
        if (bookmark == null) {
            redirectAttributes.addFlashAttribute("error", "ブックマークが見つかりませんでした。");
            return "redirect:/";
        }

        String trimmedTitle = title == null ? "" : title.trim();
        String normalizedDescription = description == null || description.isBlank() ? null : description.trim();
        String normalizedTags = tags == null || tags.isBlank() ? null : tags.trim();

        Map<String, String> errors = new HashMap<>();
        if (trimmedTitle.isEmpty()) {
            errors.put("title", "タイトルを入力してください。");
        } else if (trimmedTitle.length() > 100) {
            errors.put("title", "タイトルは100文字以下で入力してください。");
        }
        if (normalizedDescription != null && normalizedDescription.length() > 300) {
            errors.put("description", "メモは300文字以下で入力してください。");
        }

        if (!errors.isEmpty()) {
            model.addAttribute("bookmark", new Bookmark(
                    bookmark.id(),
                    trimmedTitle,
                    bookmark.url(),
                    normalizedDescription,
                    normalizedTags,
                    bookmark.ogpImageUrl(),
                    bookmark.createdAt(),
                    bookmark.updatedAt()
            ));
            model.addAttribute("errors", errors);
            return "edit";
        }

        bookmarkRepository.update(id, trimmedTitle, normalizedDescription, normalizedTags);
        return "redirect:/";
    }

    @PostMapping("/bookmarks/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        Bookmark bookmark = bookmarkRepository.findById(id);
        if (bookmark == null) {
            redirectAttributes.addFlashAttribute("error", "ブックマークが見つかりませんでした。");
            return "redirect:/";
        }
        bookmarkRepository.delete(id);
        return "redirect:/";
    }
}
