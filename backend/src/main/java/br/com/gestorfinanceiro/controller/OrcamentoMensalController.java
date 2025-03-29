package br.com.gestorfinanceiro.controller;

import br.com.gestorfinanceiro.config.security.JwtUtil;
import br.com.gestorfinanceiro.dto.OrcamentoMensal.OrcamentoMensalDTO;
import br.com.gestorfinanceiro.mappers.Mapper;
import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.services.OrcamentoMensalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/orcamento-mensal")
public class OrcamentoMensalController {

    private final OrcamentoMensalService orcamentoMensalService;
    private final Mapper<OrcamentoMensalEntity, OrcamentoMensalDTO> orcamentoMensalMapper;
    private final JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public OrcamentoMensalController(OrcamentoMensalService orcamentoMensalService,
                                     Mapper<OrcamentoMensalEntity, OrcamentoMensalDTO> orcamentoMensalMapper,
                                     JwtUtil jwtUtil) {
        this.orcamentoMensalService = orcamentoMensalService;
        this.orcamentoMensalMapper = orcamentoMensalMapper;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<List<OrcamentoMensalDTO>> findAll(HttpServletRequest request) {
        return ResponseEntity.ok(mapToDTOs(
                orcamentoMensalService.listarTodosPorUsuario(getUserId(request))
        ));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<OrcamentoMensalDTO> findById(HttpServletRequest request, @PathVariable String uuid) {
        return ResponseEntity.ok(mapToDTO(
                orcamentoMensalService.buscarPorId(getUserId(request), uuid)
        ));
    }

    @GetMapping("/periodo/{periodo}")
    public ResponseEntity<List<OrcamentoMensalDTO>> findByPeriodo(HttpServletRequest request, @PathVariable YearMonth periodo) {
        return ResponseEntity.ok(mapToDTOs(
                orcamentoMensalService.listarPorPeriodo(getUserId(request), periodo)
        ));
    }

    @PostMapping
    public ResponseEntity<OrcamentoMensalDTO> create(HttpServletRequest request, @RequestBody @Valid OrcamentoMensalDTO dto) {
        System.out.println(dto);
        OrcamentoMensalEntity orcamento = orcamentoMensalService.criarOrcamentoMensal(
                getUserId(request), dto.getCategoria(), dto.getValorLimite(), dto.getPeriodo()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{uuid}")
                .buildAndExpand(orcamento.getUuid())
                .toUri();

        return ResponseEntity.created(location).body(mapToDTO(orcamento));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<OrcamentoMensalDTO> update(HttpServletRequest request, @PathVariable String uuid,
                                                     @RequestBody @Valid OrcamentoMensalRequest requestBody) {
        return ResponseEntity.ok(mapToDTO(
                orcamentoMensalService.atualizarOrcamentoMensal(
                        getUserId(request), uuid, requestBody.categoria(), requestBody.valorLimite(), requestBody.periodo()
                )
        ));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(HttpServletRequest request, @PathVariable String uuid) {
        orcamentoMensalService.excluirOrcamentoMensal(getUserId(request), uuid);
        return ResponseEntity.noContent().build();
    }

    private String getUserId(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (token == null || !token.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Token JWT inválido ou ausente");
        }
        return jwtUtil.extractUserId(token.replace(BEARER_PREFIX, ""));
    }

    private List<OrcamentoMensalDTO> mapToDTOs(List<OrcamentoMensalEntity> orcamentos) {
        return orcamentos.stream().map(orcamentoMensalMapper::mapTo).toList();
    }

    private OrcamentoMensalDTO mapToDTO(OrcamentoMensalEntity orcamento) {
        return orcamentoMensalMapper.mapTo(orcamento);
    }

    public record OrcamentoMensalRequest(
            @NotBlank String categoria,
            @NotNull @Positive BigDecimal valorLimite,
            @NotNull YearMonth periodo
    ) {}
}
