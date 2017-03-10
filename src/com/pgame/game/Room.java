package com.pgame.game;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.pgame.Server;
import com.pgame.game.Player.Action;

public class Room extends Server {

	public static final Log LOG = LogFactory.getLog(Room.class);

	private Map<Integer, Desk> desks;

	private Map<String, Player> players;

	private int roomId;

	private RoomServer roomServer;

	volatile private boolean running = true; // true while server runs

	public Room(String ip, int port, RoomServer roomServer) {
		this(ip, port, roomServer, 5);
	}

	public Room(String ip, int port, RoomServer roomServer, int readThreadNum) {
		super(ip, port, readThreadNum);
		this.roomId = port;
		this.roomServer = roomServer;
		desks = new HashMap<Integer, Desk>();
		for (int i = 0; i < 2; i++) {
			desks.put(i, new Desk(roomServer.getGameType(), i));
		}
		players = new HashMap<String, Player>();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.roomId).append(":").append(getIp()).append(":")
				.append(getPort()).append(":").append("(").append(players.size())
				.append(")");
		return sb.toString();
	}

	public String toInetAddress() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.roomId).append(":").append(getIp()).append(":")
				.append(getPort());
		return sb.toString();
	}

	@Override
	protected void processCommand(String strCommand, SelectionKey key)
			throws IOException {
		System.out.println(strCommand);
		Command command = Command.parse(strCommand);
		Player player = null;
		switch (command) {
		case SITDOWN:
			player = players.get(command.getPlayerId());
			if (player == null) {
				player = roomServer.getPlayer(command.getPlayerId());
			}
			if (player == null) {
				break;
			}
			if (moreSeatsAvailable()) {
				players.put(player.getPlayerId(), player);
				player.sitDown(randomDesk4Player(player), (SocketChannel) key.channel());
			} else {
				players.remove(command.getPlayerId());
				player.failSitDown(key);
			}
			break;
		case HAND:
			player = players.get(command.getPlayerId());
			player.hand();
			break;
		case STANDUP:
			//leaveRoom in fact for now
			player = players.get(command.getPlayerId());
			player.getDesk().onPlayerAct(player, Action.Standup, null);
			player.act(Action.Standup);
			players.remove(command.getPlayerId()); //leave the room
			break;
		case FORWARD:
			player = players.get(command.getPlayerId());
			player.getDesk().onPlayerAct(player, Action.Game, command.getData());
			break;
		case FINISHGAME:
			player = players.get(command.getPlayerId());
			player.getDesk().onPlayerAct(player, Action.FinishGame, command.getData());
			break;
		case RESET:
			player = players.get(command.getPlayerId());
			Desk desk = player.getDesk();
			desk.syncPlayerStatus(player);
			break;
		default:
			break;
		}
	}

	private boolean moreSeatsAvailable() {
		return true;
	}

	private Desk randomDesk4Player(Player player) {
		Random r = new Random();
		int deskId = r.nextInt(desks.size());
		LOG.info(deskId);
		return desks.get(deskId);
	}

}
