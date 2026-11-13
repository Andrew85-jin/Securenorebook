package model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Note {
    private final UUID id;
    private String title;
    private String content;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Note newNote(String title, String content){
        LocalDateTime now = LocalDateTime.now();
        return new Note(UUID.randomUUID(), title, content, now, now);
    }

    public Note(UUID id, String title, String content, LocalDateTime createdAt, LocalDateTime updatedAt){
        this.id = Objects.requireNonNull(id, "id не може бути null");
        this.title = Objects.requireNonNull(title, "title не може бути null");
        this.content = Objects.requireNonNull(content, "content не може бути null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt не може бути null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt не може бути null");

        if(updatedAt.isBefore(createdAt)){
            throw new IllegalArgumentException("updated не може бути раніше за createdAt");
        }
    }

    public void edit (String title, String content){
        this.title = Objects.requireNonNull(title, "title не може бути null");
        this.content = Objects.requireNonNull(content, "content не може бути null");
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
