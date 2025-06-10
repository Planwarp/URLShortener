package ru.URLShortener.URLShortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.URLShortener.URLShortener.entity.Link;
import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByLongUrl(String longUrl);
    Optional<Link> findByShortUrl(String shortUrl);
}