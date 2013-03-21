package edu.cmu.ark.tests;

import junit.framework.TestCase;

import edu.cmu.ark.AnalysisUtilities;
import edu.cmu.ark.SpecificityAnalyzer;
import edu.stanford.nlp.trees.Tree;

public class TestSpecificityAnalyzer extends TestCase {

    protected void setUp() {
        sa = SpecificityAnalyzer.getInstance();
    }

    protected void tearDown() {
        sa = null;
    }

    public void testPronouns() {
        Tree parse = AnalysisUtilities.parseSentence("I had met her when that happened.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumPronouns(), sa.getNumPronouns() == 3);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 3);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 3);

        parse = AnalysisUtilities.parseSentence("These are new.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumPronouns(), sa.getNumPronouns() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
    }

    public void testVagueNPs() {
        Tree parse = AnalysisUtilities.parseSentence("Those men went to the store.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumPronouns(), sa.getNumPronouns() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 2);

        parse = AnalysisUtilities.parseSentence("The man went to the store in that town.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 2);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 3);

        parse = AnalysisUtilities.parseSentence("John Smith went to the bookstore that was built in the city.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 3);
        assertTrue(parse.toString() + "\t" + sa.getNumNPsWithProperNouns(), sa.getNumNPsWithProperNouns() == 1);

        parse = AnalysisUtilities.parseSentence("John Smith went to the bookstore that was built in New York.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 3);
        assertTrue(parse.toString() + "\t" + sa.getNumNPsWithProperNouns(), sa.getNumNPsWithProperNouns() == 2);

        parse = AnalysisUtilities.parseSentence("A big red dog barked.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPsWithProperNouns(), sa.getNumNPsWithProperNouns() == 0);

        parse = AnalysisUtilities.parseSentence("They strove for independence.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 2);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 2);

        parse = AnalysisUtilities.parseSentence("John went to the other.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 2);

        parse = AnalysisUtilities.parseSentence("John bought those.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 2);
    }

    public void testComplexNPs() {
        Tree parse = AnalysisUtilities.parseSentence("The best pictures available at the moment are from John Smith.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 3);

        parse = AnalysisUtilities.parseSentence("It was the ghost of the Soviet Brigade discovered in Cuba.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 4);

        parse = AnalysisUtilities.parseSentence("I read the paper which he promised us in New York.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 3);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 5);
        assertTrue(parse.toString() + "\t" + sa.getNumPronouns(), sa.getNumPronouns() == 3);
    }

    public void testCrossSentenceReferences() {
        Tree parse = AnalysisUtilities.parseSentence("Jones was stationed at another command in Colorado.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumNPsWithProperNouns() == 2);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumReferenceWords() == 1);

        parse = AnalysisUtilities.parseSentence("The other boy ran.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumNPsWithProperNouns() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumReferenceWords() == 1);
    }

    public void testDates() {
        Tree parse = AnalysisUtilities.parseSentence("The incident happened in 1974.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 2);

        parse = AnalysisUtilities.parseSentence("The incident happened in December, 1974.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 2);

        parse = AnalysisUtilities.parseSentence("The incident happened in the 1970s.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 2);
    }

    public void testPossession() {
        Tree parse = AnalysisUtilities.parseSentence("John's friend ran.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumPronouns(), sa.getNumPronouns() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumNPsWithProperNouns(), sa.getNumNPsWithProperNouns() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 2);

        parse = AnalysisUtilities.parseSentence("The man's friend ran.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumPronouns(), sa.getNumPronouns() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPsWithProperNouns(), sa.getNumNPsWithProperNouns() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 2);
    }

    public void testConjunctions() {
        Tree parse = AnalysisUtilities.parseSentence("Smith commands the Navy's Indian Ocean and Persian Gulf forces.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 3);
    }

    /**
     * expected to fail. system doesn't address these issues
     */
    public void testToughCases() {
        // "the world" is specific enough by itself
        Tree parse = AnalysisUtilities.parseSentence("The largest animal in the world is the Blue Whale.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 0);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 3);

        parse = AnalysisUtilities.parseSentence("The 60s was a tumultuous decade.").getTree();
        sa.analyze(parse);
        assertTrue(parse.toString() + "\t" + sa.getNumVagueNPs(), sa.getNumVagueNPs() == 1);
        assertTrue(parse.toString() + "\t" + sa.getNumNPs(), sa.getNumNPs() == 2);

    }

    private SpecificityAnalyzer sa;

}
