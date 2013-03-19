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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

/**
 * Wrapper class for representing a question and its context.
 * Used to track the current tree as well as the source tree, feature values, etc.
 * 
 * @author mheilman@cmu.edu
 */
public class Question implements Comparable<Question>, Serializable {

    public Question() {
        tree = null;
        setIntermediateTreeSupersenses(null);
        featureMap = new HashMap<String, Double>();
        sourceDocument = null;
        sourceArticleName = "";
    }

    public Question(final Tree tree) {
        this.tree = tree;
        setIntermediateTreeSupersenses(null);
        featureMap = new HashMap<String, Double>();
        sourceDocument = null;
        sourceArticleName = "";
    }

    public Question(final Map<String, Double> features) {
        tree = null;
        setIntermediateTreeSupersenses(null);
        featureMap = new HashMap<String, Double>();
        featureMap.putAll(features);
        sourceDocument = null;
        sourceArticleName = "";
    }

    public Question(final Tree tree, final Map<String, Double> features) {
        this.tree = tree;
        setIntermediateTreeSupersenses(null);
        featureMap = new HashMap<String, Double>();
        featureMap.putAll(features);
        sourceDocument = null;
        sourceArticleName = "";
    }

    public Question(final Tree tree, final Tree intermediateTree, final Tree sourceTree, final Map<String, Double> features) {
        this.intermediateTree = intermediateTree;
        this.sourceTree = sourceTree;
        this.tree = tree;
        setIntermediateTreeSupersenses(null);
        featureMap = new HashMap<String, Double>();
        featureMap.putAll(features);
        sourceDocument = null;
        sourceArticleName = "";
    }

    @Override
    public String toString() {
        String res = "";

        if (tree != null) {
            res += tree.yield().toString();
        }
        res += "\t";
        if (intermediateTree != null) {
            res += "Intermediate:" + intermediateTree.yield().toString();
        }
        res += "\t";
        if (sourceTree != null) {
            res += "Source:" + sourceTree.yield().toString();
        }

        return res;
    }

    public Question deeperCopy() {
        final Question res = new Question();
        res.copyFeatures(featureMap);
        res.setScore(score);
        res.setSourceSentenceNumber(sourceSentenceNumber);
        if (tree != null) {
            res.setTree(tree.deeperCopy());
        }
        res.setLabelScore(labelScore);
        if (answerPhraseTree != null) {
            res.setAnswerPhraseTree(answerPhraseTree.deeperCopy());
        }
        if (sourceTree != null) {
            res.setSourceTree(sourceTree.deeperCopy());
        }
        if (intermediateTree != null) {
            res.setIntermediateTree(intermediateTree.deeperCopy());
        }

        res.setSourceArticleName(sourceArticleName);
        res.setSourceDocument(sourceDocument);

        return res;
    }

    /**
     * Removes features that may have been set before but do not exist now
     * (if the question had been saved/serialized).
     */
    public void removeUnusedFeatures() {
        final List<String> unused = new ArrayList<String>();
        for (final String key : featureMap.keySet()) {
            if (!getFeatureNames().contains(key)) {
                unused.add(key);
            }
        }
        for (final String key : unused) {
            featureMap.remove(key);
        }
    }

    public Object getSourceDocument() {
        return sourceDocument;
    }

    public void setSourceDocument(final Object sourceDocument2) {
        sourceDocument = sourceDocument2;
    }

    public void setTree(final Tree tree) {
        this.tree = tree;
    }

    public Tree getTree() {
        return tree;
    }

    public String yield() {
        if (yield == null) {
            if (tree != null) {
                yield = AnalysisUtilities.getCleanedUpYield(tree);
            } else {
                yield = "";
            }
        }
        return yield;
    }

    public Map<String, Double> getFeatures() {
        return featureMap;
    }

    public void setFeatureValue(final String key, final Double value) {
        featureMap.put(key, value);
    }

    public void copyFeatures(final Map<String, Double> features) {
        featureMap.putAll(features);
    }

