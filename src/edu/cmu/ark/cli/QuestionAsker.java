// Question Generation via Overgenerating Transformations and Ranking
// Copyright (c) 2008, 2009 Carnegie Mellon University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
// Michael Heilman
// Carnegie Mellon University
// mheilman@cmu.edu
// http://www.cs.cmu.edu/~mheilman

package edu.cmu.ark.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.ark.AnalysisUtilities;
import edu.cmu.ark.GlobalProperties;
import edu.cmu.ark.InitialTransformationStep;
import edu.cmu.ark.Question;
import edu.cmu.ark.QuestionRanker;
import edu.cmu.ark.QuestionTransducer;
import edu.stanford.nlp.trees.Tree;
// import java.text.NumberFormat;
// import weka.classifiers.functions.LinearRegression;
// import edu.cmu.ark.ranking.WekaLinearRegressionRanker;

/**
 * Wrapper class for outputting a (ranked) list of questions given an entire document,
 * not just a sentence. It wraps the three stages discussed in the technical report and calls each in turn
 * (along with parsing and other preprocessing) to produce questions.
 * This is the typical class to use for running the system via the command line.
 * Example usage:
 * 
 java -server -Xmx800m -cp
 * lib/weka-3-6.jar:lib/stanford-parser-2008-10-26.jar:bin:lib/jwnl.jar:lib/commons-logging.jar:lib/commons-lang-2.4.jar:lib/
 * supersense-tagger.jar:lib/stanford-ner-2008-05-07.jar:lib/arkref.jar \
 * edu/cmu/ark/QuestionAsker \
 * --verbose --simplify --group \
 * --model models/linear-regression-ranker-06-24-2010.ser.gz \
 * --prefer-wh --max-length 30 --downweight-pro
 * 
 * @author mheilman@cs.cmu.edu
 */
public class QuestionAsker {
    private final QuestionTransducer        question_transducer  = new QuestionTransducer();
    private final InitialTransformationStep question_transformer = new InitialTransformationStep();
    private final QuestionRanker            question_ranker      = new QuestionRanker();

    // Flags
    private boolean                         is_verbose;
    private boolean                         is_debug;

    private boolean                         drop_pronouns        = true;
    private boolean                         downweight_pronouns;
    private boolean                         do_non_pronoun_npc;
    private boolean                         do_pronoun_npc       = true;

    private boolean                         avoid_frequent_words;

    private boolean                         prefer_wh_questions;
    private boolean                         wh_questions_only;

    private final boolean                   do_stemming          = true;

    // Arguments
    private String                          model_path;
    private Integer                         max_question_length  = 1000;

    public QuestionAsker() {
        // Pre-load analysis utilities
        AnalysisUtilities.load();
    }

    private void parseArgs(final String[] args) {
        int i = 0;

        while (i < args.length) {
            if (args[i].equals("--debug")) {
                GlobalProperties.setDebug(true);
                is_debug = true;
            } else if (args[i].equals("--verbose")) {
                is_verbose = true;
            } else if (args[i].equals("--model")) { // ranking model path
                model_path = args[i + 1];
                i++;
            } else if (args[i].equals("--keep-pro")) {
                drop_pronouns = false;
            } else if (args[i].equals("--downweight-pro")) {
                drop_pronouns = false;
                downweight_pronouns = true;
            } else if (args[i].equals("--downweight-frequent-answers")) {
                avoid_frequent_words = true;
            } else if (args[i].equals("--properties")) {
                GlobalProperties.loadProperties(args[i + 1]);
                i++;
            } else if (args[i].equals("--prefer-wh")) {
                prefer_wh_questions = true;
            } else if (args[i].equals("--just-wh")) {
                wh_questions_only = true;
            } else if (args[i].equals("--full-npc")) {
                do_non_pronoun_npc = true;
            } else if (args[i].equals("--no-npc")) {
                do_pronoun_npc = false;
            } else if (args[i].equals("--max-length")) {
                max_question_length = Integer.parseInt(args[i + 1]);
                i++;
            }

            i++;
        }
    }

