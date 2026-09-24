package org.ogmodel;

import java.io.IOException;
import java.util.List;

public class FakeTaskDurationPredictor extends TaskDurationPredictor {
    @Override
    List<ParsedTaskDescription> getAllParsedTrainingTasks() {
        List<String> demoTaskTitles = List.of(
                "Go to the gym",
                "Go for a walk",
                "Read book",
                "Clean my room",
                "Do the laundry",
                "Cook a healthy meal",
                "Meditate for 10 minutes",
                "Write in my journal",
                "Go grocery shopping",
                "Study for an hour",
                "Finish my homework",
                "Call a friend",
                "Organise my desk",
                "Take out the rubbish",
                "Do the dishes",
                "Plan my day",
                "Learn something new",
                "Stretch for 15 minutes",
                "Do an ab workout"
        );
        return ParsingToSchema.returnParsedTasks(demoTaskTitles);
    }

    @Override
    List<Integer> getDurationTimes() throws IOException {
        return List.of(
                80,
                60,
                40,
                40,
                20,
                50,
                10,
                30,
                60,
                60,
                45,
                30,
                25,
                5,
                15,
                15,
                60,
                15,
                45
        );
    }
}

