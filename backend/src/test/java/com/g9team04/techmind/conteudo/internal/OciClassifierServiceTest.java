package com.g9team04.techmind.conteudo.internal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OciClassifierServiceTest {

    @Test
    void deveMapearRespostaRealDaApiPython() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://classifier");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OciClassifierService service = new OciClassifierService(builder.build());

        server.expect(requestTo("http://classifier/predizer"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"categoria":"Backend","confianca":0.92,"palavras_chave":["java","spring"]}
                        """, MediaType.APPLICATION_JSON));

        var resultado = service.classificar("Java", "Texto sobre Spring");

        assertThat(resultado.categoria()).isEqualTo("Backend");
        assertThat(resultado.probabilidade()).isEqualTo(0.92);
        assertThat(resultado.tags()).containsExactly("java", "spring");
        server.verify();
    }
}
