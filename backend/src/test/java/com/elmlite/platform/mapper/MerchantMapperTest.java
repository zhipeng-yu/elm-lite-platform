package com.elmlite.platform.mapper;

import com.elmlite.platform.entity.Merchant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Sql(
        scripts = {
                "/db/h2/merchant-schema.sql",
                "/db/h2/merchant-data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MerchantMapperTest {

    @Autowired
    private MerchantMapper merchantMapper;

    @Test
    void shouldMapMerchantFieldsWhenSelectingById() {
        Merchant merchant = merchantMapper.selectById(1L);

        assertNotNull(merchant);
        assertEquals("demo_merchant", merchant.getAccount());
        assertEquals("Demo Merchant", merchant.getMerchantName());
        assertEquals("Test Contact", merchant.getContactName());
        assertEquals("19900000002", merchant.getContactPhone());
    }

    @Test
    void shouldInsertMerchant() {
        Merchant merchant = newMerchant("new_merchant");

        assertEquals(1, merchantMapper.insert(merchant));
        assertNotNull(merchant.getId());
    }

    @Test
    void shouldRejectDuplicateAccount() {
        Merchant merchant = newMerchant("demo_merchant");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> merchantMapper.insert(merchant)
        );
    }

    private Merchant newMerchant(String account) {
        Merchant merchant = new Merchant();
        merchant.setAccount(account);
        merchant.setPasswordHash("test_password_hash");
        merchant.setMerchantName("New Merchant");
        merchant.setContactName("New Contact");
        merchant.setContactPhone("19900000003");
        return merchant;
    }
}
