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

package edu.cmu.ark.ranking;

import java.util.*;

public interface IRanker {
    public void train(List<List<Rankable>> trainData);

    public void rank(List<Rankable> unranked);

    public void rankAll(Collection<List<Rankable>> unrankedCollections);

    public void setParameter(String key, Double value);

    public Double getParameter(String key);

    public void rank(List<Rankable> unranked, boolean doSort);

    public void rankAll(Collection<List<Rankable>> testData, boolean doSort);

}
