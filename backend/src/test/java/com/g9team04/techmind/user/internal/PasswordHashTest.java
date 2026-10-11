package com.g9team04.techmind.user.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashTest {
    @Test
    void protegeSenhaComSaltDiferenteACadaGravacao() {
        String first = PasswordHash.encode("senha-de-teste");
        String second = PasswordHash.encode("senha-de-teste");

        assertThat(first).startsWith("pbkdf2$310000$");
        assertThat(first).doesNotContain("senha-de-teste");
        assertThat(second).isNotEqualTo(first);
    }
}