    public void setFeatureValues(final List<Double> featureValueList) {
        this.featureValueList = featureValueList;
        Double value;
        final List<String> names = Question.getFeatureNames();

        // only populate the feature map if it looks like we are still using the same feature set
        if (names.size() != featureValueList.size()) {
            return;
        }

        for (int i = 0; i < featureValueList.size(); i++) {
            value = featureValueList.get(i);
            featureMap.put(names.get(i), value);
        }
    }

    protected static List<Double> createFeatureValueList(final Map<String, Double> featureNameToValueMap) {
        final List<Double> res = new ArrayList<Double>();
        Double val;

        for (final String name : getFeatureNames()) {
            val = featureNameToValueMap.get(name);
            if (val == null) {
                val = 0.0;
            }
            res.add(val);
        }

        return res;
    }

    /**
     * returns the index into the featureNames list of the given name
     * (mainly for testing purposes)
     * 
     * @param featurename
     * @return
     */
    public static int getFeatureValueIndex(final String featurename) {
        return getFeatureNames().indexOf(featurename);
    }

    public static List<String> getFeatureNames() {
        if (featureNames == null) {
            featureNames = new ArrayList<String>();

            final String defaultFeatureNames = "performedNPClarification;questionLength;sourceLength;answerPhraseLength;negation;whQuestion;whQuestionPrep;whQuestionWho;whQuestionWhat;whQuestionWhere;whQuestionWhen;whQuestionWhose;whQuestionHowMuch;whQuestionHowMany;isSubjectMovement;removedLeadConjunctions;removedAsides;removedLeadModifyingPhrases;extractedFromAppositive;extractedFromFiniteClause;extractedFromParticipial;extractedFromRelativeClause;mainVerbPast;mainVerbPresent;mainVerbFuture;mainVerbCopula;meanWordFreqSource;meanWordFreqAnswer;numNPsQuestion;numProperNounsQuestion;numQuantitiesQuestion;numAdjectivesQuestion;numAdverbsQuestion;numPPsQuestion;numSubordinateClausesQuestion;numConjunctionsQuestion;numPronounsQuestion;numNPsAnswer;numProperNounsAnswer;numQuantitiesAnswer;numAdjectivesAnswer;numAdverbsAnswer;numPPsAnswer;numSubordinateClausesAnswer;numConjunctionsAnswer;numPronounsAnswer;numVagueNPsSource;numVagueNPsQuestion;numVagueNPsAnswer;numLeadingModifiersQuestion";
            final String[] names = GlobalProperties.getProperties().getProperty("featureNames", defaultFeatureNames).split(";");

            final boolean includeGreaterThanFeatures = new Boolean(GlobalProperties.getProperties().getProperty("includeGreaterThanFeatures", "true"));

            Arrays.sort(names);

            for (final String name : names) {
                featureNames.add(name);
                if (includeGreaterThanFeatures && name.matches("num.+")) {
                    for (int j = 0; j < 5; j++) {
                        featureNames.add(name + "GreaterThan" + j);
                    }
                } else if (includeGreaterThanFeatures && name.matches("length.+")) {
                    for (int j = 0; j < 32; j += 4) {
                        featureNames.add(name + "GreaterThan" + j);
                    }
                }
            }

            Collections.sort(featureNames);

        }

        return featureNames;
    }

    public void setSourceTree(final Tree sourceTree) {
        this.sourceTree = sourceTree;
    }

    public Tree getSourceTree() {
        return sourceTree;
    }

    public List<Double> featureValueList() {
        if (featureValueList == null) {
            if (featureMap != null) {
                featureValueList = createFeatureValueList(featureMap);
            }
        }
        return featureValueList;
    }

    public double getFeatureValue(final String featureName) {
        Double val = featureMap.get(featureName);
        if (val == null) {
            val = 0.0;
        }
        return val.doubleValue();
    }

