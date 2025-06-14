package ru.URLShortener.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.URLShortener.entity.Link;
import ru.URLShortener.service.LinkService;
import java.util.Optional;

@RestController
public class LinksController {

    private final LinkService linkService;

    public LinksController(LinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody Link link) {
        try {
            Link saved = linkService.shortenUrl(link);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/api/{shortUrl}")
    public ResponseEntity<?> getLinkInfo(@PathVariable String shortUrl) {
        Optional<Link> link = linkService.findByShortUrl(shortUrl);
        if (link.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Ссылка не найдена"));
        }
        if (linkService.isExpired(link.get())) {
            return ResponseEntity.status(HttpStatus.GONE).body(new ErrorResponse("Ссылка истекла"));
        }
        return ResponseEntity.ok(link.get());
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode) {
        if (!shortCode.matches("[a-zA-Z0-9]{6}(\\.ru)?")) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Неправильный формат ссылки"));
        }

        Optional<Link> link = linkService.findByShortUrl(shortCode);
        if (link.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Ссылка не найдена"));
        }
        if (linkService.isExpired(link.get())) {
            return ResponseEntity.status(HttpStatus.GONE).body(new ErrorResponse("Ссылка истекла"));
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", link.get().getLongUrl())
                .build();
    }

    static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}