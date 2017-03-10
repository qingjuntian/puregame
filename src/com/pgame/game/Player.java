package com.pgame.game;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.pgame.game.Game.GameType;

public class Player {
	private static final String COMMAND_SEPARATOR = "|";

	public static enum PlayerStatus {
		Goofing, Sitting, Handing, Gaming;
	};

	public static enum Action {
		Sitdown, Hand, Go, Standup, Game, FinishGame;
	}

	private String playerId;

	private String playerDisplayName;

	private PlayerStatus status;

	private Desk desk;

	private SocketChannel channel;

	private int playSeq = 0;

	private Map<GameType, Integer> gameScore;

	private RoomServer roomServer;

	Player(String id, RoomServer roomServer) {
		this(id, id, roomServer);
	}

	Player(String id, String name, RoomServer roomServer) {
		this.playerId = id;
		this.playerDisplayName = name;
		this.roomServer = roomServer;
		this.status = PlayerStatus.Goofing;
		this.gameScore = new HashMap<GameType, Integer>();
	}

	public void sitDown(Desk d, SocketChannel newChannel)
			throws IOException {
		if (desk != null) {
			desk.onPlayerAct(this, Action.Standup, null);
			act(Action.Standup);
		}

		if (channel == null || newChannel.equals(channel) == false) {
			setChannel(newChannel);
		}
		setDesk(d);
		act(Action.Sitdown);
		desk.onPlayerAct(this, Action.Sitdown, null);
	}

	public void failSitDown(SelectionKey key) throws IOException {
		key.channel().close();
		sendData("resp:sitdown:Room Full");
	}

	public void hand() {
		act(Action.Hand);
		desk.onPlayerAct(this, Action.Hand, null);
	}

	public void onMessage(String msg) {
		try {
			sendData(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void act(Action action) {
		switch (status) {
		case Goofing:
			switch (action) {
			case Sitdown:
				setStatus(PlayerStatus.Sitting);
				break;
			default:
				wrongAction(status, action);
				break;
			}
			break;
		case Sitting:
			switch (action) {
			case Sitdown:
				break;
			case Hand:
				setStatus(PlayerStatus.Handing);
				break;
			case Standup:
				setStatus(PlayerStatus.Goofing);
				break;
			default:
				wrongAction(status, action);
				break;
			}
			break;
		case Handing:
			switch (action) {
			case Standup:
				setStatus(PlayerStatus.Goofing);
				break;
			case Go:
				setStatus(PlayerStatus.Gaming);
				break;
			default:
				wrongAction(status, action);
				break;
			}
			break;
		case Gaming:
			switch (action) {
			case Standup:
				setStatus(PlayerStatus.Goofing);
				break;
			case FinishGame:
				setStatus(PlayerStatus.Sitting);
				break;
			default:
				wrongAction(status, action);
				break;
			}
			break;
		}
	}

	private void wrongAction(PlayerStatus status2, Action action) {

	}

	private synchronized void setStatus(PlayerStatus s) {
		this.status = s;
	}

	public synchronized PlayerStatus getStatus() {
		return this.status;
	}

	public void onGameStart(List<Player> playerList, Game game)
			throws IOException {

		StringBuffer sb = new StringBuffer();
		sb.append("startGame:").append(getPlaySeq()).append(":");
		for (Player p : playerList) {
			if (equals(p) == false) {
				sb.append(p.getPlayerDisplayName()).append(",");
			}
		}

		sendData(sb.toString());
		act(Action.Go);
	}

	public void onGameAction(Player player, String data) {
		try {
			if (equals(player) == false) {
				sendData(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendData(String data) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.put((data + COMMAND_SEPARATOR).getBytes());
		buf.flip();
		while (buf.hasRemaining()) {
			channel.write(buf);
		}
	}

	public String getPlayerId() {
		return playerId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Player) {
			Player p = (Player) obj;
			return this.getPlayerId() == p.getPlayerId();
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.playerDisplayName).append(",").append(getScore())
				.append(",").append(status);
		return sb.toString();
	}

	private int getScore() {
		return this.getScore(roomServer.getGameType());
	}

	public boolean isInStatus(PlayerStatus s) {
		return status == s;
	}

	public void setChannel(SocketChannel c) throws IOException {
		try {
			if (channel != null) {
				channel.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.channel = c;
	}

	public int getPlaySeq() {
		return playSeq;
	}

	public void setPlaySeq(int playSeq) {
		this.playSeq = playSeq;
	}

	public String getPlayerDisplayName() {
		return playerDisplayName;
	}

	public void setPlayerDisplayName(String playerDisplayName) {
		this.playerDisplayName = playerDisplayName;
	}

	public Desk getDesk() {
		return desk;
	}

	public void setDesk(Desk desk) {
		this.desk = desk;
	}

	public int getScore(GameType gameType) {
		Integer score = gameScore.get(gameType);
		if (score == null) {
			switch (gameType) {
			case ChineseChess:
				score = (new Random()).nextInt(3000);
				break;
			default:
				score = (new Random()).nextInt(3000);
				break;
			}
			gameScore.put(gameType, score);
		}
		return score;
	}

}
