package edu.cmu.ark;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import edu.stanford.nlp.util.Pair;

/**
 * Class for generating WH phrases (e.g., which dog) from noun
 * and prepositional phrases (e.g., the big red dog).
 * This is called by the question transducer class. These two classes
 * constitute "stage 2" as discussed in the original technical report on the system.
 * 
 * @author Michael Heilman (mheilman@cs.cmu.edu)
 */
public class WhPhraseGenerator {

    public WhPhraseGenerator() {

        String[] tokens;

        leftOverPrepositions = new ArrayList<String>();
        whPhraseSubtrees = new ArrayList<String>();

        peoplePronouns = new HashSet<String>();
        partitiveConstructionHeads = new HashSet<String>();

        tokens = GlobalProperties.getProperties().getProperty("peoplePronouns", "i|he|her|him|me|she|us|we|you|myself|yourself|ourselves").split("\\|");
        for (final String token : tokens) {
            peoplePronouns.add(token);
        }

        tokens = GlobalProperties.getProperties().getProperty("partitiveConstructionHeads", "part|more|all|none|rest|much|most|some|one|many|any|either|%|percent|portion|half|third|quarter|fraction|quarter|best|worst|member|bulk|majority|minority").split("\\|");
        for (final String token : tokens) {
            partitiveConstructionHeads.add(token);
        }

    }

    /**
     * For partitive constructions (e.g., "one of my friends"),
     * this method identifies the semantic head (e.g., "friends),
     * which is used later to choose WH words (e.g., "who" instead of "what).
     * It uses a (probably not comprehensive) list of words that can be used
     * as the syntactic heads of partitive constructions (e.g., one, many, some).
     */
    protected Tree partitiveConstructionSemanticHead(final Tree np) {
        TregexPattern matchPattern;
        TregexMatcher matcher;
        final String tregexOpStr = "NP <<# DT|JJ|CD|RB|NN|JJS|JJR=syntactichead < (PP < (IN < of) < (NP <<# NN|NNS|NNP|NNPS=semantichead)) !> NP ";
        matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
        matcher = matchPattern.matcher(np);
        if (matcher.find()) {
            final Tree syntacticHead = matcher.getNode("syntactichead");
            if (partitiveConstructionHeads.contains(syntacticHead.getChild(0).label().value().toLowerCase())
                    || syntacticHead.label().value().equals("CD"))
            {
                return matcher.getNode("semantichead");
            }
        }

        return null;
    }

    public void setCurrentQuestion(final Question tmp1) {
        supersenseTags = tmp1.getIntermediateTreeSupersenses();
        if (supersenseTags == null) {
            supersenseTags = SuperSenseWrapper.getInstance().annotateSentenceWithSupersenses(tmp1.getIntermediateTree());
            tmp1.setIntermediateTreeSupersenses(supersenseTags);
        }

        sentenceTokens = new ArrayList<String>();
        final String[] origTokenArray = tmp1.getIntermediateTree().yield().toString().split("\\s");
        for (final String element : origTokenArray) {
            sentenceTokens.add(element);
        }
    }