    public List<Tree> findLogicalWordsAboveIntermediateTree() {
        final List<Tree> res = new ArrayList<Tree>();

        final Tree pred = intermediateTree.getChild(0).headPreTerminal(AnalysisUtilities.getHeadFinder());
        final String lemma = AnalysisUtilities.getLemma(pred.yield().toString(), pred.label().toString());

        String tregexOpStr;
        TregexPattern matchPattern;
        TregexMatcher matcher;

        Tree sourcePred = null;
        for (final Tree leaf : sourceTree.getLeaves()) {
            final Tree tmp = leaf.parent(sourceTree);
            final String sourceLemma = AnalysisUtilities.getLemma(leaf.label().toString(), tmp.label().toString());
            if (sourceLemma.equals(lemma)) {
                sourcePred = tmp;
                break;
            }
        }

        tregexOpStr = "RB|VB|VBD|VBP|VBZ|IN|MD|WRB|WDT|CC=command";
        matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
        matcher = matchPattern.matcher(sourceTree);

        Tree command;
        while (matcher.find() && sourcePred != null) {
            command = matcher.getNode("command");
            if (AnalysisUtilities.cCommands(sourceTree, command, sourcePred)
                    && command.parent(sourceTree) != sourcePred.parent(sourceTree))
            {
                res.add(command);
            }
        }

        return res;
    }

    public void setScore(final double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public void setAnswerPhraseTree(final Tree answerPhraseTree) {
        this.answerPhraseTree = answerPhraseTree;
    }

    public Tree getAnswerPhraseTree() {
        return answerPhraseTree;
    }

    public int compareTo(final Question o) {
        int res = Double.compare(score, o.getScore());
        if (res == 0) {
            res = Double.compare(o.getSourceSentenceNumber(), score);
        }
        return res;
    }

    public void setYield(final String yield) {
        this.yield = yield;
    }

    public void setFeatureValueList(final List<Double> featureValueList) {
        this.featureValueList = featureValueList;
    }

    public void setIntermediateTree(final Tree intermediateTree) {
        setIntermediateTreeSupersenses(null);
        this.intermediateTree = intermediateTree;
    }

    public Tree getIntermediateTree() {
        return intermediateTree;
    }

    public void setSourceSentenceNumber(final int sourceSentenceNumber) {
        this.sourceSentenceNumber = sourceSentenceNumber;
    }

    public int getSourceSentenceNumber() {
        return sourceSentenceNumber;
    }

    public String getSourceArticleName() {
        return sourceArticleName;
    }

    public void setSourceArticleName(final String n) {
        sourceArticleName = n;
    }

    public void setLabelScore(final double labelScore) {
        this.labelScore = labelScore;
    }

    public double getLabelScore() {
        return labelScore;
    }

    public void setIntermediateTreeSupersenses(
            final List<String> intermediateTreeSupersenses) {
        this.intermediateTreeSupersenses = intermediateTreeSupersenses;
    }

    public List<String> getIntermediateTreeSupersenses() {
        return intermediateTreeSupersenses;
    }

    private double                    score;                                   // assigned by QuestionRanker
    private double                    labelScore;                              // gold-standard label, used only during eval
    private List<Double>              featureValueList;

    private String                    yield;
    private static List<String>       featureNames;                            // list of feature names, for internal bookkeeping
    private Tree                      tree;                                    // output question parse tree
    private Tree                      sourceTree;                              // original parse of a sentence given as input to the entire system
    private Tree                      intermediateTree;                        // an optionally transformed or simplified copy of the source tree
                                                                                // (the
                                                                                // output of stage 1)
    private Tree                      answerPhraseTree;
    private final Map<String, Double> featureMap;
    private int                       sourceSentenceNumber;
    private List<String>              intermediateTreeSupersenses;

    private Object                    sourceDocument;                          // generic pointer to a document object (generic in order to avoid
                                                                                // unneeded
                                                                                // dependencies)
    private String                    sourceArticleName;

    private static final long         serialVersionUID = -1033671431880363286L;

}
