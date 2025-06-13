package ru.URLShortener.URLShortener.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.URLShortener.URLShortener.entity.Link;
import ru.URLShortener.URLShortener.repository.LinkRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

@RestController
public class LinksController {
    private final LinkRepository repo;

    @Value("${app.short-domain:ru}")
    private String shortDomain;

    public LinksController(LinkRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody Link link) {
        if (link.getLongUrl() == null || link.getLongUrl().isEmpty()) {
            return ResponseEntity.badRequest().body("Ошибка: URL не может быть пустым");
        }

        Optional<Link> existing = repo.findByLongUrl(link.getLongUrl());
        if (existing.isPresent()) {
            return ResponseEntity.ok(existing.get());
        }

        String shortCode = generateShortCode();
        link.setShortUrl(shortCode + "." + shortDomain);
        link.setCreatedAt(Instant.now());
        link.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        Link saved = repo.save(link);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/api/{shortUrl}")
    public ResponseEntity<?> getLinkInfo(@PathVariable String shortUrl) {
        if (!shortUrl.contains(".")) {
            shortUrl = shortUrl + "." + shortDomain;
        }

        Optional<Link> link = repo.findByShortUrl(shortUrl);
        if (link.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ссылка не найдена");
        }

        if (Instant.now().isAfter(link.get().getExpiresAt())) {
            return ResponseEntity.status(HttpStatus.GONE).body("Ссылка истекла");
        }

        return ResponseEntity.ok(link.get());
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode) {
        String shortUrl = shortCode.contains(".") ? shortCode : shortCode + "." + shortDomain;

        Optional<Link> link = repo.findByShortUrl(shortUrl);
        if (link.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ссылка не найдена");
        }

        if (Instant.now().isAfter(link.get().getExpiresAt())) {
            return ResponseEntity.status(HttpStatus.GONE).body("Ссылка истекла");
        }

        return ResponseEntity.status(HttpStatus.FOUND).header("Location", link.get().getLongUrl()).build();
    }

    private String generateShortCode() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rand = new Random();
        String code;

        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(rand.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (repo.findByShortUrl(code + "." + shortDomain).isPresent());

        return code;
    }
}