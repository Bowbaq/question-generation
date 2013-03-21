package edu.cmu.ark;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.trees.Tree;

public class SuperSenseWrapper {
    private SuperSenseWrapper() {
        sst = null;

        DiscriminativeTagger.loadProperties(GlobalProperties.getProperties().getProperty("propertiesFilePath", "config" + File.separator + "QuestionTransducer.properties"));

    }

    public static SuperSenseWrapper getInstance() {
        if (instance == null) {
            instance = new SuperSenseWrapper();
        }
        return instance;
    }

    private LabeledSentence generateSupersenseTaggingInput(final Tree sentence) {
        final LabeledSentence res = new LabeledSentence();
        final List<Tree> leaves = sentence.getLeaves();

        for (int i = 0; i < leaves.size(); i++) {
            final String word = leaves.get(i).label().toString();
            final Tree preterm = leaves.get(i).parent(sentence);
            final String pos = preterm.label().toString();
            final String stem = AnalysisUtilities.getLemma(word, pos);
            res.addToken(word, stem, pos, "0");
        }

        return res;
    }

    public List<String> annotateMostFrequentSenses(final Tree sentence) {
        final int numleaves = sentence.getLeaves().size();
        if (numleaves <= 1) {
            return new ArrayList<String>();
        }
        final LabeledSentence labeled = generateSupersenseTaggingInput(sentence);
        labeled.setMostFrequentSenses(SuperSenseFeatureExtractor.getInstance().extractFirstSensePredictedLabels(labeled));
        return labeled.getMostFrequentSenses();
    }

    public List<String> annotateSentenceWithSupersenses(final Tree sentence) {
        final List<String> result = new ArrayList<String>();

        final int numleaves = sentence.getLeaves().size();
        if (numleaves <= 1) {
            return result;
        }
        final LabeledSentence labeled = generateSupersenseTaggingInput(sentence);

        // see if a NER socket server is available
        final int port = new Integer(GlobalProperties.getProperties().getProperty("supersenseServerPort", "5555"));
        final String host = "127.0.0.1";
        Socket client;
        PrintWriter pw;
        BufferedReader br;
        String line;
        try {
            client = new Socket(host, port);

            pw = new PrintWriter(client.getOutputStream());
            br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String inputStr = "";
            for (int i = 0; i < labeled.length(); i++) {
                final String token = labeled.getTokens().get(i);
                final String stem = labeled.getStems().get(i);
                final String pos = labeled.getPOS().get(i);
                inputStr += token + "\t" + stem + "\t" + pos + "\n";
            }
            pw.println(inputStr);
            pw.flush(); // flush to complete the transmission

            while ((line = br.readLine()) != null) {
                final String[] parts = line.split("\\t");
                if (parts.length == 1) {
                    continue;
                }
                result.add(parts[2]);
            }
            br.close();
            pw.close();
            client.close();

        } catch (final Exception ex) {
            if (GlobalProperties.isDebug()) {
                System.err.println("Could not connect to SST server.");
            }
        }

        // if socket server not available, then use a local NER object
        if (result.size() == 0) {
            try {
                if (sst == null) {
                    sst = DiscriminativeTagger.loadModel(GlobalProperties.getProperties().getProperty("supersenseModelFile", "config" + File.separator + "supersenseModelAllSemcor.ser.gz"));
                }
                sst.findBestLabelSequenceViterbi(labeled, sst.getWeights());
                for (final String pred : labeled.getPredictions()) {
                    result.add(pred);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        // add a bunch of blanks if necessary
        while (result.size() < numleaves) {
            result.add("0");
        }

        if (GlobalProperties.isDebug()) {
            System.err.println("annotateSentenceSST: " + result);
        }
        return result;
    }

    private DiscriminativeTagger     sst;
    private static SuperSenseWrapper instance;

}
