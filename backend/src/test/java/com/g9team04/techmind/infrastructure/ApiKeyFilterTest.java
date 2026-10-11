package com.g9team04.techmind.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyFilterTest {
    private static final String KEY = "0123456789abcdef0123456789abcdef";
    private final ApiKeyFilter filter = new ApiKeyFilter(KEY);

    @Test
    void bloqueiaRequisicaoSemChave() throws Exception {
        var request = new MockHttpServletRequest("POST", "/conteudo");
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> called.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(called.get()).isFalse();
    }

    @Test
    void aceitaChaveCorreta() throws Exception {
        var request = new MockHttpServletRequest("POST", "/conteudo");
        request.addHeader("Authorization", "Bearer " + KEY);
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> called.set(true));

        assertThat(called.get()).isTrue();
    }
}
