package interpreterbot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramBotAdapter;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.response.GetUpdatesResponse;

public class InterpreterBot {
	
	private static TelegramBot bot;
	private static int offset = 0;
	private static Config config;
	
	public static void main(String[] args) throws Exception {
		if(args.length != 1) {
			exit("Give the configuration file as an argument");
		}
		
		readConfig(args[0]);
		
		log("[ ] Started...");
		try {
			while(true) {
				updateBot();
			}
		} finally {
			Tasks.quitAll();
		}
	}
	
	public static void readConfig(String file) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			File configFile = new File(file);
			config = mapper.readValue(configFile, Config.class);
			bot = TelegramBotAdapter.build(config.botToken);
		} catch (IOException e) {
			exit(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	public static void updateBot() {
		GetUpdatesResponse updatesResponse = bot.getUpdates(offset, 100, 0);
		List<Update> updates = updatesResponse.updates();
		
		if(!updatesResponse.isOk()) {
			log("[!] Response not OK");
			return;
		}
		
		if(updates.size() == 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { }
			return;
		}
		
		updateOffset(updates);
		
		for(Update update : updates) { handleUpdate(update); }
	}
	
	public static void updateOffset(List<Update> newestUpdates) {
		offset = newestUpdates.get(newestUpdates.size()-1).updateId()+1;
	}
	
	public static void handleUpdate(Update update) {
		Message msg = update.message();
		if(msg.text() == null || isTooOld(msg)) { return; }
		
		parseMessage(msg);
	}
	
	public static boolean isTooOld(Message msg) {
		int msgTime = msg.date();
		int nowTime = (int) Instant.now().getEpochSecond();
		int timeDiff = nowTime - msgTime;
		boolean isOld = timeDiff > config.expirationSeconds;
		if(isOld) {
			log("[ ] Didn't complete " + timeDiff + "s old request");
		}
		return isOld;
	}
	
	
	public static void parseMessage(Message msg) {
		String text = msg.text();
		if(text.equals("/start")) {
			runStart(msg.chat().id());
		} else if(text.equals("/help") || text.equals("/help" + config.botName)) {
			runHelp(msg.chat().id());
		} else {
			
			for(String command : config.commands.keySet()) {
				String invoke = "/" + command;
				String invokeWithName = invoke + config.botName;
				
				if(text.startsWith(invoke)) {
					String code = "";
					
					if(text.startsWith(invokeWithName)) {
						code = text.substring(invokeWithName.length());
					} else {
						code = text.substring((invoke).length());
					}
					
					runCode(code, config.init.get(command), config.commands.get(command), msg.chat().id());
				}
			}
		}
	}
	
	public static void runStart(long chatId) {
		bot.sendMessage(chatId, "Welcome to " + config.botName + ". Type /help for help.");
	}
	
	public static void runHelp(long chatId) {
		bot.sendMessage(chatId, "List of available interpreters:\n"
				+ config.commands.keySet().stream().collect(Collectors.joining("\n")));
	}
	
	public static void runCode(String code, String init, String command, long chatId) {
		SafeFuture<String> resultFuture = Tasks.timeout(getRunnerTask(code, init, command),
				config.codeRunTimeoutMs,
				TimeUnit.MILLISECONDS);
		
		Tasks.run(new Task<Void>(() -> {
			String output = resultFuture.get();
			for(String substring : splitMessageToChunks(output)) {
				bot.sendMessage(chatId, substring, null, null, null, null);
			}
			return null;
		},
		t -> {
			bot.sendMessage(chatId, "Error while sending output: " + t.getMessage());
			t.printStackTrace();
			return null;
		},
		() -> {
			bot.sendMessage(chatId, "Timeout while sending output");
			return null;
		}));
	}
	
	public static List<String> splitMessageToChunks(String string) {
		List<String> strings = new ArrayList<>();
		
		int lastIndex = 0;
		for(int i = 4096; i < string.length(); i+=4096) {
			strings.add(string.substring(lastIndex, i));
			lastIndex = i;
		}
		
		if(lastIndex != string.length()-1) {
			strings.add(string.substring(lastIndex, string.length()));
		}
		
		return strings;
	}
	
	public static Task<String> getRunnerTask(String input, String initCode, String interpreterCommand) {
		System.out.println("[ ] Running " + interpreterCommand);
		return new Task<>(() -> {
			ProcessBuilder bp = new ProcessBuilder(interpreterCommand.split(" "));
			bp.redirectOutput(Redirect.PIPE);
			bp.redirectInput(Redirect.PIPE);
			Process process = bp.start();
			
			try {
				BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				@SuppressWarnings("resource")
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
				
				if(initCode != null) {
					writer.write(initCode + "\n");
				}
				writer.write(input + "\n");
				writer.close();
				
				StringBuilder output = new StringBuilder();
				
				do {
					if(outReader.ready()) {
						String line = outReader.readLine();
						output.append(line + "\n");
					}
					if(errReader.ready()) {
						String line = errReader.readLine();
						output.append(line + "\n");
					}
					
				} while((!Thread.currentThread().isInterrupted()) && process.isAlive());
				
				return output.toString();
				
			} finally {
				process.destroyForcibly();
			}
		},
		throwable -> throwable.getClass().getSimpleName() + ": " + throwable.getMessage(),
		() -> "Timeout while running code");
	}
	
	public static void log(String msg) {
		System.out.println(msg);
	}
	
	public static void exit(String msg) {
		System.err.println(msg);
		System.exit(0);
	}
}