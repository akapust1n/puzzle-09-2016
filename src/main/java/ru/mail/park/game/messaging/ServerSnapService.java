package ru.mail.park.game.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mail.park.game.mechanics.GameSession;
import ru.mail.park.game.mechanics.Player;
import ru.mail.park.model.UserProfile;
import ru.mail.park.websocket.Message;
import ru.mail.park.websocket.RemotePointService;

import java.io.IOException;

@Service
public class ServerSnapService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private RemotePointService remotePointService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ServerSnapService(RemotePointService remotePointService) {
        this.remotePointService = remotePointService;
    }

    public void sendSnapsForSession(GameSession session) throws IOException {
        sendSnapsForSession(session, false, null);
    }

    public void sendGameOverSnaps(GameSession session, Player winner) throws IOException {
        sendSnapsForSession(session, true, winner.getUser().getLogin());
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private void sendSnapsForSession(GameSession session, boolean gameOver, @Nullable String winner) throws IOException {
        IOException exception = null;
        try {
            sendSnapForUser(session.getFirst().getUser(), session, gameOver, winner);
        } catch (IOException e) {
            logger.error("failed to send server snap to user " + session.getFirst().getUser().getLogin(), e);
            exception = e;
        }
        try {
            sendSnapForUser(session.getSecond().getUser(), session, gameOver, winner);
        } catch (IOException e) {
            logger.error("failed to send server snap to user " + session.getSecond().getUser().getLogin(), e);
            exception = e;
        }
        if (exception != null) {
            throw exception;
        }
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private void sendSnapForUser(UserProfile user, GameSession session, boolean gameOver, @Nullable String winner)
            throws IOException {
        final ServerSnap snap = createSnapForPlayer(session.getPlayer(user), session, gameOver, winner);
        final Message message = new Message(ServerSnap.class.getSimpleName(), objectMapper.writeValueAsString(snap));
        remotePointService.sendMessageToUser(user, message);
    }

    private ServerSnap createSnapForPlayer(Player player, GameSession session, boolean gameOver, @Nullable String winner) {
        final ServerSnap snap = new ServerSnap();
        snap.setPlayer(player.getUser().getLogin());
        snap.setOpponent(session.getOpponent(player).getUser().getLogin());
        snap.setPlayerMatrix(player.getSquare().getMatrix());
        snap.setOpponentMatrix(session.getOpponent(player).getSquare().getMatrix());
        snap.setTarget(session.getTarget().getMatrix());
        snap.setGameOver(gameOver);
        if (gameOver) {
            snap.setWin(player.getUser().getLogin().equals(winner));
        }
        return snap;
    }
}
