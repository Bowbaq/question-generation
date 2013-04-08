// Question Generation via Overgenerating Transformations and Ranking
// Copyright (c) 2010 Carnegie Mellon University. All Rights Reserved.
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

package edu.cmu.ark;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;

import org.apache.commons.lang.StringUtils;

import edu.cmu.ark.data.ParseResult;
import edu.cmu.ark.util.StanfordParserClient;
import edu.cmu.ark.util.VerbConjugator;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeReader;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import edu.stanford.nlp.util.Pair;

public class AnalysisUtilities {
	private final static StanfordParserClient		parser_client;
	private final static CollinsHeadFinder			head_finder			= new CollinsHeadFinder();
	private final static LabeledScoredTreeFactory	tree_factory		= new LabeledScoredTreeFactory();
	private final static PennTreebankLanguagePack	ptb_language_pack	= new PennTreebankLanguagePack();

	static {
		VerbConjugator.load_conjugations(GlobalProperties.getProperties().getProperty("verbConjugationsFile", "config" + File.separator + "verbConjugations.txt"));
		parser_client = new StanfordParserClient("127.0.0.1", 5556);
	}

	public static CollinsHeadFinder getHeadFinder() {
		return AnalysisUtilities.head_finder;
	}

	public static String getLemma(final String word, final String pos) {
		if (!(pos.startsWith("N") || pos.startsWith("V") || pos.startsWith("J") || pos.startsWith("R")) || pos.startsWith("NNP")) {
			return word.toLowerCase();
		}

		final String result = word.toLowerCase();

		if (result.equals("is") || result.equals("are") || result.equals("were") || result.equals("was")) {
			return "be";
		} else {
			try {
				IndexWord iw;
				if (pos.startsWith("V")) {
					iw = Dictionary.getInstance().getMorphologicalProcessor().lookupBaseForm(POS.VERB, result);
				} else if (pos.startsWith("N")) {
					iw = Dictionary.getInstance().getMorphologicalProcessor().lookupBaseForm(POS.NOUN, result);
				} else if (pos.startsWith("J")) {
					iw = Dictionary.getInstance().getMorphologicalProcessor().lookupBaseForm(POS.ADJECTIVE, result);
				} else {
					iw = Dictionary.getInstance().getMorphologicalProcessor().lookupBaseForm(POS.ADVERB, result);
				}

				return null == iw ? result : iw.getLemma();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * Remove traces and non-terminal decorations (e.g., "-SUBJ" in "NP-SUBJ") from a Penn Treebank-style tree.
	 * 
	 * @param inputTree
	 */
	public static void normalizeTree(final Tree inputTree) {
		inputTree.label().setFromString("ROOT");

		final List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
		final List<TsurgeonPattern> ps = new ArrayList<TsurgeonPattern>();
		String tregexOpStr;
		TregexPattern matchPattern;
		TsurgeonPattern p;
		TregexMatcher matcher;

		tregexOpStr = "/\\-NONE\\-/=emptynode";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(inputTree);
		ps.add(Tsurgeon.parseOperation("prune emptynode"));
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		p = Tsurgeon.collectOperations(ps);
		ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));
		Tsurgeon.processPatternsOnTree(ops, inputTree);

		Label nonterminalLabel;

		tregexOpStr = "/.+\\-.+/=nonterminal < __";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		matcher = matchPattern.matcher(inputTree);
		while (matcher.find()) {
			nonterminalLabel = matcher.getNode("nonterminal");
			if (nonterminalLabel == null) {
				continue;
			}
			nonterminalLabel.setFromString(AnalysisUtilities.ptb_language_pack.basicCategory(nonterminalLabel.value()));
		}

	}

	// TODO: Bring back ParseResult / cleanup this method
	public static ParseResult parseSentence(final String sentence) {
		return AnalysisUtilities.parser_client.parse(sentence);
	}

	public static Tree readTreeFromString(final String parseStr) {
		// read in the input into a Tree data structure
		final TreeReader treeReader = new PennTreeReader(new StringReader(parseStr), AnalysisUtilities.tree_factory);
		Tree inputTree = null;
		try {
			inputTree = treeReader.readTree();

		} catch (final IOException e) {
			e.printStackTrace();
		}
		return inputTree;
	}

	public static String abbrevTree(final Tree tree) {
		final ArrayList<String> toks = new ArrayList<String>();
		for (final Tree L : tree.getLeaves()) {
			toks.add(L.label().toString());
		}
		return tree.label().toString() + "[" + StringUtils.join(toks, " ") + "]";
	}

	public static void addPeriodIfNeeded(final Tree input) {
		final String tregexOpStr = "ROOT < (S=mainclause !< /\\./)";
		final TregexPattern matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		final TregexMatcher matcher = matchPattern.matcher(input);

		if (matcher.find()) {
			TsurgeonPattern p;
			final List<TsurgeonPattern> ps = new ArrayList<TsurgeonPattern>();
			final List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();

			ps.add(Tsurgeon.parseOperation("insert (. .) >-1 mainclause"));
			p = Tsurgeon.collectOperations(ps);
			ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));
			Tsurgeon.processPatternsOnTree(ops, input);
		}
	}

	public static boolean cCommands(final Tree root, final Tree n1, final Tree n2) {
		if (n1.dominates(n2)) {
			return false;
		}

		Tree n1Parent = n1.parent(root);
		while (n1Parent != null && n1Parent.numChildren() == 1) {
			n1Parent = n1Parent.parent(root);
		}

		if (n1Parent != null && n1Parent.dominates(n2)) {
			return true;
		}

		return false;
	}

	public static String cleanUpSentenceString(final String s) {
		String res = s;
		// if(res.length() > 1){
		// res = res.substring(0,1).toUpperCase() + res.substring(1);
		// }

		res = res.replaceAll("\\s([\\.,!\\?\\-;:])", "$1");
		res = res.replaceAll("(\\$)\\s", "$1");
		res = res.replaceAll("can not", "cannot");
		res = res.replaceAll("\\s*-LRB-\\s*", " (");
		res = res.replaceAll("\\s*-RRB-\\s*", ") ");
		res = res.replaceAll("\\s*([\\.,?!])\\s*", "$1 ");
		res = res.replaceAll("\\s+''", "''");
		// res = res.replaceAll("\"", "");
		res = res.replaceAll("``\\s+", "``");
		res = res.replaceAll("\\-[LR]CB\\-", ""); // brackets, e.g., [sic]
		res = res.replaceAll("\\. \\?", ".?");
		res = res.replaceAll(" 's(\\W)", "'s$1");
		res = res.replaceAll("(\\d,) (\\d)", "$1$2"); // e.g., "5, 000, 000" -> "5,000,000"
		res = res.replaceAll("``''", "");

		// remove extra spaces
		res = res.replaceAll("\\s\\s+", " ");
		res = res.trim();

		return res;
	}

	public static void downcaseFirstToken(final Tree inputTree) {
		final Tree firstWordTree = inputTree.getLeaves().get(0);
		if (firstWordTree == null) {
			return;
		}
		final Tree preterm = firstWordTree.parent(inputTree);
		String firstWord = firstWordTree.yield().toString();
		if (!preterm.label().toString().matches("^NNP.*") && !firstWord.equals("I")) {
			// if(firstWord.indexOf('-') == -1 && !firstWord.equals("I")){
			firstWord = firstWord.substring(0, 1).toLowerCase() + firstWord.substring(1);
			firstWordTree.label().setValue(firstWord);
		}

		// if(QuestionTransducer.DEBUG) System.err.println("downcaseFirstToken: "+inputTree.toString());
	}

	public static boolean filterOutSentenceByPunctuation(final String sentence) {
		// return (sentence.indexOf("\"") != -1
		// || sentence.indexOf("''") != -1
		// || sentence.indexOf("``") != -1
		// || sentence.indexOf("*") != -1);
		if (sentence.indexOf("*") != -1) {
			return true;
		}

		// if(sentence.matches("[^\\w\\-\\/\\?\\.,;:\\$\\#\\&\\(\\) ]")){
		// return true;
		// }

		return false;
	}

	public static String getCleanedUpYield(final Tree inputTree) {
		final Tree copyTree = inputTree.deeperCopy();

		// if(GlobalProperties.getDebug()) System.err.println("yield:"+copyTree.toString());

		return AnalysisUtilities.cleanUpSentenceString(copyTree.yield().toString());
	}

	public static int getNumberOfMatchesInTree(final String tregex_pattern, final Tree t) {
		int res = 0;
		final TregexMatcher m = TregexPatternFactory.getPattern(tregex_pattern).matcher(t);
		while (m.find()) {
			res++;
		}
		return res;
	}

	public static List<String> getSentences(String document) {
		final DocumentPreprocessor preprocessor = new DocumentPreprocessor(false);
		final List<String> sentences = new ArrayList<String>();

		document = AnalysisUtilities.preprocess(document);

		final String[] paragraphs = document.split("\\n");

		for (final String paragraph : paragraphs) {
			if (GlobalProperties.isDebug()) {
				System.err.println(paragraph);
			}

			List<List<? extends HasWord>> parsed_sentences = new ArrayList<List<? extends HasWord>>();
			try {
				parsed_sentences = preprocessor.getSentencesFromText(new StringReader(paragraph));
			} catch (final Exception e) {
				e.printStackTrace();
			}

			for (final List<? extends HasWord> parsed_sentence : parsed_sentences) {
				StringBuilder sentence = new StringBuilder();
				for (final HasWord hasword : parsed_sentence) {
					sentence.append(hasword.word()).append(" ");
				}
				sentences.add(sentence.toString().trim());
			}
		}

		return sentences;
	}

	public static String preprocess(String sentence) {
		// remove trailing whitespace
		sentence = sentence.trim();

		// remove single words in parentheses.
		// the stanford parser api messed up on these
		// by removing the parentheses but not the word in them
		sentence = sentence.replaceAll("\\(\\S*\\)", "");
		sentence = sentence.replaceAll("\\(\\s*\\)", "");

		// some common unicode characters that the tokenizer throws out otherwise
		sentence = sentence.replaceAll("—", "--");
		sentence = sentence.replaceAll("’", "'");
		sentence = sentence.replaceAll("”", "\"");
		sentence = sentence.replaceAll("“", "\"");
		sentence = sentence.replaceAll("é|è|ë|ê", "e");
		sentence = sentence.replaceAll("É|È|Ê|Ë", "E");
		sentence = sentence.replaceAll("ì|í|î|ï", "i");
		sentence = sentence.replaceAll("Ì|Í|Î|Ï", "I");
		sentence = sentence.replaceAll("à|á|â|ã|ä|æ|å", "a");
		sentence = sentence.replaceAll("À|Á|Â|Ã|Ä|Å|Æ", "A");
		sentence = sentence.replaceAll("ò|ó|ô|õ|ö", "o");
		sentence = sentence.replaceAll("Ò|Ó|Ô|Õ|Ö", "O");
		sentence = sentence.replaceAll("ù|ú|û|ü", "u");
		sentence = sentence.replaceAll("Ù|Ú|Û|Ü", "U");
		sentence = sentence.replaceAll("ñ", "n");
		sentence = sentence.replaceAll("� ���", "");

		// contractions
		sentence = sentence.replaceAll("can't", "can not");
		sentence = sentence.replaceAll("won't", "will not");
		sentence = sentence.replaceAll("n't", " not"); // aren't shouldn't don't isn't
		sentence = sentence.replaceAll("are n't", "are not");

		// simply remove other unicode characters
		// if not, the tokenizer replaces them with spaces,
		// which wreaks havoc on the final parse sometimes
		for (int i = 0; i < sentence.length(); i++) {
			if (sentence.charAt(i) > 'z') {
				sentence = sentence.substring(0, i) + sentence.substring(i + 1);
			}
		}

		// add punctuation to the end if necessary
		/*
		 * Matcher matcher = Pattern.compile(".*\\.['\"\n ]*$", Pattern.DOTALL).matcher(sentence); if(!matcher.matches()){ sentence += "."; }
		 */

		return sentence;
	}

	public static String preprocessTreeString(String sentence) {
		sentence = sentence.replaceAll(" n't", " not");
		sentence = sentence.replaceAll("\\(MD ca\\)", "(MD can)");
		sentence = sentence.replaceAll("\\(MD wo\\)", "(MD will)");
		sentence = sentence.replaceAll("\\(MD 'd\\)", "(MD would)");
		sentence = sentence.replaceAll("\\(VBD 'd\\)", "(VBD had)");
		sentence = sentence.replaceAll("\\(VBZ 's\\)", "(VBZ is)");
		sentence = sentence.replaceAll("\\(VBZ 's\\)", "(VBZ is)");
		sentence = sentence.replaceAll("\\(VBZ 's\\)", "(VBZ is)");
		sentence = sentence.replaceAll("\\(VBP 're\\)", "(VBP are)");

		return sentence;
	}

	/**
	 * remove extra quotation marks (a hack due to annoying PTB conventions by which quote marks aren't in the same consituent)
	 * 
	 * @param input
	 */
	public static void removeExtraQuotes(final Tree input) {
		final List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
		String tregexOpStr;
		TregexPattern matchPattern;
		TsurgeonPattern p;
		List<TsurgeonPattern> ps;

		ps = new ArrayList<TsurgeonPattern>();
		tregexOpStr = "ROOT [ << (``=quote < `` !.. ('' < '')) | << (''=quote < '' !,, (`` < ``)) ] ";
		matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
		ps.add(Tsurgeon.parseOperation("prune quote"));
		p = Tsurgeon.collectOperations(ps);
		ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));
		Tsurgeon.processPatternsOnTree(ops, input);

	}

	public static void upcaseFirstToken(final Tree inputTree) {
		final Tree firstWordTree = inputTree.getLeaves().get(0);
		if (firstWordTree == null) {
			return;
		}

		String firstWord = firstWordTree.yield().toString();
		firstWord = firstWord.substring(0, 1).toUpperCase() + firstWord.substring(1);
		firstWordTree.label().setValue(firstWord);

		// if(QuestionTransducer.DEBUG) System.err.println("upcaseFirstToken: "+inputTree.toString());
	}
}