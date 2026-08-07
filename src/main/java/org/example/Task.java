package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
    public String ID;
    public String TaskTitle;
    public String ListTitle;
    public List<LocJudgement> LocJudgements;
    public List<TimeJudgement> TimeJudgements;
}
