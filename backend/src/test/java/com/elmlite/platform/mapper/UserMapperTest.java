package com.elmlite.platform.mapper;

import com.elmlite.platform.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Sql(
        scripts = {
                "/db/h2/user-schema.sql",
                "/db/h2/user-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldMapUserFieldsWhenSelectingById() {
        User user = userMapper.selectById(1L);

        assertNotNull(user);
        assertEquals("demo_user", user.getUsername());
        assertEquals("演示用户", user.getNickname());
        assertNull(user.getPhone());
    }

    @Test
    void shouldInsertUserWithoutPhone() {
        User user = newUser("new_user", null);

        assertEquals(1, userMapper.insert(user));
        assertNotNull(user.getId());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        User user = newUser("demo_user", null);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userMapper.insert(user)
        );
    }

    @Test
    void shouldRejectDuplicateNonNullPhone() {
        User user = newUser("another_user", "19900000001");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userMapper.insert(user)
        );
    }

    private User newUser(String username, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPasswordHash("test_password_hash");
        user.setNickname("测试用户");
        return user;
    }
}
