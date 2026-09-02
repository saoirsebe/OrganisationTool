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
     * It does this by calling classes ParsingToSchema, TimeOfDayPredictor and TaskDurationPredictor
     * @throws Exception
     */
    public static void createAndTrainTaskDurationPredictor() throws Exception {
        TaskDurationPredictor durationPredictorModel = new TaskDurationPredictor();
        durationPredictorModel.modelSetup();
        durationPredictorModel.initialModelTraining();
        durationPredictorModel.trainModel();
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

