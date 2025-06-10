package ru.URLShortener.URLShortener.controller;

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

    public LinksController(LinkRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody Link link) {
        System.out.println("Пришёл longUrl: " + link.getLongUrl());
        if (link.getLongUrl() == null || link.getLongUrl().isEmpty()) {
            return new ResponseEntity<>("Ошибка: longUrl пустой или null", HttpStatus.BAD_REQUEST);
        }

        Optional<Link> existing = repo.findByLongUrl(link.getLongUrl());
        if (existing.isPresent()) {
            System.out.println("Нашёл старую: " + existing.get().getShortUrl());
            return new ResponseEntity<>(existing.get(), HttpStatus.CREATED);
        }

        link.setCreatedAt(Instant.now());
        link.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES)); // 10 минут
        link.setShortUrl(makeShortUrl());
        Link saved = repo.save(link);
        System.out.println("Сохранили: " + saved.getShortUrl());
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/api/{shortUrl}")
    public ResponseEntity<?> getLongUrl(@PathVariable String shortUrl) {
        System.out.println("Ищем shortUrl: " + shortUrl);
        Optional<Link> link = repo.findByShortUrl(shortUrl);
        if (link.isEmpty()) {
            return new ResponseEntity<>("Ошибка: ссылка не найдена", HttpStatus.NOT_FOUND);
        }
        // Проверяем TTL
        if (Instant.now().isAfter(link.get().getExpiresAt())) {
            System.out.println("Ссылка " + shortUrl + " просрочена!");
            return new ResponseEntity<>("Ошибка: ссылка истекла", HttpStatus.GONE);
        }
        return new ResponseEntity<>(link.get(), HttpStatus.OK);
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> redirectToLongUrl(@PathVariable String shortUrl) {
        System.out.println("Редирект по shortUrl: " + shortUrl);
        Optional<Link> link = repo.findByShortUrl(shortUrl);
        if (link.isEmpty()) {
            return new ResponseEntity<>("Ошибка: ссылка не найдена", HttpStatus.NOT_FOUND);
        }

        if (Instant.now().isAfter(link.get().getExpiresAt())) {
            System.out.println("Ссылка " + shortUrl + " просрочена!");
            return new ResponseEntity<>("Ошибка: ссылка истекла", HttpStatus.GONE);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", link.get().getLongUrl())
                .build();
    }

    private String makeShortUrl() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; // символы
        Random rand = new Random();
        StringBuilder shorty = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            shorty.append(chars.charAt(rand.nextInt(chars.length())));
        }

        while (repo.findByShortUrl(shorty.toString()).isPresent()) {
            shorty = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                shorty.append(chars.charAt(rand.nextInt(chars.length())));
            }
        }
        return shorty.toString();
    }
}