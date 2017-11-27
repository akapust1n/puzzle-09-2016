import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.mail.park.game.GameMechService;
import ru.mail.park.game.mechanics.GameSession;
import ru.mail.park.game.mechanics.Player;
import ru.mail.park.game.messaging.PlayerAction;
import ru.mail.park.game.messaging.ServerSnap;
import ru.mail.park.game.messaging.ServerSnapService;
import ru.mail.park.model.UserProfile;
import ru.mail.park.websocket.Message;
import ru.mail.park.websocket.RemotePointService;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class GameMechServiceTest extends AccountServiceMockedTest {
    @MockBean
    private RemotePointService remotePointService;
    @Autowired
    private GameMechService gameMechService;
    @Autowired
    private ServerSnapService serverSnapService;
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private boolean initialized = false;
    private Map<UserProfile, List<Message>> messages = new HashMap<>();
    private Queue<UserProfile> queue;
    private Map<UserProfile, GameSession> sessions;

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws Exception {
        if (!initialized) {
            super.init();
            when(remotePointService.isConnected(any())).thenReturn(true);
            doAnswer(invocationOnMock -> {
                final Object[] args = invocationOnMock.getArguments();
                UserProfile user = (UserProfile) args[0];
                Message message = (Message) args[1];
                List<Message> userMessages = messages.computeIfAbsent(user, k -> new ArrayList<>());
                userMessages.add(message);
                return null;
            }).when(remotePointService).sendMessageToUser(any(), any());
            if (queue == null) {
                Field queueField = GameMechService.class.getDeclaredField("queue");
                queueField.setAccessible(true);
                queue = (Queue) queueField.get(gameMechService);
            }
            if (sessions == null) {
                Field sessionsField = GameMechService.class.getDeclaredField("sessions");
                sessionsField.setAccessible(true);
                sessions = (Map) sessionsField.get(gameMechService);
            }
        }
        initialized = true;
        messages.clear();
        queue.clear();
        sessions.clear();
    }

    @Test
    public void addEvenUsers() throws Exception {
        int userCount = 10;
        addUsers(userCount);
        assertEquals(0, queue.size());
        assertEquals(userCount, sessions.size());
    }

    @Test
    public void addOddUsers() throws Exception {
        int userCount = 11;
        addUsers(userCount);
        assertEquals(1, queue.size());
        assertEquals(userCount - 1, sessions.size());
    }

    @Test
    public void disconnectAll() throws Exception {
        addUsers(10);
        for (UserProfile user : users) {
            executor.execute(() -> gameMechService.handleDisconnect(user));
        }
        Thread.sleep(200);
        assertEquals(0, sessions.size());
    }

    @Test
    public void disconnectOne() throws Exception {
        int userCount = 10;
        addUsers(userCount);
        gameMechService.handleDisconnect(users.get(0));
        assertEquals(userCount - 2, sessions.size());
    }

    @Test
    public void disconnectHalfUnique() throws Exception {
        addUsers(20);
        Set<GameSession> uniqueSessions = new HashSet<>(sessions.values());
        uniqueSessions.stream().limit(5).forEach(session -> {
            UserProfile user = session.getFirst().getUser();
            executor.execute(() -> gameMechService.handleDisconnect(user));
        });
        Thread.sleep(200);
        assertEquals(10, sessions.size());
    }

    @Test
    public void gameJoinMessages() throws Exception {
        int userCount = 10;
        addUsers(userCount);
        int messageCount = messages.values().stream().map(List::size).mapToInt(Integer::valueOf).sum();
        assertEquals(userCount, messageCount);
    }

    @Test
    public void handleOnePlayerActions() throws Exception {
        addUsers(2);
        int actionCount = 10;
        for (int i = 0; i < actionCount; i++) {
            executor.execute(() -> gameMechService.addPlayerAction(users.get(0), new PlayerAction()));
        }
        Thread.sleep(200);
        int messageCount = messages.values().stream().map(List::size).mapToInt(Integer::valueOf).sum();
        assertEquals(2 + actionCount * 2, messageCount);
    }

    @Test
    public void handleTwoPlayersActions() throws Exception {
        addUsers(2);
        int actionCount = 20;
        for (int i = 0; i < actionCount; i++) {
            int n = (int) (2 * Math.random());
            executor.execute(() -> gameMechService.addPlayerAction(users.get(n), new PlayerAction()));
        }
        Thread.sleep(200);
        int messageCount = messages.values().stream().map(List::size).mapToInt(Integer::valueOf).sum();
        assertEquals(2 + actionCount * 2, messageCount);
    }

    @Test
    public void checkReceiver() throws Exception {
        UserProfile user = new UserProfile("a", "b", "c");
        Message message = new Message();
        remotePointService.sendMessageToUser(user, message);
        assertEquals(user, messages.keySet().stream().findAny().orElse(null));
    }

    @Test
    public void gameOverReceivers() throws Exception {
        UserProfile user1 = new UserProfile("a", "b", "c");
        UserProfile user2 = new UserProfile("q", "w", "e");
        Player player1 = new Player(user1);
        Player player2 = new Player(user2);
        GameSession session = new GameSession(player1, player2);
        serverSnapService.sendGameOverSnaps(session, player1);
        List<Message> messages1 = messages.get(user1);
        List<Message> messages2 = messages.get(user2);
        Message message1 = messages1.get(0);
        Message message2 = messages2.get(0);
        JSONObject content1 = new JSONObject(message1.getContent());
        String name1 = content1.getString("player");
        boolean win1 = content1.getBoolean("win");
        JSONObject content2 = new JSONObject(message2.getContent());
        String name2 = content2.getString("player");
        boolean win2 = content2.getBoolean("win");
        assertEquals(user1.getLogin(), name1);
        assertEquals(user2.getLogin(), name2);
        assertTrue(win1);
        assertFalse(win2);
    }

    private void addUsers(int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            accountService.addUser("login" + i, "email" + i, "password" + i);
        }
        for (int i = 0; i < count; i++) {
            int finalI = i;
            executor.execute(() -> gameMechService.addPlayer(users.get(finalI)));
        }
        Thread.sleep(200);
    }
}
