package edu.cmu.ark.tool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.ark.AnalysisUtilities;
import edu.cmu.ark.GlobalProperties;
import edu.cmu.ark.InitialTransformationStep;
import edu.cmu.ark.Question;
import edu.cmu.ark.QuestionRanker;
import edu.cmu.ark.QuestionTransducer;
import edu.cmu.ark.data.ParseResult;
import edu.stanford.nlp.trees.Tree;

public class QuestionAskerTool extends BaseTool {
	private final QuestionTransducer		question_transducer		= new QuestionTransducer();
	private final InitialTransformationStep	question_transformer	= new InitialTransformationStep();
	private final QuestionRanker			question_ranker			= new QuestionRanker();

	// Flags
	private boolean							drop_pronouns;
	private boolean							downweight_pronouns;
	private boolean							do_non_pronoun_npc;
	private boolean							do_pronoun_npc;

	private boolean							avoid_frequent_words;

	private boolean							prefer_wh_questions;
	private boolean							wh_questions_only;

	private boolean							do_stemming;

	private boolean							is_debug;

	// Options
	private String							ranking_model_path;
	private Integer							max_question_length;

	public QuestionAskerTool() throws FileNotFoundException, IOException {
		super();
		configure();
	}

	@Override
	public String getConfigPath() {
		return "config/question-asker.properties";
	}

	@Override
	public void run() {
		String input = getDocumentFromStdin();

		String[] split = input.split("\nSee also\n\n");

		if (split.length == 2) {
			input = split[0];
		}

		try {
			// Segment document into sentences
			final List<String> sentences = AnalysisUtilities.getSentences(input);

			// Parse individual sentences
			final List<Tree> parsed_sentences = new ArrayList<Tree>();
			for (final String sentence : sentences) {
				if (is_debug) {
					System.err.println("[Question Asker] sentence: " + sentence);
				}

				ParseResult parse = AnalysisUtilities.parseSentence(sentence);

				if (null != parse) {
					parsed_sentences.add(parse.getTree());
				}
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
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void configure() {
		// Infer flags from properties
		ranking_model_path = options.getProperty("ranking-model-path");
		max_question_length = Integer.parseInt(options.getProperty("max-question-length"));

		drop_pronouns = (null == options.getProperty("keep-pronouns"));
		if (null != options.getProperty("downweight-pronouns") && options.getProperty("downweight-pronouns").equals("true")) {
			drop_pronouns = false;
			downweight_pronouns = true;
		}
		do_non_pronoun_npc = (null != options.getProperty("do-non-pronoun-npc"));
		do_pronoun_npc = (null != options.getProperty("do-pronoun-npc"));

		avoid_frequent_words = (null != options.getProperty("avoid-frequent-answers"));

		prefer_wh_questions = (null != options.getProperty("prefer-wh-questions"));
		wh_questions_only = (null != options.getProperty("wh-questions-only"));

		do_stemming = (null != options.getProperty("no-stemming"));

		is_debug = GlobalProperties.isDebug();

		// Configure tools
		question_transducer.setAvoidPronounsAndDemonstratives(drop_pronouns);
		question_transformer.setDoPronounNPC(do_pronoun_npc);
		question_transformer.setDoNonPronounNPC(do_non_pronoun_npc);

		if (null == ranking_model_path) {
			System.err.println("[Question Asker] Fatal error: path to model is needed (--model)");
			System.exit(-1);
		}

		System.err.print("[Question Asker] Loading question ranking models from " + ranking_model_path + "...");
		question_ranker.loadModel(ranking_model_path);
		System.err.println("Done");
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

			if (is_debug) {
				System.out.print("\t" + AnalysisUtilities.getCleanedUpYield(question.getSourceTree()) + "\t");

				if (null != question.getAnswerPhraseTree()) {
					System.out.print(AnalysisUtilities.getCleanedUpYield(question.getAnswerPhraseTree()));
				}

				System.out.print("\t" + question.getScore());
			}

			System.out.println();
		}
	}
}
