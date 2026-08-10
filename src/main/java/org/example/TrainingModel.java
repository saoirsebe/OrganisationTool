package org.example;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Properties;


public class TrainingModel {
    /**
     * This class will train a model (using my synthetic dataset MS-LaTTE_synthetic) to predict the best time of day for the task and the duration of the task.
     * It does this by calling classes TimeOfDayPredictor and TaskDurationPredictor
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        List<Task> tasks = mapper.readValue(
                new File("src/main/resources/MS-LaTTE_synthetic.json"),
                mapper.getTypeFactory().constructCollectionType(List.class, Task.class)
        );

        System.out.println("Loaded " + tasks.size() + " tasks");

        Task first = tasks.get(0);
        System.out.println("Task: " + first.TaskTitle);
        if (first.LocJudgements != null) {
            for (LocJudgement lj : first.LocJudgements) {
                if (lj.isKnown()) {
                    System.out.println("  Location: " + lj.Locations);
                }
            }
        }
        if (first.TimeJudgements != null) {
            for (TimeJudgement lj : first.TimeJudgements) {
                if (lj.isKnown()) {
                    System.out.println("  Time: " + lj.Times);
                }
            }
        }
        if (first.TimeTaken != null) {
            for (TimeTaken lj : first.TimeTaken) {
                System.out.println("  Time taken: " + lj.EstimatedMinutes);
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Task {
    public String ID;
    public String TaskTitle;
    public List<LocJudgement> LocJudgements;
    public List<TimeJudgement> TimeJudgements;
    public List<TimeTaken> TimeTaken;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class LocJudgement {
    public String Known;
    public List<String> Locations;
    public List<String> PublicLocations;

    public boolean isKnown() {
        return "yes".equalsIgnoreCase(Known);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TimeJudgement {
    public String Known;
    public List<String> Times;

    public boolean isKnown() {
        return "yes".equalsIgnoreCase(Known);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TimeTaken{
    public List<Integer> EstimatedMinutes;

}

