package com.pgame.game;

import java.util.List;

import com.pgame.game.cc.ChineseChessGame;


public class Game {

	public static enum GameType {
		ChineseChess;
	}

	private GameType type;


	public int getSeats() {
		return 0;
	}

	public int getByStanders() {
		return 0;
	}

	public void start(Desk desk) {

	}

	public GameType getGameType() {
		return type;
	}

	protected Game(GameType t) {
		this.type = t;
	}

	public static Game createGame(GameType gameType) {
		switch (gameType) {
		case ChineseChess:
			return new ChineseChessGame(gameType);
		}
		return null;
	}

	public int getResultScore(Player player, List<Player> playerList,
			int winloss) {
		return 0;
	}

	public void orderPlayerSequence(int set, List<Player> playerList) {

	}
}
