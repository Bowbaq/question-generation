package edu.cmu.ark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.stanford.nlp.trees.Tree;

/**
 * Used in an optional ranking step to discount questions that
 * include very common nouns that appear in the text
 * (such questions may be ``obvious'').
 * 
 * @author mheilman
 */
public class BagOfWordsExtractor {
    private final Set<String>          stopWordList;
    private static BagOfWordsExtractor instance;

    private BagOfWordsExtractor() {
        stopWordList = loadStopList(GlobalProperties.getProperties().getProperty("stopWordList", "config" + File.separator + "stopWordList.txt"));
    }

    public static BagOfWordsExtractor getInstance() {
        if (instance == null) {
            instance = new BagOfWordsExtractor();
        }
        return instance;
    }

    public List<String> extractNounTokensFromTrees(final List<Tree> parsedSentences) {
        final List<String> res = new ArrayList<String>();

        for (final Tree sentence : parsedSentences) {
            res.addAll(extractNounTokens(sentence));
        }

        return res;
    }

    public List<String> extractNounTokens(final Tree parsedSentence) {
        final List<String> res = new ArrayList<String>();

        for (final Tree leaf : parsedSentence.getLeaves()) {
            final String word = leaf.label().toString().toLowerCase();
            if (stopWordList.contains(word)) {
                continue;
            }
            final Tree pos = leaf.parent(parsedSentence);
            if (pos.label().toString().matches("^N.*")) {
                res.add(word);
            }
        }

        return res;
    }

    private static Set<String> loadStopList(final String stoplist) {
        String buf;
        final Set<String> res = new HashSet<String>();
        try {
            final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(stoplist)));
            while ((buf = br.readLine()) != null) {
                if (buf.length() > 0) {
                    res.add(buf.toLowerCase());
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public Map<String, Double> extractCounts(final List<String> wordTokens) {
        return extractCounts(wordTokens, true);
    }

    public Map<String, Double> extractCounts(final List<String> wordTokens, final boolean doStemming) {
        final Map<String, Double> res = new HashMap<String, Double>();

        String w;

        for (final String tok : wordTokens) {
            if (doStemming) {
                w = PorterStemmer.getInstance().stem(tok);
            } else {
                w = tok;
            }

            Double tmp = res.get(w);
            if (tmp == null) {
                tmp = new Double(0);
            }
            // tmp += 1.0/(double)wordTokens.size() * -1.0*lm.unigramLogBase10Probability(tok);
            tmp += 1.0;
            res.put(w, tmp);
        }

        return res;
    }

    // @formatter:off
    public static final Comparator<Map.Entry<String, Double>> wordCountEntriesSorter = new Comparator<Map.Entry<String, Double>>() {
        public int compare(final Entry<String, Double> o1, final Entry<String, Double> o2) {
            int res = o1.getValue().compareTo(o2.getValue());
            if (res == 0) {
                res = Double.compare(o1.getKey().hashCode(), o2.getKey().hashCode());
            }
            return res * -1; // descending order
        }
    };
    // @formatter:on
}
