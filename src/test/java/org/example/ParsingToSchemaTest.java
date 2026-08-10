package org.example;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;

import com.fasterxml.jackson.databind.ObjectMapper;

class ParsingToSchemaTest {

    private final ParsingToSchema taskParser = new ParsingToSchema();

    @Test
    void returnParsedTasks_parsesSimpleImperativeCommands() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        List<Task> tasks = mapper.readValue(
                new File("src/main/resources/MS-LaTTE_synthetic.json"),
                mapper.getTypeFactory().constructCollectionType(List.class, Task.class)
        );

        // List of the first 20 task descriptions
        List<String> taskDescriptions = tasks.stream()
                .limit(20)
                .map(task -> task.TaskTitle)
                .toList();


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

        // Spot-check particle-verb handling: "put on license sticker" -> action "put on"
        assertEquals("put on", parsedTasks.get(8).action,
                "Particle 'put' should be appended to root lemma 'on' via compound:prt");
    }

    @Test
    void returnParsedTasks_handlesEmptyInputList() {
        List<ParsedTaskDescription> parsedTasks = taskParser.returnParsedTasks(List.of());

        assertNotNull(parsedTasks);
        assertTrue(parsedTasks.isEmpty(), "Empty input list should produce an empty result list");
    }

    @Test
    void returnParsedTasks_printsParsedResultsForManualInspection() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        List<Task> tasks = mapper.readValue(
                new File("src/main/resources/MS-LaTTE_synthetic.json"),
                mapper.getTypeFactory().constructCollectionType(List.class, Task.class)
        );

        // List of the first 20 task descriptions
        List<String> taskDescriptions = tasks.stream()
                .limit(20)
                .map(task -> task.TaskTitle)
                .toList();

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