    private void configure() {
        question_transducer.setAvoidPronounsAndDemonstratives(drop_pronouns);
        question_transformer.setDoPronounNPC(do_pronoun_npc);
        question_transformer.setDoNonPronounNPC(do_non_pronoun_npc);

        if (null == model_path) {
            System.err.println("[Question Asker] Fatal error: path to model is needed (--model)");
            System.exit(-1);
        }

        System.out.print("[Question Asker] Loading question ranking models from " + model_path + "...");
        question_ranker.loadModel(model_path);
        System.out.println("Done");
    }

    private void process(final String input) {
        final long start = System.currentTimeMillis();
        try {
            // Segment document into sentences
            final List<String> sentences = AnalysisUtilities.getSentences(input);
            // Parse individual sentences
            final List<Tree> parsed_sentences = new ArrayList<Tree>();
            for (final String sentence : sentences) {
                if (is_debug) {
                    System.err.println("[Question Asker] sentence: " + sentence);
                }

                parsed_sentences.add(AnalysisUtilities.parseSentence(sentence).getTree());
            }
            if (is_debug) {
                System.err.println("[Question Asker] Parsing time:\t" + (System.currentTimeMillis() - start) / 1000.0);
            }

            // Step 1: sentence transformation
            final List<Question> transformed_sentences = question_transformer.transform(parsed_sentences);

            // Step 2: question generation
            final List<Question> output_questions = new ArrayList<Question>();
            for (final Question transformation : transformed_sentences) {
                if (is_debug) {
                    System.err.println("[Question Asker] Stage 2 Input: " + transformation.getIntermediateTree().yield().toString());
                }
                question_transducer.generateQuestionsFromParse(transformation);
                output_questions.addAll(question_transducer.getQuestions());
            }
            // Remove duplicates
            QuestionTransducer.removeDuplicateQuestions(output_questions);

            // Step 3: question ranking
            question_ranker.scoreGivenQuestions(output_questions);
            QuestionRanker.adjustScores(output_questions, parsed_sentences, avoid_frequent_words, prefer_wh_questions, downweight_pronouns, do_stemming);
            QuestionRanker.sortQuestions(output_questions, false);

            printQuestions(output_questions);

            if (is_debug) {
                System.err.println("[Question Asker] Time elapsed:\t" + (System.currentTimeMillis() - start) / 1000.0);
                System.err.println("\nInput Text:");
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void printQuestions(final List<Question> output_questions) {
        for (final Question question : output_questions) {
            if (question.getTree().getLeaves().size() > max_question_length) {
                continue;
            }

            if (wh_questions_only && question.getFeatureValue("whQuestion") != 1.0) {
                continue;
            }

            System.out.print(question.yield());

            if (is_verbose) {
                System.out.print("\t" + AnalysisUtilities.getCleanedUpYield(question.getSourceTree()) + "\t");

                if (null != question.getAnswerPhraseTree()) {
                    System.out.print(AnalysisUtilities.getCleanedUpYield(question.getAnswerPhraseTree()));
                }

                System.out.print("\t" + question.getScore());
            }

            System.out.println();
        }
    }

    private String getDocumentFromStdin() throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        final StringBuilder document = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            document.append(line);
            line = reader.readLine();
        }

        return document.length() == 0 ? null : document.toString();
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        final QuestionAsker asker = new QuestionAsker();
        asker.parseArgs(args);
        asker.configure();

        // Get document from stdin
        do {
            if (GlobalProperties.isDebug()) {
                System.err.println("\nInput Text:");
            }

            final String input = asker.getDocumentFromStdin();
            if (null == input) {
                return;
            }

            asker.process(input);
        } while (GlobalProperties.isDebug());
    }

    // TODO: Check unused
    public static void printFeatureNames() {
        final List<String> featureNames = Question.getFeatureNames();
        for (int i = 0; i < featureNames.size(); i++) {
            if (i > 0) {
                System.out.println();
            }
            System.out.print(featureNames.get(i));
        }
        System.out.println();
    }

}
