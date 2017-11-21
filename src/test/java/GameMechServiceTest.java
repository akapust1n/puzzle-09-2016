import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.mail.park.game.GameMechService;
import ru.mail.park.game.mechanics.GameSession;
import ru.mail.park.game.messaging.PlayerAction;
import ru.mail.park.model.UserProfile;
import ru.mail.park.websocket.Message;
import ru.mail.park.websocket.RemotePointService;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class GameMechServiceTest extends AccountServiceMockedTest {
    @MockBean
    private RemotePointService remotePointService;
    @Autowired
    private GameMechService gameMechService;
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private boolean initialized = false;
    private List<Message> messages = new ArrayList<>();
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
                messages.add((Message) args[1]);
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
    public void addUsers() throws Exception {
        int userCount = 11;
        for (int i = 0; i < userCount; i++) {
            accountService.addUser("login" + i, "email" + i, "password" + i);
        }
        for (int i = 0; i < userCount; i++) {
            int finalI = i;
            executor.execute(() -> gameMechService.addPlayer(users.get(finalI)));
        }
        assertNotEquals(0, queue.size());
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
    public void uniqueSessions() throws Exception {
        int userCount = 10;
        addUsers(userCount);
        assertEquals(0, queue.size());
        assertEquals(userCount, sessions.size());
        Set<GameSession> uniqueSessions = new HashSet<>(sessions.values());
        assertEquals(userCount / 2, uniqueSessions.size());
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
        assertEquals(userCount, messages.size());
    }

    @Test
    public void handleOnePlayerActions() throws Exception {
        addUsers(2);
        int actionCount = 10;
        for (int i = 0; i < actionCount; i++) {
            executor.execute(() -> gameMechService.addPlayerAction(users.get(0), new PlayerAction()));
        }
        Thread.sleep(200);
        assertEquals(2 + actionCount * 2, messages.size());
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
        assertEquals(2 + actionCount * 2, messages.size());
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
