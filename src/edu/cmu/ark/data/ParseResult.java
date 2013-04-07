package edu.cmu.ark.data;

import edu.stanford.nlp.trees.Tree;

public class ParseResult {
	private final boolean	success;
	private final Tree		parse;
	private final double	score;

	public ParseResult(final boolean success, final Tree parse, final double score) {
		this.success = success;
		this.score = score;
		this.parse = parse;

	}

	public boolean isSuccess() {
		return success;
	}

	public Tree getTree() {
		return parse;
	}

	public double getScore() {
		return score;
	}

}
