package com.payment.payment_processing.user.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class UserSerializationTest {

    @Test
    void passwordIsNotIncludedInSerializedUser() throws Exception {
        User user = new User();
        user.setPassword("hashed-password");

        String json = new ObjectMapper().writeValueAsString(user);

        assertFalse(json.contains("password"));
        assertFalse(json.contains("hashed-password"));
    }
}
