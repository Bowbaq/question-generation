package edu.cmu.ark.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import edu.cmu.ark.AnalysisUtilities;
import edu.cmu.ark.data.ParseResult;
import edu.stanford.nlp.trees.Tree;

public class StanfordParserClient {
	private final String		host;
	private final int			port;

	private final ParseResult	failure	= new ParseResult(false, AnalysisUtilities.readTreeFromString("(ROOT (. .))"), Double.MIN_VALUE);

	public StanfordParserClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public ParseResult parse(String sentence) {
		String line;
		StringBuilder treestring = new StringBuilder();

		try {
			Socket client = new Socket(host, port);
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			PrintWriter out = new PrintWriter(client.getOutputStream());

			out.println(sentence);
			out.flush();

			line = in.readLine();
			if (null != line && line.equals("SUCCESS")) {
				do {
					line = in.readLine();
					if (line.startsWith("SCORE: ")) {
						Tree parse = AnalysisUtilities.readTreeFromString(treestring.toString());
						Double score = Double.parseDouble(line.substring("SCORE: ".length()));
						return new ParseResult(true, parse, score);
					}
					treestring.append(line.replaceAll("\n", "").replaceAll("\\s+", " "));
				} while (in.ready());
			} else {
				return failure;
			}

			in.close();
			out.close();
		} catch (Exception e) {
			System.err.println("Couldn't connect to Stanford parser server at " + host + ":" + port);
		}

		return null;
	}
}
