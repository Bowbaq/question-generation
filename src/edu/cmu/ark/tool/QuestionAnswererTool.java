package edu.cmu.ark.tool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import edu.cmu.ark.AnalysisUtilities;
import edu.cmu.ark.SuperSenseWrapper;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class QuestionAnswererTool extends BaseTool {
	public QuestionAnswererTool() throws FileNotFoundException, IOException {
		super();
	}

	@Override
	public String getConfigPath() {
		return null;
	}

	@Override
	public void run() {
		String input = getDocumentFromStdin();
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();

		try {
			// Segment document into sentences
			final String[] sentences = input.split("\n");

			// Parse individual sentences
			for (final String sentence : sentences) {
				String clean_sentence = AnalysisUtilities.preprocess(sentence);
				Tree parse = AnalysisUtilities.parseSentence(clean_sentence).getTree();
				GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
				Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();
				List<String> super_senses = SuperSenseWrapper.getInstance().annotateSentenceWithSupersenses(parse);

				System.out.println(clean_sentence);
				System.out.println(parse.labeledYield());
				System.out.println(tdl);
				System.out.println(super_senses + "\n");
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
