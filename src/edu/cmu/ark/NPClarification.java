package edu.cmu.ark;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import arkref.analysis.ARKref;
import arkref.analysis.FindMentions;
import arkref.analysis.RefsToEntities;
import arkref.analysis.Resolve;
import arkref.analysis.Types;
import arkref.data.Document;
import arkref.data.Mention;
import arkref.parsestuff.TregexPatternFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import edu.stanford.nlp.util.Pair;

public class NPClarification {
    public NPClarification() {
        ARKref.Opts.propertiesFile = GlobalProperties.getProperties().getProperty("propertiesFilePath");
    }

    public void resolveCoreference(final List<Tree> origdoc) {
        final List<Tree> trees = new ArrayList<Tree>();
        final List<String> entityStrings = new ArrayList<String>();

        for (final Tree t : origdoc) {
            Document.addNPsAbovePossessivePronouns(t);
            Document.addInternalNPStructureForRoleAppositives(t);
            trees.add(t);
            entityStrings.add(convertSupersensesToEntityString(t, SuperSenseWrapper.getInstance().annotateSentenceWithSupersenses(t)));
        }

        doc = new Document(trees, entityStrings);

        FindMentions.go(doc);
        Resolve.go(doc);
        RefsToEntities.go(doc);
    }

    private String convertSupersensesToEntityString(final Tree t, final List<String> supersenses) {
        String res = "";

        final List<String> converted = new ArrayList<String>();
        for (int i = 0; i < supersenses.size(); i++) {
            if (supersenses.get(i).endsWith("noun.person")) {
                converted.add("PERSON");
            } else {
                converted.add(supersenses.get(i));
            }
        }

        final List<Tree> leaves = t.getLeaves();
        while (leaves.size() > converted.size()) {
            converted.add("0");
        }
        for (int i = 0; i < leaves.size(); i++) {
            if (i > 0) {
                res += " ";
            }
            res += leaves.get(i) + "/" + converted.get(i);
        }

        return res;
    }

    public static boolean hasPronoun(final Tree t) {
        final TregexPattern pat = TregexPatternFactory.getPattern("/^PRP/=pronoun");
        final TregexMatcher matcher = pat.matcher(t);
        return matcher.find();
    }

    public static boolean isPronoun(final Tree t) {
        final TregexPattern pat = TregexPatternFactory.getPattern("NP !>> __ <<# /^PRP/=pronoun");
        final TregexMatcher matcher = pat.matcher(t);
        return matcher.find();
    }

    public List<Question> clarifyNPs(final List<Question> treeSet, final boolean clarifyPronouns, final boolean clarifyNonPronouns) {
        final List<Question> newTrees = new ArrayList<Question>();

        // arrays for return values from findReplacement
        final List<Boolean> retModified = new ArrayList<Boolean>();
        final List<Boolean> retResolvedPronounsIfNecessary = new ArrayList<Boolean>();
        final List<Boolean> retHadPronouns = new ArrayList<Boolean>();
        Tree replacement;

        for (final Question q : treeSet) {
            boolean modified = false;
            boolean resolvedPronounsIfNecessary = true;
            boolean hadPronouns = false;
            final Question qCopy = q.deeperCopy();
            final Tree qRoot = qCopy.getIntermediateTree();

            final List<Tree> replacedMentionTrees = new ArrayList<Tree>();
            final List<Tree> replacementMentionTrees = new ArrayList<Tree>();

            if (GlobalProperties.getDebug()) {
                System.err.println("NPClarification processing: " + qRoot.yield().toString());
            }

            // iterate over mentions in the input tree
            final List<Tree> sentenceMentionNodes = FindMentions.findMentionNodes(qRoot);
            final Set<Tree> alreadySeenNodes = new HashSet<Tree>();
            for (final Tree qMentionNode : sentenceMentionNodes) {

                if (isPronoun(qMentionNode) && !clarifyPronouns) {
                    continue;
                }
                if (!isPronoun(qMentionNode) && !clarifyNonPronouns) {
                    continue;
                }

                // if the input contains multiple instances of the same node,
                // only replace the first one (e.g., as in "He thought he could win.")
                // this works in conjunction with the later call to isFirstMentionOfEntityInSentence,
                // which handles cases like "John thought he could win" (where he = John).
                if (alreadySeenNodes.contains(qMentionNode)) {
                    continue;
                }
                alreadySeenNodes.add(qMentionNode);

                replacement = findAndReplace(qMentionNode, sentenceMentionNodes, qRoot, qCopy.getSourceSentenceNumber(), clarifyNonPronouns, retHadPronouns, retResolvedPronounsIfNecessary, retModified);

                if (replacement != null) {
                    replacedMentionTrees.add(qMentionNode);
                    replacementMentionTrees.add(replacement);
                }

                modified |= retModified.get(0);
                resolvedPronounsIfNecessary &= retResolvedPronounsIfNecessary.get(0);
                hadPronouns |= retHadPronouns.get(0);
            }

            if (modified && (!hadPronouns || hadPronouns && resolvedPronounsIfNecessary)) {
                if (GlobalProperties.getDebug()) {
                    System.err.println("NPClarification added: " + qCopy.getIntermediateTree().yield().toString());
                }

                extractClarificationFeatures(qCopy, replacedMentionTrees, replacementMentionTrees);
                newTrees.add(qCopy);

            }

            if (!modified && resolvedPronounsIfNecessary && hadPronouns) {
                if (GlobalProperties.getDebug()) {
                    System.err.println("NPClarification resolved pronouns in: " + q.getIntermediateTree().yield().toString());
                }
                // set the NPC feature for the ORIGINAL tree (we don't need to add it), not the copy
                q.setFeatureValue("performedNPClarification", 1.0);
            }
        }

        // add the newly created trees to the input set
        return newTrees;
    }

