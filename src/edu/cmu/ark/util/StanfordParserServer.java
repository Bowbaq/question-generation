package edu.cmu.ark.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import edu.cmu.ark.GlobalProperties;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.lexparser.Options;

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
			System.err.println("Server is running on port " + port);
			while (true) {
				Socket client = server.accept();

				BufferedReader in = new BufferedReader(new InputStreamReader(new DataInputStream(client.getInputStream())));
				PrintWriter out = new PrintWriter(new PrintStream(client.getOutputStream()));

				StringBuilder sentence = new StringBuilder();
				do {
					sentence.append(in.readLine());
				} while (in.ready());

				try {
					if (parser.parse(sentence.toString())) {
						out.println("SUCCESS");
						parser.getTreePrint().printTree(parser.getBestParse(), out);

						if (GlobalProperties.isDebug()) {
							System.err.println(parser.getBestParse());
						}

						out.println("SCORE: " + parser.getPCFGScore());
						out.flush();
					} else {
						out.println("FAILURE");
					}

					/*
					 * out.println();
					 * 
					 * for (Dependency d : parser.getBestDependencyParse().dependencies()) { out.println(d.toString()); }
					 */

				} catch (Exception badparse) {
					out.println("FAILURE");
				}

				in.close();
				out.close();
			}
		} catch (IOException e) {
			System.err.println("Error starting up Stanford parser server on port " + port);
		}
	}
}
