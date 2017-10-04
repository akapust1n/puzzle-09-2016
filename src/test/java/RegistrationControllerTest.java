import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import ru.mail.park.Application;
import ru.mail.park.main.ResponseCode;
import ru.mail.park.model.UserProfile;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class RegistrationControllerTest extends AccountServiceMockedTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    @Override
    public void init() {
        super.init();
        users.clear();
        users.add(new UserProfile("a", "c", "b"));
    }

    @Test
    public void addNewUser() {
        final ResponseEntity<String> postUserResponse = postUser("q", "w", "e");
        assertEquals(HttpStatus.OK, postUserResponse.getStatusCode());
        final JSONObject postUserResponseBody = new JSONObject(postUserResponse.getBody());
        assertEquals(ResponseCode.OK.getCode(), postUserResponseBody.getInt("code"));
        assertEquals("q", postUserResponseBody.getJSONObject("content").getString("login"));
        UserProfile q = accountService.getUserByLogin("q");
        assertEquals("q", q.getLogin());
        assertEquals("w", q.getPassword());
        assertEquals("e", q.getEmail());
    }

    @Test
    public void addUserWithExistingLogin() {
        final ResponseEntity<String> responseEntity = postUser("a", "new", "new");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        final JSONObject body = new JSONObject(responseEntity.getBody());
        assertEquals(ResponseCode.DUPLICATE_USER.getCode(), body.getInt("code"));
        assertEquals(ResponseCode.DUPLICATE_USER.getMessage(), body.getString("content"));
    }

    @Test
    public void addUserWithExistingEmail() {
        final ResponseEntity<String> responseEntity = postUser("new", "new", "c");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        final JSONObject body = new JSONObject(responseEntity.getBody());
        assertEquals(ResponseCode.DUPLICATE_USER.getCode(), body.getInt("code"));
        assertEquals(ResponseCode.DUPLICATE_USER.getMessage(), body.getString("content"));
    }

    @Test
    public void login() {
        final ResponseEntity<String> responseEntity = postSession("a", "b");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        final JSONObject body = new JSONObject(responseEntity.getBody());
        assertEquals(ResponseCode.OK.getCode(), body.getInt("code"));
        assertEquals("a", body.getJSONObject("content").getString("login"));
    }

    @Test
    public void badLogin() {
        final ResponseEntity<String> responseEntity = postSession("a", "c");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        final JSONObject body = new JSONObject(responseEntity.getBody());
        assertEquals(ResponseCode.AUTH_ERROR.getCode(), body.getInt("code"));
        assertEquals(ResponseCode.AUTH_ERROR.getMessage(), body.getString("content"));
    }

    @Test
    public void notExistingUser() {
        final ResponseEntity<String> responseEntity = postSession("www", "www");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        final JSONObject body = new JSONObject(responseEntity.getBody());
        assertEquals(ResponseCode.AUTH_ERROR.getCode(), body.getInt("code"));
        assertEquals(ResponseCode.AUTH_ERROR.getMessage(), body.getString("content"));
    }

    @Test
    public void addWithMissingParameter() {
        ResponseEntity<String> responseEntity = postUser(null, "w", "e");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        final JSONObject body = new JSONObject(responseEntity.getBody());
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), body.getInt("code"));
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), body.getString("content"));
    }

    @Test
    public void loginWithMissingParameter() {
        ResponseEntity<String> responseEntity = postSession("a", null);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        final JSONObject body = new JSONObject(responseEntity.getBody());
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), body.getInt("code"));
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), body.getString("content"));
    }

    private ResponseEntity<String> postUser(String login, String password, String email) {
        final JSONObject request = new JSONObject();
        if (login != null) {
            request.put("login", login);
        }
        if (password != null) {
            request.put("password", password);
        }
        if (email != null) {
            request.put("email", email);
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);
        return restTemplate.postForEntity("/api/user/", entity, String.class);
    }

    private ResponseEntity<String> postSession(String login, String password) {
        final JSONObject request = new JSONObject();
        if (login != null) {
            request.put("login", login);
        }
        if (password != null) {
            request.put("password", password);
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);
        return restTemplate.postForEntity("/api/session/", entity, String.class);
    }
}
