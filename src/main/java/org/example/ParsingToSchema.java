package org.example;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

/**
 * This class is used to parse the task description.
 */
public class ParsingToSchema {

    /**
     *
     * @param deps Dependency tree
     * @param head The word we want the children of
     * @return
     */
    private String getFullPhrase(SemanticGraph deps, IndexedWord head) {
        List<IndexedWord> subtree = new ArrayList<>(deps.descendants(head));
        subtree.sort((a, b) -> Integer.compare(a.index(), b.index()));

        StringBuilder sb = new StringBuilder();
        for (IndexedWord w : subtree) {
            String pos = w.get(PartOfSpeechAnnotation.class); // or w.tag()
            if (isContentWord(pos)) {
                sb.append(w.word()).append(" ");
            }
        }
        return sb.toString().trim();
    }

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
            CoreDocument doc = new CoreDocument(taskDescription);
            pipeline.annotate(doc);  // Runs every annotator configured in pipeline. Doc is now made up of tokens, POS tags, lemmas, dependency trees, etc.
            CoreSentence sentence = doc.sentences().get(0);  // Grab sentence
            SemanticGraph deps = sentence.dependencyParse(); // Gets dependency tree for taskDescription

            IndexedWord root = deps.getFirstRoot();
            String action = root.lemma(); // The lemma of the root word should be the action
            // Handle particle verbs (e.g. "turn off" -> "turn off" instead of just "turn")
            for (SemanticGraphEdge edge : deps.outgoingEdgeList(root)) {
                String rel = edge.getRelation().toString();
                if (rel.equals("compound:prt")) {
                    action = action + " " + edge.getDependent().word();
                }
            }

            // Collect objects of the action
            List<String> objects = new ArrayList<>();
            for (SemanticGraphEdge edge : deps.outgoingEdgeList(root)) {
                IndexedWord dep = edge.getDependent();
                objects.add(dep.lemma()); //Add each dependent of the action to objects list
            }

            ParsedTaskDescription parsedTask = new ParsedTaskDescription(action, objects, null, taskDescription);
            parsedTasks.add(parsedTask);
        }

        return parsedTasks;
    }
}

