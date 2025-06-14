package ru.URLShortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.URLShortener.entity.Link;
import ru.URLShortener.repository.LinkRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

@Service
public class LinkService {

    private final LinkRepository repo;

    @Value("${app.short-domain:ru}")
    private String shortDomain;

    public LinkService(LinkRepository repo) {
        this.repo = repo;
    }

    public Link shortenUrl(Link link) {
        System.out.println("В сервисе URL: " + link.getLongUrl()); // отладка
        if (link.getLongUrl() == null || link.getLongUrl().isEmpty()) {
            throw new IllegalArgumentException("URL не может быть пустым");
        }

        Optional<Link> existing = repo.findByLongUrl(link.getLongUrl());
        if (existing.isPresent()) {
            System.out.println("Нашёл старую: " + existing.get().getShortUrl());
            return existing.get();
        }

        String shortCode = generateShortCode();
        link.setShortUrl(shortCode + "." + shortDomain);
        link.setCreatedAt(Instant.now());
        link.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        Link saved = repo.save(link);
        System.out.println("Сохранили: " + saved.getShortUrl());
        return saved;
    }

    public Optional<Link> findByShortUrl(String shortUrl) {
        System.out.println("Ищем в сервисе: " + shortUrl);
        if (!shortUrl.contains(".")) {
            shortUrl = shortUrl + "." + shortDomain;
        }
        return repo.findByShortUrl(shortUrl);
    }

    public boolean isExpired(Link link) {
        boolean expired = Instant.now().isAfter(link.getExpiresAt());
        if (expired) {
            System.out.println("Ссылка " + link.getShortUrl() + " просрочена!");
        }
        return expired;
    }

    // Генерим короткий код
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