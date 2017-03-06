package com.pgame.game;

import com.pgame.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class RoomServer extends Server {

//	public static final String SERVER_IP = "54.254.190.35"; //"10.0.2.2"; //

    public static final String SERVER_IP = "127.0.0.1";

	private Map<String, Player> players;

	private Map<Integer, Room> rooms;

	private Game.GameType gameType;

	public RoomServer(String ip, int port, Game.GameType gt) {
		super(ip, port);
		this.setGameType(gt);
		players = new HashMap<String, Player>();
		rooms = new HashMap<Integer, Room>();
		rooms.put(10001, new Room(SERVER_IP, 10001, this));
		rooms.put(10002, new Room(SERVER_IP, 10002, this));
		try {
			char ch = (char) System.in.read();
			if (ch == 'c') {
				for (Room room : rooms.values()) {
					room.stop();
				}
				stop();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void processCommand(String strCommand, SelectionKey key)
			throws IOException {
		Command command = Command.parse(strCommand);
		switch (command) {
		case SYNCROOM:
			Player p = new Player(command.getPlayerId(), this);
			p.setChannel((SocketChannel) key.channel());
			players.put(command.getPlayerId(), p);
			respSyncRoom(key, p);
			break;
		default:
			break;
		}
	}

	private void respSyncRoom(SelectionKey key, Player p) {
		StringBuffer sb = new StringBuffer();
		sb.append("syncRoom:");
		for (Room r : rooms.values()) {
			sb.append(r.toString()).append(",");
		}
		sb.append(";Player:" + p.toString());
		sb.append("|");
		respond(sb.toString(), key);
	}

	private void respond(String resp, SelectionKey key) {
		ByteBuffer buf = (ByteBuffer) key.attachment();
		buf.put(resp.getBytes());
		buf.flip();
		doWrite(key);
	}

	public Player getPlayer(String playerId) {
		return players.get(playerId);
	}

	public Game.GameType getGameType() {
		return gameType;
	}

	public void setGameType(Game.GameType gameType) {
		this.gameType = gameType;
	}

}