    /**
     * This method analyzes the answer phrase to determine whether it is
     * a prepositional phrase or whether it is a partitive construction.
     * For PPs, it identifies the preposition, its object, and any modifiers of it,
     * which are used by later methods.
     * For partitive constructions, the method identifies the semantic head
     * (e.g., "friends" for "one of my friends"), which
     * is used to choose appropriate WH words.
     */
    public void setAnswer(final Tree ans, final String origSentence) {
        answerTree = ans;
        if (answerTree == null) {
            return;
        }
        final String[] answerTokenArray = answerTree.yield().toString().split("\\s");

        // find out the start and end indexes of the answer phrase in the original sentence
        // note: this fails, perhaps gracefully enough, if the same string of tokens appears twice in the sentence -- MJH
        int start = 0;
        int end = -1;
        for (; start < sentenceTokens.size(); start++) {
            for (int j = 0; j < answerTokenArray.length; j++) {
                if (!sentenceTokens.get(start + j).equalsIgnoreCase(answerTokenArray[j])) {
                    break;
                }
                if (j == answerTokenArray.length - 1) {
                    end = start + j;
                }
            }
            if (end != -1) {
                break;
            }
        }

        // extract the noun phrase out of the prepositional phrase (if necessary)
        // also, store the preposition and its adverbial modifiers (if any)
        Tree answerNP = null;
        try {
            TregexPattern matchPattern;
            TregexMatcher matcher;
            final String tregexOpStr = "PP !>> NP ?< RB|ADVP=adverb [< (IN|TO=preposition !$ IN) | < (IN=preposition $ IN=preposition2)] < NP=object";
            matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
            matcher = matchPattern.matcher(ans);
            if (matcher.find()) {
                answerNP = matcher.getNode("object");
                answerPreposition = matcher.getNode("preposition").yield().toString();
                final Tree answerPreposition2 = matcher.getNode("preposition2");
                if (answerPreposition2 != null) {
                    answerPreposition += " " + answerPreposition2.yield().toString();
                }
                answerPrepositionModifier = matcher.getNode("adverb");
            } else {
                // check if this is a partitive construction
                // e.g., for "one of my friends", the system should create
                // "who" from "friends" rather than "what" from the syntactic head "one"
                final Tree semanticHead = partitiveConstructionSemanticHead(ans);
                if (semanticHead != null) {
                    answerNP = semanticHead;
                } else {
                    answerNP = ans;
                }

                answerPreposition = "";
                answerPrepositionModifier = null;
            }
        } catch (final Exception e) {
            e.printStackTrace();
            answerPreposition = "";
            answerPrepositionModifier = null;
        }

        final List<Tree> leaves = answerTree.getLeaves();

        answerNPHeadTokenIdx = start + leaves.indexOf(answerNP.headTerminal(AnalysisUtilities.getHeadFinder()));
        headSupersenseTag = supersenseTags.get(answerNPHeadTokenIdx);
        headWord = sentenceTokens.get(answerNPHeadTokenIdx);
    }

    protected void addIfAllowedWhat(final Tree phraseToMove) {
        if (isPerson(headWord, headSupersenseTag) || isTime(headWord, headSupersenseTag)) {
            return;
        }

        // want to ask "what is a president" for sentences like "a president is a man who leads a country."
        // note: we also want to ask "who is john" for "john is a man I met yesterday"

        whPhraseSubtrees.add("(WHNP (WRB what))");
    }

    /**
     * Returns whether the given answer phrase is a definite NP.
     * Note: this method almost certainly does not exhaustively cover the indicators definiteness.
     * 
     * @param phraseToMove
     * @return
     */
    protected boolean isDefinite(final Tree phraseToMove) {
        String tregexOpStr;
        TregexPattern matchPattern;
        TregexMatcher matcher;

        // definite articles
        tregexOpStr = "NP <+(NP) (DT < the|this|that|these|those|all|no|every|both|each) !> __";
        matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
        matcher = matchPattern.matcher(phraseToMove);
        if (matcher.find()) {
            return true;
        }

        // proper nouns or pronouns are definite
        tregexOpStr = "NP <<# NNP|NNPS";
        matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
        matcher = matchPattern.matcher(phraseToMove);
        if (matcher.find())
        {
            return true;
        }

        return false;
    }

    protected void addIfAllowedWho(final Tree phraseToMove) {
        if (isPerson(headWord, headSupersenseTag)
                // || isGroup(headWord, headSupersenseTag)
                || headWord.toLowerCase().matches("^(they|them|themselves)$")) // might be a person (these aren't included in isPerson)
        {
            whPhraseSubtrees.add("(WHNP (WRB who))");
        }
    }

    protected void addIfAllowedWhen(final Tree phraseToMove) {
        if (isTime(headWord, headSupersenseTag)) {// && !answerPreposition.matches("on|in|at|over")){ // don't want "in when"
            whPhraseSubtrees.add("(WHADVP (WRB when))");
        }
    }

    protected void addIfAllowedWhere(final Tree phraseToMove) {
        // if(locationPrepositions.contains(answerPreposition) && isLocation()){
        if (answerPreposition.length() > 0 && answerPreposition.matches("on|in|at|over|to") && isLocation(headWord, headSupersenseTag)) {
            whPhraseSubtrees.add("(WHADVP (WRB where))");
        }
    }

