package interpreterbot;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Config {
	
	public String botName = "";
	public String botToken = "";
	public long expirationSeconds = 0;
	public long codeRunTimeoutMs = 0;
	public Map<String, String> commands = new HashMap<>();
	public Map<String, String> init = new HashMap<>();
	
	public Config() {}
	
	@JsonCreator
	public Config(
				@JsonProperty("botName") String botName,
				@JsonProperty("botToken") String botToken,
				@JsonProperty("expirationSeconds") long expirationSeconds,
				@JsonProperty("codeRunTimeoutMs") long codeRunTimeoutMs,
				@JsonProperty("commands") Map<String, String> commands,
				@JsonProperty("init") Map<String, String> init
			) {
		this.botName = botName;
		this.botToken = botToken;
		this.codeRunTimeoutMs = codeRunTimeoutMs;
		this.expirationSeconds = expirationSeconds;
		this.commands = commands;
		this.init = init;
	}
}