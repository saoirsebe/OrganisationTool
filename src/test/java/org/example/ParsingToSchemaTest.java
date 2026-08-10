package org.example;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParsingToSchemaTest {

    private final ParsingToSchema taskParser = new ParsingToSchema();

    @Test
    void returnParsedTasks_parsesSimpleImperativeCommands() {
        List<String> taskDescriptions = Arrays.asList(
                "Turn off the lights",
                "Book a flight to Paris",
                "Send an email to John",
                "go to the gym"
        );

        List<ParsedTaskDescription> parsedTasks = taskParser.returnParsedTasks(taskDescriptions);

        // Structural assertions
        assertNotNull(parsedTasks, "Parsed task list should not be null");
        assertEquals(taskDescriptions.size(), parsedTasks.size(),
                "Should return exactly one ParsedCommand per input description");

        for (int i = 0; i < parsedTasks.size(); i++) {
            ParsedTaskDescription parsed = parsedTasks.get(i);
            assertNotNull(parsed.action, "Action should not be null");
            assertFalse(parsed.action.isBlank(), "Action should not be blank");
            assertEquals(taskDescriptions.get(i), parsed.raw,
                    "Raw field should match the original input string exactly");
            assertNull(parsed.target_type,
                    "target_type is never set in the current implementation, so it should be null");
            assertNotNull(parsed.targets, "Target/objects list should not be null");
        }

        // Spot-check particle-verb handling: "Turn off the lights" -> action "turn off"
        assertEquals("turn off", parsedTasks.get(0).action,
                "Particle 'off' should be appended to root lemma 'turn' via compound:prt");
    }

    @Test
    void returnParsedTasks_handlesEmptyInputList() {
        List<ParsedTaskDescription> parsedTasks = taskParser.returnParsedTasks(List.of());

        assertNotNull(parsedTasks);
        assertTrue(parsedTasks.isEmpty(), "Empty input list should produce an empty result list");
    }

    @Test
    void returnParsedTasks_printsParsedResultsForManualInspection() {
        List<String> taskDescriptions = Arrays.asList(
                "Turn off the lights",
                "Book a flight to Paris",
                "Send an email to John",
                "Schedule a meeting with the design team tomorrow"
        );

        List<ParsedTaskDescription> parsedTasks = taskParser.returnParsedTasks(taskDescriptions);

        System.out.println("Parsed Tasks:");
        for (ParsedTaskDescription parsed : parsedTasks) {
            System.out.printf(
                    "  raw=\"%s\" -> action=\"%s\", target=%s, target_type=%s%n",
                    parsed.raw,
                    parsed.action,
                    parsed.targets,
                    parsed.target_type
            );
        }

        assertEquals(taskDescriptions.size(), parsedTasks.size());
    }

}