package org.example;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Properties;

public class ParsingToSchema {
    /**
     * This class is used to parse the task description and the method returnParsedTask will return [action, target, target_type, raw]
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,ner");
        /*
        * tokenize — split raw text into individual word tokens
        * ssplit — split into sentences (needed even for one sentence — it's a required building block)
        * pos — tag each token's part of speech (verb, noun, etc.)
        * lemma — compute the base form of each token
        * depparse — build the dependency tree
        * ner — tag entities (locations, people, etc.)
         */


    }
    public String[] returnParsedTask(String taskDescription){
        return []
    }
}

