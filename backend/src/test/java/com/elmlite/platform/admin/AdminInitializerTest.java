package com.elmlite.platform.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 管理员初始化器在无 admin_account 表的库上（例如未运行迁移的测试库）必须跳过初始化，
 * 不能因查询表失败而中断 Spring 上下文启动。对应生产场景：ADMIN_USERNAME/ADMIN_PASSWORD
 * 已设置，但数据库尚未执行 V4 迁移。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_initializer_no_table;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "admin.username=init_admin",
        "admin.password=InitAdmin@123"
})
class AdminInitializerTest {

    @Test
    void contextStartsWhenAdminTableIsMissing() {
        // 上下文成功加载即通过：初始化器在表缺失时静默跳过。
    }
}
