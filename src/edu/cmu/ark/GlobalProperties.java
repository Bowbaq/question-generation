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
import java.io.FileInputStream;
import java.util.Properties;

public class GlobalProperties {
    private static Properties   properties;
    private static final String default_properties_file_path = "config" + File.separator + "QuestionTransducer.properties";

    private static boolean      is_debug;
    private static boolean      compute_features             = true;

    public static Properties getProperties() {
        if (properties == null) {
            loadProperties(default_properties_file_path);
        }
        return properties;
    }

    public static void loadProperties(final String path) {
        if (!new File(path).exists()) {
            System.err.println("[Properties] file not found at the location, " + path + ".  Please specify with --properties PATH.");
            System.exit(-1);
        }

        properties = new Properties();
        try {
            properties.load(new FileInputStream(path));
        } catch (final Exception e) {
            System.err.println("[Properties] failed reading configuration file");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void setDebug(final boolean debug) {
        is_debug = debug;
    }

    public static boolean isDebug() {
        return is_debug;
    }

    public static void setComputeFeatures(final boolean flag) {
        compute_features = flag;
    }

    public static boolean getComputeFeatures() {
        return compute_features;
    }
}
