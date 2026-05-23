package com.example.bookmark;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
        } catch (IllegalArgumentException ex) {
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
}
