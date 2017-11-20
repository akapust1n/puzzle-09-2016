import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ru.mail.park.Application;
import ru.mail.park.model.UserProfile;
import ru.mail.park.model.exception.UserAlreadyExistsException;
import ru.mail.park.services.AccountService;
import ru.mail.park.services.SecurityService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public abstract class AccountServiceMockedTest {
    @MockBean
    protected AccountService accountService;
    @MockBean
    protected SecurityService securityService;
    protected List<UserProfile> users = new ArrayList<>();
    private boolean initialized = false;

    @Before
    public void init() throws Exception {
        if (!initialized) {
            doAnswer(invocationOnMock -> {
                final Object[] args = invocationOnMock.getArguments();
                String login = (String) args[0];
                String password = (String) args[1];
                String email = (String) args[2];
                if (users.stream().anyMatch(user -> user.getLogin().equals(login) || user.getEmail().equals(email))) {
                    throw new UserAlreadyExistsException();
                }
                users.add(new UserProfile(login, email, password));
                return null;
            }).when(accountService).addUser(any(), any(), any());
            doAnswer(invocationOnMock -> {
                String login = (String) invocationOnMock.getArguments()[0];
                return users.stream().filter(user -> user.getLogin().equals(login)).findAny().orElse(null);
            }).when(accountService).getUserByLogin(any());
            doAnswer(invocationOnMock -> {
                UserProfile updatedUser = (UserProfile) invocationOnMock.getArguments()[0];
                users.stream().filter(user -> user.getLogin().equals(updatedUser.getLogin())).findAny()
                        .ifPresent(user -> user.setRank(updatedUser.getRank()));
                return null;
            }).when(accountService).updateUser(any());
            doAnswer(invocationOnMock -> {
                int limit = (int) invocationOnMock.getArguments()[0];
                Stream<UserProfile> sorted = users.stream().sorted(Comparator.comparingInt(UserProfile::getRank)
                        .reversed());
                if (limit > 0) {
                    sorted = sorted.limit(limit);
                }
                return sorted.collect(Collectors.toList());
            }).when(accountService).getTopRanked(anyInt());
            doAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]).when(securityService).encode(any());
            doAnswer(i -> i.getArguments()[0].equals(i.getArguments()[1])).when(securityService).matches(any(), any());
        }
        initialized = true;
    }
}
