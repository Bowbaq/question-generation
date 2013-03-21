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

package edu.cmu.ark.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;

public class VerbConjugator {

    /**
     * Map from lemma+pos to surface form (e.g., walk+VBZ => walks)
     */
    private static Map<String, String> conjugations          = new HashMap<String, String>();
    private static Map<String, Long>   base_form_counts      = new HashMap<String, Long>();

    private static String              jwnl_config_file_path = "config" + File.separator + "file_properties.xml";

    /**
     * Initialize JWNL
     */
    static {
        try {
            JWNL.initialize(new FileInputStream(jwnl_config_file_path));
        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final JWNLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void readFromTreebankFile(final String path) {
        try {
            final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            final Pattern p = Pattern.compile("\\((VB\\w*) (\\w+)\\)");
            String buf;
            while ((buf = br.readLine()) != null) {
                final Matcher m = p.matcher(buf);
                while (m.find()) {
                    final String pos = m.group(1);
                    final String token = m.group(2);
                    if (pos.equals("VB")) {
                        Long count = base_form_counts.get(token);
                        if (count == null) {
                            count = new Long(0);
                        }
                        count++;
                        base_form_counts.put(token, count);
                    } else {
                        String lemma = "";
                        try {
                            final IndexWord iw = Dictionary.getInstance().lookupIndexWord(POS.VERB, token);
                            if (iw == null) {
                                continue;
                            }
                            lemma = iw.getLemma();
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }

                        final String key = lemma + "/" + pos;
                        System.err.println("adding\t" + key + "\t" + token);
                        conjugations.put(key, token);
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void serialize_conjugations(final String filePath) {
        try {
            final PrintWriter pw = new PrintWriter(new FileOutputStream(filePath));
            for (final Map.Entry<String, String> entry : conjugations.entrySet()) {
                final String key = entry.getKey();
                final String[] parts = key.split("/");
                final String token = entry.getValue();
                pw.println(parts[0] + "\t" + parts[1] + "\t" + token);
            }

            pw.println("*");

            for (final Map.Entry<String, Long> entry : base_form_counts.entrySet()) {
                final String key = entry.getKey();
                final Long count = entry.getValue();
                pw.println(key + "\t" + count);
            }

            pw.flush();
            pw.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void load_conjugations(final String path) {
        try {
            String buf;
            int state = 0;
            final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

            while ((buf = br.readLine()) != null) {
                if (buf.equals("*")) {
                    state++;
                    continue;
                }
                if (state == 0) {
                    final String[] parts = buf.split("\\t");
                    if (parts.length != 3) {
                        continue;
                    }
                    final String key = parts[0].toLowerCase() + "/" + parts[1];
                    final String token = parts[2].toLowerCase();
                    conjugations.put(key, token);
                } else if (state == 1) {
                    final String[] parts = buf.split("\\t");
                    if (parts.length != 2) {
                        continue;
                    }
                    final String key = parts[0].toLowerCase();
                    final Long count = new Long(parts[1]);
                    base_form_counts.put(key, count);
                }
            }
            br.close();
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String getSurfaceForm(final String lemma, final String pos) {
        String result = new String(lemma);
        final String key = lemma + "/" + pos;
        if (conjugations.containsKey(key)) {
            result = conjugations.get(key);
        } else if (pos.equals("VBD") || pos.equals("VBZ")) {
            if (!lemma.matches("^.*[aieou]$")) {
                // char lastChar = lemma.charAt(lemma.length()-1);
                // if(lastChar == 'a'
                // || lastChar == 'e'
                // || lastChar == 'i'
                // || lastChar == 'o'
                // || lastChar == 'u')
                // {
                result += "e";
            }
            if (pos.equals("VBD")) {
                result += "d";
            } else if (pos.equals("VBZ")) {
                result += "s";
            }
        }

        return result;
    }

    public static int getBaseFormCount(final String lemma) {
        Long result = base_form_counts.get(lemma);
        if (result == null) {
            result = new Long(0);
        }

        return result.intValue();
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        VerbConjugator.readFromTreebankFile(args[0]);
        VerbConjugator.serialize_conjugations("verbConjugations.txt");

        System.err.println(VerbConjugator.getSurfaceForm("walk", "VBZ"));
        System.err.println(VerbConjugator.getSurfaceForm("walk", "VBD"));
        System.err.println(VerbConjugator.getSurfaceForm("alleviate", "VBZ"));
        System.err.println(VerbConjugator.getSurfaceForm("alleviate", "VBD"));
        System.err.println(VerbConjugator.getBaseFormCount("walk"));
        System.err.println(VerbConjugator.getBaseFormCount("alleviate"));
    }
}