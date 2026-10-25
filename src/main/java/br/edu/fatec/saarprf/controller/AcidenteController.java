package br.edu.fatec.saarprf.controller;

import br.edu.fatec.saarprf.dto.ContagemCausa;
import br.edu.fatec.saarprf.dto.ContagemMensal;
import br.edu.fatec.saarprf.dto.FiltroAcidente;
import br.edu.fatec.saarprf.dto.PaginaResposta;
import br.edu.fatec.saarprf.dto.ResultadoImportacao;
import br.edu.fatec.saarprf.model.Acidente;
import br.edu.fatec.saarprf.service.AcidenteService;
import br.edu.fatec.saarprf.service.ImportacaoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/acidentes")
public class AcidenteController {

    private final AcidenteService service;
    private final ImportacaoService importacaoService;

    public AcidenteController(AcidenteService service, ImportacaoService importacaoService) {
        this.service = service;
        this.importacaoService = importacaoService;
    }

    @GetMapping
    public ResponseEntity<PaginaResposta<Acidente>> listar(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(service.listar(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Acidente> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Acidente> criar(@Valid @RequestBody Acidente acidente) {
        Acidente salvo = service.criar(acidente);
        return ResponseEntity.created(URI.create("/api/acidentes/" + salvo.getId())).body(salvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Acidente> atualizar(@PathVariable Long id, @Valid @RequestBody Acidente acidente) {
        return service.atualizar(id, acidente)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return service.excluir(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoImportacao> importar(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(importacaoService.importar(arquivo));
    }

    @GetMapping("/filtro")
    public ResponseEntity<PaginaResposta<Acidente>> filtrar(FiltroAcidente filtro,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(service.filtrar(filtro, page, size));
    }

    @GetMapping("/estatisticas/causas")
    public ResponseEntity<List<ContagemCausa>> topCausas(FiltroAcidente filtro) {
        return ResponseEntity.ok(service.topCausas(filtro));
    }

    @GetMapping("/estatisticas/mensal")
    public ResponseEntity<List<ContagemMensal>> evolucaoMensal(FiltroAcidente filtro) {
        return ResponseEntity.ok(service.evolucaoMensal(filtro));
    }
}
