package com.pgame.game.cc;

import java.util.List;
import java.util.Random;

import com.pgame.game.Game;
import com.pgame.game.Player;

public class ChineseChessGame extends Game {
	public ChineseChessGame(GameType gameType) {
		super(gameType);
	}

	@Override
	public void orderPlayerSequence(int set, List<Player> playerList) {
		assert (playerList.size() == 2);
		if (set == 0) {
			Random r = new Random();
			boolean red = r.nextBoolean();
			if (red) {
				playerList.get(0).setPlaySeq(0);
				playerList.get(1).setPlaySeq(1);
			} else {
				playerList.get(0).setPlaySeq(1);
				playerList.get(1).setPlaySeq(0);
			}
		} else {
			int seq = playerList.get(0).getPlaySeq();
			playerList.get(0).setPlaySeq(playerList.get(1).getPlaySeq());
			playerList.get(1).setPlaySeq(seq);
		}
	}

	@Override
	public int getResultScore(Player player, List<Player> playerList,
			int winloss) {
		assert playerList.size() == 2;
		Player partner = null;
		for (Player p : playerList) {
			if (p.equals(player) == false) {
				partner = p;
				break;
			}
		}
		return getResultScore(player, partner, winloss);
	}

	private int getResultScore(Player player, Player partner, int winloss) {
		final int SCORE_FACTOR = 10;
		final int RED_BLACK_FACTOR = 3;
		int levelDiff = player.getScore(this.getGameType())
				- partner.getScore(this.getGameType());
		int lossFactor = SCORE_FACTOR
				+ (player.getPlaySeq() == 0 ? RED_BLACK_FACTOR
						: -RED_BLACK_FACTOR);
		int tieFactor = player.getPlaySeq() == 0 ? -RED_BLACK_FACTOR
				: RED_BLACK_FACTOR;
		int winFactor = SCORE_FACTOR
				- (player.getPlaySeq() == 0 ? RED_BLACK_FACTOR
						: -RED_BLACK_FACTOR);
		int score = 0;
		int delta = 0;

		switch (winloss) {
		case -1: // loss
			if (levelDiff == 0) {
				score += -lossFactor;
			} else if (levelDiff > 0) {
				do {
					score -= lossFactor;
					delta += 80;
				} while (delta < levelDiff && score > -50);
			} else {
				score = -50;
				do {
					score += lossFactor;
					delta -= 80;
				} while (score < 0 && delta > levelDiff);
			}
			break;
		case 0: // tie
			if (levelDiff == 0) {
				score = -tieFactor;
			} else if (levelDiff > 0) {
				do {
					score += tieFactor;
					delta += 160;
				} while (delta < levelDiff);
			} else {
				score = -30;
				do {
					score += tieFactor;
					delta -= 160;
				} while (score < 0 && delta > levelDiff);
			}
			break;
		case 1: // win
			if (levelDiff == 0) {
				score = winFactor;
			} else if (levelDiff > 0) {
				do {
					score += winFactor;
					delta += 80;
				} while (delta < levelDiff && score > 0);
			} else {
				score = 10;
				do {
					score += winFactor;
					delta -= 80;
				} while (score < 50 && delta > levelDiff);
			}
			break;
		}
		System.out.println(player.getPlayerDisplayName() + ": " + score);
		return score;
	}

	@Override
	public int getSeats() {
		return 2;
	}

	@Override
	public int getByStanders() {
		return 4;
	}

}
