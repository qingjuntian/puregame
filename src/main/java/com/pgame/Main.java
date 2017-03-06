package com.pgame;

import com.pgame.game.Game;
import com.pgame.game.RoomServer;

public class Main {
	public static void main(String[] args) {
		new RoomServer(RoomServer.SERVER_IP, 10000, Game.GameType.ChineseChess);
	}
}
