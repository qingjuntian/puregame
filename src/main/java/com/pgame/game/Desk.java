package com.pgame.game;

import com.pgame.game.Game.GameType;
import com.pgame.game.Player.Action;
import com.pgame.game.Player.PlayerStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Desk {
	enum DeskStatus {
		Empty, PlayerWaiting, PlayerFull, InGame;
	};

	private int deskId;

	private int seatNumber;

	private int readyPlayer;

	private List<Player> playerList;

	private DeskStatus status;

	private int set;

	private Game game;

	private static final String GAMERESULT = "gameresult:";

	private static final String PLAYER_READY = "playerReady:";

	private static final String PLAYER_JOIN = "playerJoin:";

	private static final String PLAYER_LEAVE = "playerLeave:";


	public Desk(GameType gameType, int id) {
		this.deskId = id;
		setStatus(DeskStatus.Empty);
		game = Game.createGame(gameType);
		seatNumber = game.getSeats();
		playerList = new ArrayList<Player>();
	}

	private synchronized void setStatus(DeskStatus st) {
		this.status = st;
	}

	private boolean startGame() {
		try {
			setStatus(DeskStatus.InGame);
			game.orderPlayerSequence(set, playerList);
			set++;
			game.start(this);
			for (Player p : playerList) {
				p.onGameStart(playerList, game);
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void syncPlayerStatus(Player player) {
		StringBuffer sb = new StringBuffer();
		sb.append(PLAYER_JOIN);
		for (Player p : playerList) {
			if (p.equals(player) == false) {
				sb.append(p.toString()).append(";");
			}
		}
		player.onMessage(sb.toString());
	}

	public synchronized void onPlayerAct(Player player, Action action,
			String data) {
		switch (status) {
		case Empty:
			switch (action) {
			case Sitdown:
				informPlayersAboutJoin(player);
				playerList.add(player);
				if (playerList.size() == seatNumber) {
					setStatus(DeskStatus.PlayerFull);
				} else {
					setStatus(DeskStatus.PlayerWaiting);
				}
				break;
			default:
				wrongPlayerAction(player, action);
				break;
			}
			break;
		case PlayerWaiting:
			switch (action) {
			case Sitdown:
				informPlayersAboutJoin(player);
				playerList.add(player);
				if (playerList.size() == seatNumber) {
					setStatus(DeskStatus.PlayerFull);
				}
				break;
			case Hand:
				readyPlayer++;
				informPlayersAboutReady(player);
				break;
			case Standup:
				if (removePlayer(playerList, player)) {
					if (playerList.size() == 0) {
						setStatus(DeskStatus.Empty);
					}
					set = 0;
				}
				break;
			default:
				wrongPlayerAction(player, action);
				break;
			}
			break;
		case PlayerFull:
			switch (action) {
			case Hand:
				if (++readyPlayer == seatNumber) {
					if (startGame()) {
						setStatus(DeskStatus.InGame);
					}
				} else {
					informPlayersAboutReady(player);
				}
				break;
			case Standup:
				if (removePlayer(playerList, player)) {
					setStatus(DeskStatus.PlayerWaiting);
					set = 0;
				}
				break;
			default:
				wrongPlayerAction(player, action);
				break;
			}
			break;

		case InGame: {
			switch (action) {
			case Standup:
				if (removePlayer(playerList, player)) {
					setStatus(DeskStatus.PlayerWaiting);
					set = 0;
				}
				break;
			case Game:
				for (Player p : playerList) {
					p.onGameAction(player, data);
				}
				break;
			case FinishGame:
				for (Player p : playerList) {
					p.act(action);
				}
				setStatus(DeskStatus.PlayerFull);
				readyPlayer = 0;
				informPlayersAboutFinish(player, data);
				break;
			default:
				wrongPlayerAction(player, action);
				break;
			}
			break;
		}
		}
	}

	private void informPlayersAboutFinish(Player player, String strWinloss) {
		int winloss = Integer.parseInt(strWinloss);
		player.onMessage(GAMERESULT + winloss + ":"
				+ game.getResultScore(player, playerList, winloss));
		for (Player p : playerList) {
			if (p.equals(player) == false) {
				p.onMessage(GAMERESULT + (-winloss) + ":"
						+ game.getResultScore(p, playerList, -winloss));
			}
		}
	}

	private void informPlayersAboutReady(Player player) {
		if (playerList.size() > 0) {
			for (Player p : playerList) {
				if (p.equals(player) == false) {
					p.onMessage(PLAYER_READY + player.toString() + ";");
				}
			}
		}
	}

	private void informPlayersAboutJoin(Player newPlayer) {
		StringBuffer sb = new StringBuffer();
		sb.append("playerJoin:");
		if (playerList.size() > 0) {
			for (Player p : playerList) {
				sb.append(p.toString()).append(";");
				p.onMessage(PLAYER_JOIN + newPlayer.toString() + ";");
			}
		}
		newPlayer.onMessage(sb.toString());
	}

	private boolean removePlayer(List<Player> list, Player player) {
		if (player.getStatus() == PlayerStatus.Handing) {
			readyPlayer--;
		}

		int idx = -1;
		for (int i = 0; i < list.size(); i++) {
			Player p = list.get(i);
			if (player.equals(p)) {
				idx = i;
			} else {
				p.onMessage(PLAYER_LEAVE + player.getPlayerId());
			}
		}
		if (idx >= 0) {
			list.remove(idx);
			return true;
		}
		return false;
	}

	private void wrongPlayerAction(Player player, Action action) {

	}

	public synchronized String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(deskId).append(":").append(status).append(":")
				.append(playerList.size()).append(":");
		for (Player p : playerList) {
			sb.append(p.toString()).append(";");
		}
		return sb.toString();
	}

	public Game getGame() {
		return game;
	}

}
