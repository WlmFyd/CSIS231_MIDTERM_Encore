package com.example.midterm_cinema.model;

import jakarta.persistence.*;

@Entity
@Table(name = "movies")
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String synopsis;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false, length = 80)
    private String genre;

    @Column(name = "poster_path", length = 500)
    private String posterPath;

    protected Movie() {} // Required by Hibernate.

    public Movie(
            String title, String synopsis, int durationMinutes, String genre, String posterPath) {
        updateDetails(title, synopsis, durationMinutes, genre);
        this.posterPath = posterPath;
    }

    public void updateDetails(String title, String synopsis, int durationMinutes, String genre) {
        if (title == null || title.isBlank() || title.trim().length() > 150) {
            throw new IllegalArgumentException("Enter a movie title of 1–150 characters.");
        }
        if (synopsis == null || synopsis.isBlank() || synopsis.trim().length() > 2000) {
            throw new IllegalArgumentException("Enter a synopsis of 1–2000 characters.");
        }
        if (durationMinutes <= 0 || durationMinutes > 600) {
            throw new IllegalArgumentException("Duration must be between 1 and 600 minutes.");
        }
        if (genre == null || genre.isBlank() || genre.trim().length() > 80) {
            throw new IllegalArgumentException("Enter a genre of 1–80 characters.");
        }
        this.title = title.trim();
        this.synopsis = synopsis.trim();
        this.durationMinutes = durationMinutes;
        this.genre = genre.trim();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getGenre() {
        return genre;
    }

    public String getPosterPath() {
        return posterPath;
    }
}
