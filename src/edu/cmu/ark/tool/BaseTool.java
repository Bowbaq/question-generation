package edu.cmu.ark.tool;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public abstract class BaseTool {
	protected Properties	options	= new Properties();

	public BaseTool() throws FileNotFoundException, IOException {
		String path = getConfigPath();
		if (null != path) {
			options.load(new FileInputStream(path));
		}
	}

	public void setCommandLineOption(Properties command_line_args) {
		for (Object key : command_line_args.keySet()) {
			this.options.put(key, command_line_args.get(key));
		}
	}

	public String getDocumentFromStdin() {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		final StringBuilder document = new StringBuilder();

		try {
			String line = reader.readLine();
			while (line != null) {
				document.append(line).append('\n');
				line = reader.readLine();
			}
		} catch (Exception e) {
		}

		return document.length() == 0 ? null : document.toString();
	}

	abstract public String getConfigPath();

	abstract public void run();

}
