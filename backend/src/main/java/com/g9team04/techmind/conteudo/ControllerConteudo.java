package com.g9team04.techmind.conteudo;

import com.g9team04.techmind.infrastructure.LoteProcessamentoException;
import com.g9team04.techmind.infrastructure.ValidatorCsv;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("conteudo")
public class ControllerConteudo {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ControllerConteudo.class);
    private final ConteudoService conteudoService;
    private final ValidatorCsv validatorCsv;
    private final LoteProcessor loteProcessor;

    public ControllerConteudo(ConteudoService conteudoService, LoteProcessor loteProcessor, ValidatorCsv validatorCsv) {
        this.conteudoService = conteudoService;
        this.validatorCsv = validatorCsv;
        this.loteProcessor = loteProcessor;
    }

    @PostMapping
    public ResponseEntity<ConteudoResponse> processarConteudo(
            @Valid @RequestBody ConteudoRequest request) {
        ConteudoResponse response = conteudoService.processar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/titulo")
    public ResponseEntity<PageResult<ConteudoResponse>> buscarPorTitulo(
            @RequestParam(required = false) String titulo,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResult.from(conteudoService.findByTituloContainingIgnoreCase(
                titulo == null ? "" : titulo, limitarPagina(pageable))));
    }

    @GetMapping("/categoria")
    public ResponseEntity<PageResult<ConteudoResponse>> categoria(
            @RequestParam String categoria,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResult.from(conteudoService.buscarPorCategoria(categoria, limitarPagina(pageable))));
    }

    @GetMapping("relacionados/{id}")
    public ResponseEntity<PageResult<ConteudoResponse>> buscarRelacionados(
            @PathVariable Long id,
            @PageableDefault(size = 5, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResult.from(conteudoService.buscarRelacionados(id, limitarPagina(pageable))));
    }

    @PostMapping(value = "/lote", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LoteResponse> processarLote(@RequestParam("arquivo") MultipartFile arquivo) {

        validatorCsv.validarCsv(arquivo);
        try{
            LoteResponse response = loteProcessor.processar(arquivo.getInputStream());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            throw new LoteProcessamentoException("Erro ao ler o arquivo enviado", e);

        }

    }

    private Pageable limitarPagina(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), pageable.getSort());
    }
}
