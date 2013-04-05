package edu.cmu.ark.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.lexparser.Options;
import edu.stanford.nlp.trees.Tree;

public class StanfordParserServer {
	private final LexicalizedParser	parser;

	public StanfordParserServer(String grammar_path, int max_length) {
		parser = new LexicalizedParser(grammar_path, new Options());
		parser.setMaxLength(max_length);
		parser.setOptionFlags("-outputFormat", "oneline");
	}

	public void serve(int port) {
		try {
			ServerSocket server = new ServerSocket(port);
			System.out.println("Server is running on port " + port);
			while (true) {
				Socket client = server.accept();

				BufferedReader in = new BufferedReader(new InputStreamReader(new DataInputStream(client.getInputStream())));
				PrintWriter out = new PrintWriter(new PrintStream(client.getOutputStream()));

				StringBuilder sentence = new StringBuilder();
				do {
					sentence.append(in.readLine());
				} while (in.ready());

				try {
					parser.parse(sentence.toString());

					Tree best_parse = parser.getBestParse();
					parser.getTreePrint().printTree(best_parse, out);
					out.println(parser.getPCFGScore());

					/*
					 * out.println();
					 * 
					 * for (Dependency d : parser.getBestDependencyParse().dependencies()) { out.println(d.toString()); }
					 */

				} catch (Exception badparse) {
					out.println("(ROOT (. .))");
					out.println("-999999999.0");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
