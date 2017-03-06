package com.pgame.game;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Command {
	SYNCROOM, ENTERROOM, SITDOWN, STANDUP, HAND, FORWARD, FINISHGAME, RESET, UNKNOWN;

	private final static Matcher commandSyncRoom = Pattern.compile(
			"^roomlist:(.+)$").matcher("");

	private final static Matcher commandHand = Pattern.compile("^hand:(.+)$")
			.matcher("");

	private final static Matcher commandSitdown = Pattern.compile(
			"^sitdown:(.+)$").matcher("");

	private final static Matcher commandStandup = Pattern.compile(
			"^standup:(.+)$").matcher("");

	private final static Matcher commandEngine = Pattern.compile(
			"^game:(.+?):(.+)\\n$").matcher("");

	private final static Matcher commandFinishGame = Pattern.compile(
			"finishgame:(.+?)").matcher("");

	private final static Matcher commandResetGame = Pattern.compile(
			"resetsync:(.+?)").matcher("");

	private String playerId;

	private String data;

	public static Command parse(String strComm) {
		if (strComm != null && strComm.length() > 0) {
			commandSyncRoom.reset(strComm);
			if (commandSyncRoom.matches()) {
				SYNCROOM.setPlayerId(commandSyncRoom.group(1));
				return SYNCROOM;
			}
			commandSitdown.reset(strComm);
			if (commandSitdown.matches()) {
				SITDOWN.setPlayerId(commandSitdown.group(1));
				return SITDOWN;
			}
			commandHand.reset(strComm);
			if (commandHand.matches()) {
				HAND.setPlayerId(commandHand.group(1));
				return HAND;
			}
			commandStandup.reset(strComm);
			if (commandStandup.matches()) {
				STANDUP.setPlayerId(commandStandup.group(1));
				return STANDUP;
			}
			commandEngine.reset(strComm);
			if (commandEngine.matches()) {
				String playerId = commandEngine.group(1);
				String data = commandEngine.group(2);
				commandFinishGame.reset(data);
				if (commandFinishGame.matches()) {
					FINISHGAME.setPlayerId(playerId);
					FINISHGAME.setData(commandFinishGame.group(1));
					return FINISHGAME;
				} else {
					FORWARD.setPlayerId(playerId);
					FORWARD.setData(data + "\n");
					return FORWARD;
				}
			}
			commandResetGame.reset(strComm);
			if (commandResetGame.matches()) {
				RESET.setPlayerId(commandResetGame.group(1));
				return RESET;
			}

		}
		return UNKNOWN;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
