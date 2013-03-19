package edu.cmu.ark;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.trees.Tree;

public class InitialTransformationStep {
    private final SentenceSimplifier simplifier      = new SentenceSimplifier();
    private final NPClarification    npc             = new NPClarification();
    private boolean                  doPronounNPC    = true;
    private boolean                  doNonPronounNPC = false;

    public InitialTransformationStep() {
        simplifier.setExtractFromVerbComplements(false);
        simplifier.setBreakNPs(false);
    }

    public List<Question> transform(final List<Tree> sentences) {
        final List<Question> questions = new ArrayList<Question>();

        if (doPronounNPC || doNonPronounNPC) {
            npc.resolveCoreference(sentences);
        }

        List<Question> simplified_questions;

        // extract simplifications for each input sentence and record their input sentence numbers
        int sentence_index = 0;
        for (final Tree sentence : sentences) {
            if (!AnalysisUtilities.filterOutSentenceByPunctuation(sentence.yield().toString())) {
                simplified_questions = simplifier.simplify(sentence, false);
                for (final Question question : simplified_questions) {
                    question.setSourceSentenceNumber(sentence_index);
                    if (doPronounNPC || doNonPronounNPC) {
                        question.setSourceDocument(npc.getDocument());
                    }
                }
                questions.addAll(simplified_questions);
            }

            sentence_index++;
        }

        // add new sentences with clarified/resolved NPs
        if (doPronounNPC) {
            questions.addAll(npc.clarifyNPs(questions, doPronounNPC, doNonPronounNPC));
        }

        // upcase the first tokens of all output trees.
        for (final Question question : questions) {
            AnalysisUtilities.upcaseFirstToken(question.getIntermediateTree());
        }

        return questions;
    }

    public void setDoPronounNPC(final boolean flag) {
        doPronounNPC = flag;
    }

    public boolean doingNonPronounNPC() {
        return doNonPronounNPC;
    }

    public void setDoNonPronounNPC(final boolean flag) {
        doNonPronounNPC = flag;
    }
}
