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
            final String defaultPath = "config" + File.separator + "QuestionTransducer.properties";
            loadProperties(defaultPath);
        }
        return properties;
    }

    public static void loadProperties(final String propertiesFile) {
        if (!new File(propertiesFile).exists()) {
            System.err.println("properties file not found at the location, " + propertiesFile + ".  Please specify with --properties PATH.");
            System.exit(0);
        }

        properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFile));
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(0);
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
