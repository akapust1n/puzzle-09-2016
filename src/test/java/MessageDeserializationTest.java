import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.mail.park.Application;
import ru.mail.park.game.messaging.GameJoin;
import ru.mail.park.game.messaging.PlayerAction;
import ru.mail.park.game.messaging.ServerSnap;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class MessageDeserializationTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Test
    public void testGameJoin() throws IOException {
        objectMapper.readValue("{}", GameJoin.class);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Test
    public void testPlayerAction() throws IOException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("row", 1);
        jsonObject.put("col", 2);
        jsonObject.put("positive", true);
        final PlayerAction playerAction = objectMapper.readValue(jsonObject.toString(), PlayerAction.class);
        assertEquals(1, playerAction.getRow());
        assertEquals(2, playerAction.getCol());
        assertEquals(true, playerAction.isPositive());
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Test
    public void testServerSnap() throws IOException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("player", "a");
        jsonObject.put("opponent", "b");
        final int[][] playerMatrix = new int[][]{{1, 2}, {3, 4}};
        jsonObject.put("playerMatrix", playerMatrix);
        final int[][] opponentMatrix = new int[][]{{5, 6}, {7, 8}};
        jsonObject.put("opponentMatrix", opponentMatrix);
        final int[][] target = new int[][]{{9, 9}, {9, 9}};
        jsonObject.put("target", target);
        jsonObject.put("gameOver", true);
        jsonObject.put("win", true);
        final ServerSnap snap = objectMapper.readValue(jsonObject.toString(), ServerSnap.class);
        assertEquals("a", snap.getPlayer());
        assertEquals("b", snap.getOpponent());
        assertArrayEquals(playerMatrix, snap.getPlayerMatrix());
        assertArrayEquals(opponentMatrix, snap.getOpponentMatrix());
        assertArrayEquals(target, snap.getTarget());
        assertEquals(true, snap.isGameOver());
        assertEquals(true, snap.isWin());
    }
}
