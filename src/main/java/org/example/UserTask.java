package org.example;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserTask {
    private final String id;
    String description;
    int priority;
    int time_taken;
    String prefTimeOfDay;
    boolean isStatic;
    LocalDateTime deadline;
    boolean completed;
    LocalDateTime startDateTime;


    public UserTask(int priority, String description, int time_taken, boolean isStatic) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.priority = priority;
        this.time_taken = time_taken;
        this.isStatic = isStatic;
        this.startDateTime = LocalDateTime.now();
        this.completed = false;
    }
}
