package nl.idgis.publisher.console;


import java.util.function.Consumer;

import org.jolokia.client.J4pClient;

public class App {
	
	private static void printUsage() {
		System.out.println("Usage: [host] [command]");
		System.out.println("Commands: ");
		System.out.println("\tshowLoggers");
		System.out.println("\tsetLogger [logger] [loglevel]");
		System.out.println("\tresetLogger [logger]");
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length < 2) {
			printUsage();
			return;
		}
		
		String host = args[0];
		String command = args[1];
		
		final Consumer<LogbackContext> action;
		switch(command) {
			case "showLoggers":
				action = context -> {
					for(String logger : context.getLoggerList()) {
						String loggerLevel = context.getLoggerLevel(logger);
						if(!loggerLevel.isEmpty()) {
							System.out.println(logger + " " + loggerLevel);
						}
					}
				};
				break;
			case "setLogger":
				if(args.length != 4) {
					printUsage();
					return;
				} else {
					String logger = args[2];
					String logLevel = args[3];
					action = context -> {
						context.setLoggerLevel(logger, logLevel);
					};
				}
				break;
			case "resetLogger":
				if(args.length != 3) {
					printUsage();
					return;
				} else {
					String logger = args[2];
					String logLevel = "null";
					action = context -> {
						context.setLoggerLevel(logger, logLevel);
					};
				}
				break;
			default:
				System.out.println("unknown command: " + command);
				return;
		}
		
		J4pClient client = J4pClient
			.url("http://" + host + ":8778/jolokia")
			.build();
		
		LogbackContext context = LogbackContext.getSingleLogbackContext(client);
		action.accept(context);
	}	
}