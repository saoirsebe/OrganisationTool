package org.example;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;

import java.util.*;

/**
 * This class is used to parse the task description.
 */
public class ParsingToSchema {


    private boolean isContentWord(String pos) {
        // Keep nouns, proper nouns, adjectives
        return pos.startsWith("NN") || pos.startsWith("JJ");
    }



    /**
     * This method is used to parse the task description.
     * @param taskDescriptions
     * @return List of ParsedCommand [action, target, target_type, raw]
     */
    public List<ParsedTaskDescription> returnParsedTasks(List<String> taskDescriptions){
        List<ParsedTaskDescription> parsedTasks = new ArrayList<>();

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, depparse, ner");
        /*
         * tokenize — Splits text into tokens
         * ssplit — Splits tokens into sentences (required building block)
         * pos — Part-of-speech tagging (verb, noun, etc.)
         * lemma — Compute the base form of each token
         * depparse — Build the dependency tree
         * ner — Tag entities (locations, people, etc.)
         */
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);


        for (String taskDescription : taskDescriptions) {
            CoreDocument doc = new CoreDocument("You " + taskDescription);
            pipeline.annotate(doc);  // Runs every annotator configured in pipeline. Doc is now made up of tokens, POS tags, lemmas, dependency trees, etc.
            CoreSentence sentence = doc.sentences().get(0);  // Grab sentence
            SemanticGraph deps = sentence.dependencyParse(); // Gets dependency tree for taskDescription

            IndexedWord root = deps.getFirstRoot();


            String action = root.lemma(); // The lemma of the root word should be the action
            if (Objects.equals(action, "you")) {
                System.out.println(deps);
                CoreDocument doc1 = new CoreDocument(taskDescription);
                pipeline.annotate(doc1);  // Runs every annotator configured in pipeline. Doc is now made up of tokens, POS tags, lemmas, dependency trees, etc.
                CoreSentence sentence1 = doc1.sentences().get(0);  // Grab sentence
                SemanticGraph deps1 = sentence1.dependencyParse(); // Gets dependency tree for taskDescription

                IndexedWord root1 = deps1.getFirstRoot();
                action = root1.lemma();
            }


            // Collect objects of the action
            List<String> objects = new ArrayList<>();

            for (SemanticGraphEdge edge : deps.outgoingEdgeList(root)) {
                String relation = edge.getRelation().getShortName();

                if (relation.equals("obj") || relation.equals("obl") || relation.equals("dobj")) {
                    // Only add real target nouns
                    IndexedWord child = edge.getTarget();
                    objects.add(child.lemma()); //Add each noun dependent of the action to objects list
                }
                else if (relation.equals("compound:prt")) {
                    // Handle particle verbs (e.g. "turn off" -> "turn off" instead of just "turn")
                    action = action + " " + edge.getDependent().word();
                }
            }

            ParsedTaskDescription parsedTask = new ParsedTaskDescription(action, objects, null, taskDescription);
            parsedTasks.add(parsedTask);
        }

        return parsedTasks;
    }
}

