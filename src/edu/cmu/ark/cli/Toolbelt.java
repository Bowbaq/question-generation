package edu.cmu.ark.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.cmu.ark.GlobalProperties;
import edu.cmu.ark.tool.BaseTool;
import edu.cmu.ark.tool.QuestionAskerTool;
import edu.cmu.ark.tool.StanfordParserServerTool;

public class Toolbelt {
	private final static ArrayList<String>	available_tools	= new ArrayList<String>();

	static {
		Toolbelt.available_tools.add("question-asker");
		Toolbelt.available_tools.add("supersense-tagger-server");
		Toolbelt.available_tools.add("supersense-tagger-client");
		Toolbelt.available_tools.add("stanford-parser-server");
		Toolbelt.available_tools.add("stanford-parser-client");
	}

	/**
	 * @param args
	 * @throws ParseException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws ParseException, FileNotFoundException, IOException {
		String tool = null;

		if (args.length > 0) {
			tool = args[0];
		} else {
			System.out.println("usage: ./scripts/run.sh <tool> [OPTIONS]");
			System.err.println("Valid tools are :");
			for (String valid : Toolbelt.available_tools) {
				System.err.println("\t" + valid);
			}
			System.exit(-1);
		}

		if (!Toolbelt.available_tools.contains(tool)) {
			System.err.println("Invalid tool. Valid tools are :");
			for (String valid : Toolbelt.available_tools) {
				System.err.println("\t" + valid);
			}
			System.exit(-1);
		}

		Options options = Toolbelt.buildOptions(tool);
		options.addOption(new Option("h", "help", false, "print this message"));
		options.addOption(new Option("v", "verbose", false, "enable verbose logging"));
		options.addOption(new Option(null, "debug", false, "enable debugging output"));

		CommandLineParser parser = new BasicParser();
		CommandLine cli = parser.parse(options, args);

		if (cli.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			PrintWriter writter = new PrintWriter(System.out);

			System.out.println("usage: ./scripts/run.sh " + tool + " [OPTIONS]");
			formatter.printOptions(writter, formatter.getWidth(), options, formatter.getLeftPadding(), formatter.getDescPadding());
			writter.flush();

			System.exit(0);
		}

		if (cli.hasOption("debug")) {
			GlobalProperties.setDebug(true);
		}

		Properties properties = new Properties();
		for (Option opt : cli.getOptions()) {
			if (null != opt.getValue()) {
				properties.setProperty(opt.getLongOpt(), opt.getValue());
			} else {
				properties.setProperty(opt.getLongOpt(), "true");
			}
		}

		Toolbelt.runTool(tool, properties);
	}

	private static void runTool(String name, Properties options) throws FileNotFoundException, IOException {
		BaseTool tool = Toolbelt.instantiateTool(name);
		tool.setCommandLineOption(options);
		tool.run();
	}

	private static BaseTool instantiateTool(String tool) throws FileNotFoundException, IOException {
		if (tool.equals("question-asker")) {
			return new QuestionAskerTool();
		}

		if (tool.equals("stanford-parser-server")) {
			return new StanfordParserServerTool();
		}

		return null;
	}

	private static Options buildOptions(String tool) {
		if (tool.equals("question-asker")) {
			return Toolbelt.buildQuestionAskerOptions();
		}

		if (tool.equals("stanford-parser-server")) {
			return Toolbelt.buildStanfordParserServerOptions();
		}

		return new Options();
	}

	private static Options buildStanfordParserServerOptions() {
		Options options = new Options();

		options.addOption(new Option("l", "max-length", false, "Maximum sentence length"));
		options.addOption(new Option("g", "grammar", false, "Specify grammar file path"));
		options.addOption(new Option("p", "port", false, "Run the server on a specific port"));

		return options;
	}

	private static Options buildQuestionAskerOptions() {
		Options options = new Options();

		options.addOption(new Option(null, "keep-pronouns", false, "TODO: Not sure what that does"));
		options.addOption(new Option(null, "downweight-pronouns", false, "TODO: Not sure what that does"));
		options.addOption(new Option(null, "do-non-pronoun-npc", false, "TODO: Not sure what that does"));
		options.addOption(new Option(null, "do-pronoun-npc", false, "TODO: Not sure what that does"));

		options.addOption(new Option(null, "avoid-frequent-answers", false, "TODO: Not sure what that does"));

		options.addOption(new Option(null, "prefer-wh-questions", false, "TODO: Not sure what that does"));
		options.addOption(new Option(null, "wh-questions-only", false, "TODO: Not sure what that does"));

		options.addOption(new Option(null, "no-stemming", false, "TODO: Not sure what that does"));

		options.addOption(new Option(null, "ranking-model-path", false, "TODO: Not sure what that does"));
		options.addOption(new Option(null, "max-question-length", false, "TODO: Not sure what that does"));

		return options;
	}

}
