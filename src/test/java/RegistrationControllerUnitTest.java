import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import ru.mail.park.main.ApiResponse;
import ru.mail.park.main.RegistrationController;
import ru.mail.park.main.ResponseCode;
import ru.mail.park.model.UserProfile;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class RegistrationControllerUnitTest extends AccountServiceMockedTest {
    private boolean initialized = false;
    private HttpSession session;
    private RegistrationController registrationController;
    private Class authRequestClass;
    private Class regRequestClass;
    private Constructor authRequestConstructor;
    private Constructor regRequestConstructor;
    private Field successResponseLoginField;
    private Method signupMethod;
    private Method authMethod;
    private Method sessionAuthMethod;
    private Method logoutMethod;

    @SuppressWarnings("unchecked")
    @Before
    @Override
    public void init() {
        super.init();
        if (!initialized) {
            registrationController = new RegistrationController(accountService, securityService);
            Class<?>[] declaredClasses = RegistrationController.class.getDeclaredClasses();
            try {
                sessionAuthMethod = RegistrationController.class.getMethod("sessionAuth", HttpSession.class);
                logoutMethod = RegistrationController.class.getMethod("logout", HttpSession.class);
                for (Class c : declaredClasses) {
                    if ("AuthRequest".equals(c.getSimpleName())) {
                        authRequestClass = c;
                        authMethod = RegistrationController.class.getMethod("auth", c, HttpSession.class);
                    } else if ("RegistrationRequest".equals(c.getSimpleName())) {
                        regRequestClass = c;
                        signupMethod = RegistrationController.class.getMethod("signup", c);
                    } else if ("SuccessResponse".equals(c.getSimpleName())) {
                        successResponseLoginField = c.getDeclaredField("login");
                        successResponseLoginField.setAccessible(true);
                    }
                }
                authRequestConstructor = authRequestClass.getDeclaredConstructor(String.class, String.class);
                regRequestConstructor = regRequestClass.getDeclaredConstructor(String.class, String.class, String.class);
            } catch (NoSuchMethodException | NoSuchFieldException e) {
                e.printStackTrace();
            }
            authRequestConstructor.setAccessible(true);
            regRequestConstructor.setAccessible(true);
        }
        initialized = true;
        users.clear();
        users.add(new UserProfile("a", "c", "b"));
        session = new MockHttpSession();
    }

    @Test
    public void addNewUser() {
        ResponseEntity response = postUser("q", "w", "e");
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) response.getBody();
        assertEquals(ResponseCode.OK.getCode(), apiResponse.getCode());
        assertEquals("q", getSuccessResponseLogin(apiResponse.getContent()));
        UserProfile q = accountService.getUserByLogin("q");
        assertEquals("q", q.getLogin());
        assertEquals("w", q.getPassword());
        assertEquals("e", q.getEmail());
    }

    @Test
    public void addUserWithExistingLogin() {
        ResponseEntity responseEntity = postUser("a", "new", "new");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.DUPLICATE_USER.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.DUPLICATE_USER.getMessage(), apiResponse.getContent());
    }

    @Test
    public void addUserWithExistingEmail() {
        ResponseEntity responseEntity = postUser("new", "new", "c");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.DUPLICATE_USER.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.DUPLICATE_USER.getMessage(), apiResponse.getContent());
    }

    @Test
    public void login() {
        ResponseEntity responseEntity = postSession("a", "b");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.OK.getCode(), apiResponse.getCode());
        assertEquals("a", getSuccessResponseLogin(apiResponse.getContent()));
        assertEquals("a", session.getAttribute("login"));
    }

    @Test
    public void badLogin() {
        ResponseEntity responseEntity = postSession("a", "xxx");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.AUTH_ERROR.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.AUTH_ERROR.getMessage(), apiResponse.getContent());
    }

    @Test
    public void sessionAuth() {
        postSession("a", "b");
        ResponseEntity responseEntity = getSession();
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.OK.getCode(), apiResponse.getCode());
        assertEquals("a", getSuccessResponseLogin(apiResponse.getContent()));
    }

    @Test
    public void badSessionAuth() {
        ResponseEntity responseEntity = getSession();
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.AUTH_ERROR.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.AUTH_ERROR.getMessage(), apiResponse.getContent());
    }

    @Test
    public void notExistingUser() {
        ResponseEntity responseEntity = postSession("www", "www");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.AUTH_ERROR.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.AUTH_ERROR.getMessage(), apiResponse.getContent());
    }

    @Test
    public void logout() {
        postSession("a", "b");
        ResponseEntity responseEntity = deleteSession();
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.OK.getCode(), apiResponse.getCode());
        assertEquals("a", getSuccessResponseLogin(apiResponse.getContent()));
        assertNull(session.getAttribute("login"));
    }

    @Test
    public void badLogout() {
        ResponseEntity responseEntity = deleteSession();
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.AUTH_ERROR.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.AUTH_ERROR.getMessage(), apiResponse.getContent());
    }

    @Test
    public void addWithMissingLogin() {
        ResponseEntity responseEntity = postUser(null, "w", "e");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    @Test
    public void addWithMissingPassword() {
        ResponseEntity responseEntity = postUser("q", null, "e");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    @Test
    public void addWithMissingEmail() {
        ResponseEntity responseEntity = postUser("q", "w", null);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    @Test
    public void loginWithMissingLogin() {
        ResponseEntity responseEntity = postSession(null, "b");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    @Test
    public void loginWithMissingPassword() {
        ResponseEntity responseEntity = postSession("a", null);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    @Test
    public void registerWithEmptyLogin() {
        ResponseEntity responseEntity = postUser("", "w", "e");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    @Test
    public void registerWithEmptyPassword() {
        ResponseEntity responseEntity = postUser("q", "", "e");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    @Test
    public void registerWithEmptyEmail() {
        ResponseEntity responseEntity = postUser("q", "w", "");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    @Test
    public void loginWithEmptyLogin() {
        ResponseEntity responseEntity = postSession("", "b");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    @Test
    public void loginWithEmptyPassword() {
        ResponseEntity responseEntity = postSession("a", "");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ApiResponse apiResponse = (ApiResponse) responseEntity.getBody();
        assertEquals(ResponseCode.PARAMETER_MISSING.getCode(), apiResponse.getCode());
        assertEquals(ResponseCode.PARAMETER_MISSING.getMessage(), apiResponse.getContent());
    }

    private ResponseEntity postUser(String login, String password, String email) {
        try {
            Object request = regRequestConstructor.newInstance(login, password, email);
            return (ResponseEntity) signupMethod.invoke(registrationController, request);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity postSession(String login, String password) {
        try {
            Object request = authRequestConstructor.newInstance(login, password);
            return (ResponseEntity) authMethod.invoke(registrationController, request, session);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getSession() {
        try {
            return (ResponseEntity) sessionAuthMethod.invoke(registrationController, session);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity deleteSession() {
        try {
            return (ResponseEntity) logoutMethod.invoke(registrationController, session);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getSuccessResponseLogin(Object successResponse) {
        try {
            return (String) successResponseLoginField.get(successResponse);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