    protected void addIfAllowedHowMany(final Tree phraseToMove) {
        String tregexOpStr;
        TregexPattern matchPattern;
        TregexMatcher matcher;

        // check if there is a quantifying phrase modifying the head noun
        // "A !>> __" means that A is the top node in the subtree
        // "A <<# B means that B is a head of A"
        tregexOpStr = "NP=top !>> __ <<# (NNS $ QP|CD=quant)";
        matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
        matcher = matchPattern.matcher(phraseToMove);

        final boolean hasQuantifier = matcher.find();

        if (hasQuantifier) {
            List<Pair<TregexPattern, TsurgeonPattern>> ops;
            TsurgeonPattern p;
            List<TsurgeonPattern> ps;
            Tree copyTree;

            // remove the quantifier
            ps = new ArrayList<TsurgeonPattern>();
            ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
            copyTree = phraseToMove.deeperCopy();
            ps.add(Tsurgeon.parseOperation("prune quant"));
            p = Tsurgeon.collectOperations(ps);
            ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));
            Tsurgeon.processPatternsOnTree(ops, copyTree);

            // remove determiners if present to avoid, e.g., "*How many the cars did I buy?")
            tregexOpStr = "NP=top !>> __ <<# (NNS $ DT=det)";
            matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
            matcher = matchPattern.matcher(phraseToMove);
            if (matcher.find()) {
                ps = new ArrayList<TsurgeonPattern>();
                ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
                ps.add(Tsurgeon.parseOperation("prune det"));
                p = Tsurgeon.collectOperations(ps);
                ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));
                Tsurgeon.processPatternsOnTree(ops, copyTree);
            }

            // result.add("(WHADJP (WRB how) (JJ many)) (NNS "+sentenceTokens.get(answerNPHeadTokenIdx)+")");
            whPhraseSubtrees.add("(WHADJP (WRB how) (JJ many)) (NP " + copyTree.toString() + ")");
        }
    }

    protected void addIfAllowedWhose(final Tree phraseToMove) {
        try {
            String tregexOpStr;
            TregexPattern matchPattern;
            TregexMatcher matcher;
            boolean isPossessive;

            tregexOpStr = "NP=np <<# (NNS|NN|NNP|NNPS=head $ (NP=possessive <<# (POS , NNS|NN|NNP|NNPS=possessor))) !>> NP";
            // tregexOpStr = "NP=np <<# (NNS|NN|NNP|NNPS=head [$ /PRP\\$/=possessive | $ (NP=possessive << POS) ]) !>> NP";
            matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
            matcher = matchPattern.matcher(phraseToMove);
            isPossessive = matcher.find();

            if (isPossessive) {
                List<Pair<TregexPattern, TsurgeonPattern>> ops;
                TsurgeonPattern p;
                List<TsurgeonPattern> ps;
                Tree copyTree;

                ps = new ArrayList<TsurgeonPattern>();
                ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();

                final String possessorToken = matcher.getNode("possessor").getChild(0).label().toString();
                final int possIndex = sentenceTokens.indexOf(possessorToken);
                if (possIndex == -1) {
                    return;
                }
                final String sst = supersenseTags.get(possIndex);
                if (!isPerson(possessorToken, sst))
                {
                    return;// && !isGroup(possessorToken, sst)) return;
                }

                // make a copy and use that
                copyTree = matcher.getNode("np").deeperCopy();
                matcher = matchPattern.matcher(copyTree);
                matcher.find();

                ps.add(Tsurgeon.parseOperation("prune possessive"));
                p = Tsurgeon.collectOperations(ps);
                ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));
                Tsurgeon.processPatternsOnTree(ops, copyTree);

                whPhraseSubtrees.add("(WHNP (WP$ whose) " + copyTree.toString() + ")");
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Primary method called by the stage 2 question transducer to generate
     * WH words and phrases (e.g., what, who, when) from a given answer phrase.
     * The possible answer phrases, along with prepositions that should be left in
     * place in the output questions, are stored in lists that are fields of this class
     * and then later accessed by the question transducer class.
     */
    public void generateWHPhraseSubtrees(final Tree phraseToMove, final String inputTreeYield) {
        // clear the lists that store the possible question phrases created by this method
        leftOverPrepositions.clear();
        whPhraseSubtrees.clear();

        setAnswer(phraseToMove, inputTreeYield);

        if (GlobalProperties.getDebug()) {
            System.err.println("getWHPhraseSubtrees: phraseToMove: " + phraseToMove.toString());
        }

        // identify what question types to create from this answer phrase
        addIfAllowedWhat(phraseToMove);
        addIfAllowedWhen(phraseToMove);
        addIfAllowedWhere(phraseToMove);
        addIfAllowedWho(phraseToMove);
        addIfAllowedWhose(phraseToMove);
        addIfAllowedHowMany(phraseToMove);

        // construct the final WH phrases depending on whether the answer was a NP or PP
        String phrase;
        String prepositionModifierStr;
        final int length = whPhraseSubtrees.size();
        for (int i = 0; i < length; i++) {
            phrase = whPhraseSubtrees.get(i);
            prepositionModifierStr = "";
            whPhraseSubtrees.set(i, "(WHNP " + phrase + ")");
            if (answerPreposition.length() == 0 || phrase.equals("(WHADVP (WRB when))") || phrase.equals("(WHADVP (WRB where))")) {
                // this condition handles answer phrases that are not prepositional phrases
                // AND prepositional phrases where we remove the preposition and have a "where" or "when" question
                // e.g., John went TO JAPAN -> WHERE did John go? (no preposition)
                leftOverPrepositions.add(null);
            } else {
                // this condition handles WH phrases that came from prepositional phrases
                // for which we want to keep the preposition in its original place
                // e.g., John gave the book to Mary -> WHO did John give the book TO?

                // the prepositionModifierStr is for keeping adverb phrases that
                // modify prepositional phrases
                // e.g., John left IMMEDIATELY after the meeting -> What did John leave IMMEDIATELY after?
                // Note: such modifiers could probably just be dropped to simplify things...
                if (answerPrepositionModifier != null) {
                    prepositionModifierStr = answerPrepositionModifier.yield().toString();
                }

                leftOverPrepositions.add("(PP " + prepositionModifierStr + " (IN " + answerPreposition + "))");
            }
        }
    }

    protected boolean isTime(final String word, final String sst) {
        if (sst.endsWith("noun.time")) {
            return true;
        }

        // special case for years 1000-present (which are fairly common)
        if (word.matches("[1|2]\\d\\d\\d")) {
            return true;
        }

        return false;
    }

    protected boolean isLocation(final String word, final String sst) {
        if (sst.endsWith("noun.location")) {
            return true;
        }
        return false;
    }

    protected boolean isGroup(final String word, final String sst) {
        if (sst.endsWith("noun.group")) {
            return true;
        }
        return false;
    }

    protected boolean isPerson(final String word, final String sst) {
        if (peoplePronouns.contains(word.toLowerCase())) {
            return true;
        }
        if (sst.endsWith("noun.person")) {
            return true;
        }
        return false;
    }

    public boolean isFirstTokenNamedEntity() {
        final String firstLabel = supersenseTags.get(0);
        if (firstLabel.equals("O")) {
            return false;
        } else {
            return true;
        }
    }

    public List<String> getLeftOverPrepositions() {
        return leftOverPrepositions;
    }

    public List<String> getWHPhraseSubtrees() {
        return whPhraseSubtrees;
    }

    private Tree               answerTree;                // current answer tree that is being processed
    private List<String>       supersenseTags;            // supersense tags for the sentence that is being processed
    private List<String>       sentenceTokens;
    private int                answerNPHeadTokenIdx;
    private String             headWord;
    private String             headSupersenseTag;
    private String             answerPreposition;
    private Tree               answerPrepositionModifier;
    private final Set<String>  peoplePronouns;            // list of personal pronouns to consider as PERSON entities
    private final List<String> leftOverPrepositions;
    private final List<String> whPhraseSubtrees;
    private final Set<String>  partitiveConstructionHeads; // words that can be the syntactic heads of partitive constructions (e.g., ONE of the most
    // prolific quarterbacks of all time)

}
