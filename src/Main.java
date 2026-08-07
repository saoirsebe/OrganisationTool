import java.time.*;
import java.time.temporal.*;
import java.util.UUID;

import java.io.File;
import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {



    }
}




class Task {
    private final String id;
    String description;
    int priority;
    int time_taken;
    String prefTimeOfDay;
    boolean isStatic;
    LocalDateTime deadline;
    boolean completed;
    LocalDateTime startDateTime;


    public Task(int priority, String description, int time_taken, boolean isStatic) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.priority = priority;
        this.time_taken = time_taken;
        this.isStatic = isStatic;
        this.startDateTime = LocalDateTime.now();
        this.completed = false;
    }
}


