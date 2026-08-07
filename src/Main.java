import java.time.*;
import java.time.temporal.*;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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




public class MsLatteLoader {
    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File("MS-LaTTE.json"));

        System.out.println("Total entries: " + root.size());

        // Inspect the first entry
        JsonNode first = root.get(0);
        System.out.println(first.toPrettyString());

        // Print title + known locations/times for the first 5 entries
        for (int i = 0; i < 5; i++) {
            JsonNode entry = root.get(i);
            String title = entry.get("TaskTitle").asText();

            System.out.print(title + " | locs: ");
            for (JsonNode j : entry.get("LocJudgements")) {
                if (j.get("Known").asBoolean()) {
                    System.out.print(j.get("Locations") + " ");
                }
            }

            System.out.print("| times: ");
            for (JsonNode j : entry.get("TimeJudgements")) {
                if (j.get("Known").asBoolean()) {
                    System.out.print(j.get("Times") + " ");
                }
            }
            System.out.println();
        }
    }
}