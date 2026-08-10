package org.example;

import java.util.List;

public class ParsedTaskDescription {
    String action;
    List<String> targets;
    String target_type;
    String raw;

    public ParsedTaskDescription(String action, List<String> targets, String target_type, String raw){
        this.action = action;
        this.targets = targets;
        this.target_type = target_type;
        this.raw = raw;
    }
}