    private void extractClarificationFeatures(final Question qCopy, final List<Tree> replacedMentionTrees, final List<Tree> replacementMentionTrees) {

        qCopy.setFeatureValue("performedNPClarification", 1.0);
        String treeName;
        List<Tree> treeList;

        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                treeList = replacedMentionTrees;
                treeName = "ReplacedMentions";
            } else {
                treeList = replacementMentionTrees;
                treeName = "ReplacementMentions";
            }

            double numVagueNPs = 0.0;
            double length = 0.0;
            double numNPs = 0.0;
            double numProperNouns = 0.0;
            double numQuantities = 0.0;
            double numAdjectives = 0.0;
            double numAdverbs = 0.0;
            double numPPs = 0.0;
            double numSubordinateClauses = 0.0;
            double numConjunctions = 0.0;
            double numPronouns = 0.0;

            for (final Tree tree : treeList) {
                SpecificityAnalyzer.getInstance().analyze(tree);
                numVagueNPs += SpecificityAnalyzer.getInstance().getNumVagueNPs();
                length += tree.yield().size();
                numNPs += AnalysisUtilities.getNumberOfMatchesInTree("NP !> NP", tree);
                numProperNouns += AnalysisUtilities.getNumberOfMatchesInTree("/^NNP/", tree);
                numQuantities += AnalysisUtilities.getNumberOfMatchesInTree("CD|QP", tree);
                numAdjectives += AnalysisUtilities.getNumberOfMatchesInTree("/^JJ/", tree);
                numAdverbs += AnalysisUtilities.getNumberOfMatchesInTree("/^RB/", tree);
                numPPs += AnalysisUtilities.getNumberOfMatchesInTree("PP", tree);
                numSubordinateClauses += AnalysisUtilities.getNumberOfMatchesInTree("SBAR", tree);
                numConjunctions += AnalysisUtilities.getNumberOfMatchesInTree("CC", tree);
                numPronouns += AnalysisUtilities.getNumberOfMatchesInTree("/^PRP/", tree);
            }

