package ru.URLShortener.URLShortener.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Value("${app.short-domain}")
    private String shortDomain;

    public LinksController(LinkRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody Link link) {
        if (link.getLongUrl() == null || link.getLongUrl().isEmpty()) {
            return new ResponseEntity<>("Ошибка: longUrl пустой или null", HttpStatus.BAD_REQUEST);
        }

        Optional<Link> existing = repo.findByLongUrl(link.getLongUrl());
        if (existing.isPresent()) {
            return new ResponseEntity<>(existing.get(), HttpStatus.CREATED);
        }

        String shortCode = makeShortUrl();
        link.setShortUrl(shortCode + "." + shortDomain);
        link.setCreatedAt(Instant.now());
        link.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        Link saved = repo.save(link);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/api/{shortUrl}")
    public ResponseEntity<?> getLongUrl(@PathVariable String shortUrl) {
        Optional<Link> link = repo.findByShortUrl(shortUrl);
        if (link.isEmpty()) {
            return new ResponseEntity<>("Ошибка: ссылка не найдена", HttpStatus.NOT_FOUND);
        }
        if (Instant.now().isAfter(link.get().getExpiresAt())) {
            return new ResponseEntity<>("Ошибка: ссылка истекла", HttpStatus.GONE);
        }
        return new ResponseEntity<>(link.get(), HttpStatus.OK);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirectToLongUrl(@PathVariable String shortCode) {
        String shortUrl = shortCode + "." + shortDomain;
        Optional<Link> link = repo.findByShortUrl(shortUrl);

        if (link.isEmpty()) {
            return new ResponseEntity<>("Ошибка: ссылка не найдена", HttpStatus.NOT_FOUND);
        }
        if (Instant.now().isAfter(link.get().getExpiresAt())) {
            return new ResponseEntity<>("Ошибка: ссылка истекла", HttpStatus.GONE);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", link.get().getLongUrl())
                .build();
    }

    private String makeShortUrl() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rand = new Random();
        StringBuilder shorty = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            shorty.append(chars.charAt(rand.nextInt(chars.length())));
        }

        // Проверяем уникальность
        while (repo.findByShortUrl(shorty.toString() + "." + shortDomain).isPresent()) {
            shorty = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                shorty.append(chars.charAt(rand.nextInt(chars.length())));
            }
        }
        return shorty.toString();
    }
}

