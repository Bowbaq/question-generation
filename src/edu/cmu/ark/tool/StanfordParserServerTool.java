package edu.cmu.ark.tool;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.cmu.ark.util.StanfordParserServer;

public class StanfordParserServerTool extends BaseTool {
	private final StanfordParserServer	server;
	private final int					port;

	public StanfordParserServerTool() throws FileNotFoundException, IOException {
		super();
		String grammar_path = options.getProperty("grammar_path");
		int max_length = Integer.parseInt(options.getProperty("max_sentence_length"));

		port = Integer.parseInt(options.getProperty("port"));
		server = new StanfordParserServer(grammar_path, max_length);
	}

	@Override
	public String getConfigPath() {
		return "config/stanford-parser-server.properties";
	}

	@Override
	public void run() {
		server.serve(port);
	}
}