            // set the features now
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numVagueNPs" + treeName, 1, 5, numVagueNPs);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "length" + treeName, 4, 10, length);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numNPs" + treeName, 1, 5, numNPs);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numProperNouns" + treeName, 1, 5, numProperNouns);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numQuantities" + treeName, 1, 5, numQuantities);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numAdjectives" + treeName, 1, 5, numAdjectives);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numAdverbs" + treeName, 1, 5, numAdverbs);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numPPs" + treeName, 1, 5, numPPs);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numSubordinateClauses" + treeName, 1, 5, numSubordinateClauses);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numConjunctions" + treeName, 1, 5, numConjunctions);
            QuestionFeatureExtractor.extractCountAndGreaterThanFeatures(qCopy, "numPronouns" + treeName, 1, 5, numPronouns);

        }

    }

    /**
     * Look in the entity graph of the document to find a replacement for mentionNode (a copy of a node from the original document).
     * Checks sentenceMentionNodes to make sure that the mention is the first in the sentence.
     * Uses sentenceRoot (a copy of the original sentence root) to find out if head's match.
     * 
     * @param mentionNode
     * @param sentenceMentionNodes
     * @param sentenceRoot
     * @param qSentNumber
     * @return
     */
    private Tree findAndReplace(final Tree mentionNode, final List<Tree> sentenceMentionNodes, final Tree sentenceRoot, final int qSentNumber, final boolean clarifyNonPronouns, final List<Boolean> retHadPronouns, final List<Boolean> retResolvedPronounsIfNecessary, final List<Boolean> retModified) {
        boolean modified = false;
        boolean resolvedPronounsIfNecessary = true;
        boolean hadPronouns = false;
        Tree replacementCopy = null;

        // iterate over document mentions to find a match
        for (int i = 0; i < doc.mentions().size(); i++) {
            replacementCopy = null;
            final Mention m = doc.mentions().get(i);
            final int mentionSentenceNum = m.getSentence().ID();

            // skip the tree if its not the same sentence as the mention
            if (mentionSentenceNum != qSentNumber) {
                continue;
            }

            // Skip if the original mention doesn't match the input mention.
            // We use case-insensitive match so that "he" equals "He", which might happen due to other transformations...
            // Of course, using the case insensitive matching like this is a bit of a hack,
            // but it's probably simpler than making sure all the input is properly cased.
            if (!caseInsensitiveNodeMatch(m.node(), mentionNode)) {
                // if(!m.node().equals(mentionNode)){
                continue;
            }

            if (Types.isPronominal(m)) {
                hadPronouns = true;
                resolvedPronounsIfNecessary = false;
            }

            // with multiple instances of the same word (e.g., "he said he did")
            // skip if the original mention has a different parent head word
            // if(!parentsHaveSameHeadWords(m.node(), m.getSentence().rootNode(), mentionNode, sentenceRoot)){
            // continue;
            // }

            // find best mention to replace this with
            final Mention replacement = findReplacementByFirstMention(m);
            // don't replace if the replacement is identical
            if (replacement.node().yield().toString().equalsIgnoreCase(mentionNode.yield().toString())) {
                continue;
            }

            // skip it if the best is a pronoun itself
            if (hasPronoun(replacement.node())) {
                continue;
            } else if (hadPronouns) {
                resolvedPronounsIfNecessary = true;
            }

            // only consider replacements for the first mention in the sentence of each entity
            if (!isFirstMentionOfEntityInSentence(m, mentionNode, sentenceMentionNodes)) {
                continue;
            }

            if (replacement.ID() != m.ID() && !replacement.node().dominates(m.node())) {
                // make copy with nested mentions replaced
                // create a copy of the node that will be used to replace
                // the node in the input tree.
                if (clarifyNonPronouns) {
                    replacementCopy = createCopyWithNestedMentionsClarified(replacement, sentenceMentionNodes);
                } else {
                    replacementCopy = replacement.node().deeperCopy();
                }
                replacementCopy = simplifyMentionTree(replacementCopy);

                if (isPossessiveNP(m.node()) && !isPossessiveNP(replacement.node())) {
                    replacementCopy.addChild(AnalysisUtilities.readTreeFromString("(POS 's)"));
                }
                if (!isPossessiveNP(m.node()) && isPossessiveNP(replacement.node())) {
                    // remove the POS node
                    final List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
                    final List<TsurgeonPattern> ps = new ArrayList<TsurgeonPattern>();
                    final TregexPattern matchPattern = TregexPatternFactory.getPattern("POS=pos");
                    ps.add(Tsurgeon.parseOperation("prune pos"));
                    final TsurgeonPattern p = Tsurgeon.collectOperations(ps);
                    ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));
                    Tsurgeon.processPatternsOnTree(ops, replacementCopy);

                }
                AnalysisUtilities.downcaseFirstToken(replacementCopy);

                // insert the copy into the input tree
                final Tree qParent = mentionNode.parent(sentenceRoot);
                // if we already replaced a parent node, skip
                if (qParent == null) {
                    continue;
                }

                qParent.setChild(qParent.indexOf(mentionNode), replacementCopy);
                modified = true;

                // don't need to consider other mentions
                break;
            }
        }

        retModified.clear();
        retModified.add(modified);
        retResolvedPronounsIfNecessary.clear();
        retResolvedPronounsIfNecessary.add(resolvedPronounsIfNecessary);
        retHadPronouns.clear();
        retHadPronouns.add(hadPronouns);

        return replacementCopy;
    }

    /**
     * Creates a copy of the node for replacement that has any nested mentions clarified.
     * Uses sentenceMentionNodes to make sure any nested mentions will not be replaced if they are the second mention in the sentence.
     * 
     * @param replacement
     *            (from the original document object)
     * @param sentenceMentionNodes
     * @param sentenceRoot
     * @param qSentNumber
     * @return
     */
    private Tree createCopyWithNestedMentionsClarified(final Mention replacement, final List<Tree> sentenceMentionNodes) {
        final Tree copy = replacement.node().deeperCopy();

        // consider all nodes dominated by the replacement
        for (final Mention other : doc.mentions()) {
            if (!replacement.node().dominates(other.node())) {
                continue;
            }

            for (final Tree t : copy) {
                if (t.equals(other.node())) {
                    findAndReplace(t, sentenceMentionNodes, copy, other.getSentence().ID(), true, new ArrayList<Boolean>(), new ArrayList<Boolean>(), new ArrayList<Boolean>());
                }
            }
        }

        return copy;
    }

    /**
     * return true if one of the other mentions in the sentence (qMentionNodes)
     * is in the list of linked mentions for the given mention m
     * AND if this other mention comes before m.
     * Else, return false.
     * 
     * @param mentionInOriginalDocument
     * @param mentionNodesInCurrentSentence
     * @return
     */
    private boolean isFirstMentionOfEntityInSentence(final Mention mentionInOriginalDocument, final Tree mentionNodeInCurrentSentence, final List<Tree> mentionNodesInCurrentSentence) {
        final int mentionIndex = mentionNodesInCurrentSentence.indexOf(mentionNodeInCurrentSentence);

        // iterate over mentions that are linked with the given mention m
        for (final Mention linkedMention : doc.entGraph().getLinkedMentions(mentionInOriginalDocument)) {

            // skip the linked mention if it is not in the same sentence
            if (linkedMention.getSentence().ID() != mentionInOriginalDocument.getSentence().ID()) {
                continue;
            }

            // iterate through all the mentions before the given
            // mentionNode in the current (possibly transformed) sentence.
            // if the linked mention matches a tree object,
            // then return false to indicate that the given mention node is
            // not the first mention of its entity (because linkedmention is).
            final Tree linkedMentionHead = linkedMention.getHeadNode();
            for (int i = 0; i < mentionIndex; i++) {
                if (caseInsensitiveNodeMatch(linkedMentionHead, mentionNodesInCurrentSentence.get(i).headTerminal(AnalysisUtilities.getHeadFinder())))
                {
                    return false;
                }
            }

        }

        return true;
    }

    private Mention findReplacementByFirstMention(final Mention m) {
        Mention res = m;
        int minID = m.ID();
        for (final Mention other : doc.entGraph().getLinkedMentions(m)) {
            if (other.ID() < minID) {
                res = other;
                minID = other.ID();
            }
        }

        return res;
    }

    public Document getDocument() {
        return doc;
    }

    public static boolean isPossessiveNP(final Tree tree) {
        final String patS = "NP=parentnp [ < /^PRP\\$/ | < POS ] !> __";
        final TregexPattern pat = TregexPatternFactory.getPattern(patS);
        final TregexMatcher matcher = pat.matcher(tree);
        return matcher.find();
    }

    /**
     * remove non-restrictive appositives and non-restrictive relative clauses
     * 
     * @param input
     * @return
     */
    public Tree simplifyMentionTree(final Tree input) {
        String tregexOpStr;
        TregexPattern matchPattern;
        final Tree res = input;

        // if the head is a proper noun, return the NP subtree dominating the head
        Tree newHead = input.headPreTerminal(AnalysisUtilities.getHeadFinder());

        boolean hasCommaSubtree = false;
        boolean hasParenthesesSubtree = false;
        for (final Tree subtree : input.getChildrenAsList()) {
            if (subtree.label().toString().equals(",")) {
                hasCommaSubtree = true;
            }
        }

        List<TsurgeonPattern> ps = new ArrayList<TsurgeonPattern>();
        tregexOpStr = "NP < (PRN=paren $ __)";
        matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
        if (matchPattern.matcher(input).find()) {
            hasParenthesesSubtree = true;

            final List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
            TsurgeonPattern p;
            ps.add(Tsurgeon.parseOperation("prune paren"));
            p = Tsurgeon.collectOperations(ps);
            ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));
            Tsurgeon.processPatternsOnTree(ops, input);

        }

        // remove parenthesis
        tregexOpStr = "__=parenthetical $, /-LRB-/=leadingpunc $. /-RRB-/=trailingpunc";
        matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
        if (matchPattern.matcher(input).find()) {
            hasParenthesesSubtree = true;
            final List<Pair<TregexPattern, TsurgeonPattern>> ops = new ArrayList<Pair<TregexPattern, TsurgeonPattern>>();
            ps = new ArrayList<TsurgeonPattern>();
            ps.add(Tsurgeon.parseOperation("prune leadingpunc"));
            ps.add(Tsurgeon.parseOperation("prune parenthetical"));
            ps.add(Tsurgeon.parseOperation("prune trailingpunc"));
            final TsurgeonPattern p = Tsurgeon.collectOperations(ps);
            ops.add(new Pair<TregexPattern, TsurgeonPattern>(matchPattern, p));

            Tsurgeon.processPatternsOnTree(ops, input);
        }

        if (!hasCommaSubtree && !hasParenthesesSubtree) {
            return input;
        }

        // if there is a comma, choose the subtree that has the proper noun as the head
        if (!newHead.label().toString().equals("NNP") && !newHead.label().toString().equals("NNPS")) {

            tregexOpStr = "__=mention !> __ < /,/=comma << NP=np";

            matchPattern = TregexPatternFactory.getPattern(tregexOpStr);
            final TregexMatcher m = matchPattern.matcher(input);
            Tree np;
            while (m.find()) {
                np = m.getNode("np");
                final Tree subtreeHead = np.headPreTerminal(AnalysisUtilities.getHeadFinder());
                if (subtreeHead.label().toString().equals("NNP") || subtreeHead.label().toString().equals("NNPS")) {
                    newHead = subtreeHead;
                    break;
                }
            }
        }

        for (final Tree subtree : input.getChildrenAsList()) {
            if (subtree.dominates(newHead)) {
                return subtree;
            }
        }

        return res;
    }

    public static boolean caseInsensitiveNodeMatch(final Tree n1, final Tree n2) {
        return n1.toString().equalsIgnoreCase(n2.toString());
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        try {
            final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            GlobalProperties.setDebug(true);
            // ARKref.Opts.debug = true;

            if (GlobalProperties.getDebug()) {
                System.err.println("\nInput Text:");
            }
            String doc;

            while (true) {
                doc = "";
                String buf = "";

                buf = br.readLine();
                if (buf == null) {
                    break;
                }
                doc += buf;

                while (br.ready()) {
                    buf = br.readLine();
                    if (buf == null) {
                        break;
                    }
                    doc += buf + " ";
                }
                if (doc.length() == 0) {
                    break;
                }

                final List<String> sentences = AnalysisUtilities.getSentences(doc);
                final List<Question> questions = new ArrayList<Question>();
                final List<Tree> trees = new ArrayList<Tree>();

                int sentenceNum = 0;
                for (final String s : sentences) {
                    final Tree t = AnalysisUtilities.parseSentence(s).getTree();
                    trees.add(t);

                    final Question q = new Question();
                    q.setSourceTree(t);
                    q.setSourceSentenceNumber(sentenceNum);
                    q.setIntermediateTree(t);
                    questions.add(q);

                    sentenceNum++;
                }

                final NPClarification npc = new NPClarification();
                npc.resolveCoreference(trees);

                // Normally, we would perform some transformations right here,
                // but for this class we just print out clarified versions of the original sentences.

                final List<Question> newTrees = npc.clarifyNPs(questions, true, true);

                for (final Question q : newTrees) {
                    System.out.println(q.getIntermediateTree().yield().toString());
                }

            }

        } catch (final Exception e) {
            e.printStackTrace();
        }

    }

    private Document doc;

}
