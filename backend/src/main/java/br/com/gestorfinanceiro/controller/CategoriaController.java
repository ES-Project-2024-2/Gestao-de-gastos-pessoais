package br.com.gestorfinanceiro.controller;

import br.com.gestorfinanceiro.config.security.JwtUtil;
import br.com.gestorfinanceiro.dto.categoria.CategoriaCreateDTO;
import br.com.gestorfinanceiro.dto.categoria.CategoriaDTO;
import br.com.gestorfinanceiro.dto.categoria.CategoriaUpdateDTO;
import br.com.gestorfinanceiro.mappers.Mapper;
import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.services.CategoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {
    private final CategoriaService categoriaService;
    private final Mapper<CategoriaEntity, CategoriaDTO> categoriaMapper;
    private final JwtUtil jwtUtil;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";


    public CategoriaController(CategoriaService categoriaService, Mapper<CategoriaEntity, CategoriaDTO> categoriaMapper, JwtUtil jwtUtil) {
        this.categoriaService = categoriaService;
        this.categoriaMapper = categoriaMapper;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<CategoriaDTO> criarCategoria(@Valid @RequestBody CategoriaCreateDTO categoriaCreateDTO, HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER)
                .replace(BEARER_PREFIX, "");
        String userId = jwtUtil.extractUserId(token);

        CategoriaEntity novaCategoria = categoriaService.criarCategoria(categoriaCreateDTO, userId);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(novaCategoria.getUuid())
                .toUri();

        return ResponseEntity.created(location)
                .body(categoriaMapper.mapTo(novaCategoria));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> listarCategorias(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER)
                .replace(BEARER_PREFIX, "");
        String userId = jwtUtil.extractUserId(token);

        List<CategoriaEntity> categorias = categoriaService.listarCategorias(userId);
        List<CategoriaDTO> response = categorias.stream()
                .map(categoriaMapper::mapTo)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/despesas")
    public ResponseEntity<List<CategoriaDTO>> listarCategoriasDespesas(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER)
                .replace(BEARER_PREFIX, "");
        String userId = jwtUtil.extractUserId(token);

        List<CategoriaEntity> categorias = categoriaService.listarCategoriasDespesas(userId);
        List<CategoriaDTO> response = categorias.stream()
                .map(categoriaMapper::mapTo)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/receitas")
    public ResponseEntity<List<CategoriaDTO>> listarCategoriasReceitas(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER)
                .replace(BEARER_PREFIX, "");
        String userId = jwtUtil.extractUserId(token);

        List<CategoriaEntity> categorias = categoriaService.listarCategoriasReceitas(userId);
        List<CategoriaDTO> response = categorias.stream()
                .map(categoriaMapper::mapTo)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{categoriaId}")
    public ResponseEntity<CategoriaDTO> atualizarCategoria(@PathVariable String categoriaId, @Valid @RequestBody CategoriaUpdateDTO categoriaUpdateDTO, HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER)
                .replace(BEARER_PREFIX, "");
        String userId = jwtUtil.extractUserId(token);

        CategoriaEntity categoriaAtualizada = categoriaService.atualizarCategoria(categoriaId, categoriaUpdateDTO,
                userId);

        return ResponseEntity.ok(categoriaMapper.mapTo(categoriaAtualizada));
    }

    @DeleteMapping("/{categoriaId}")
    public ResponseEntity<Void> deletarCategoria(@PathVariable String categoriaId, HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER)
                .replace(BEARER_PREFIX, "");
        String userId = jwtUtil.extractUserId(token);

        categoriaService.excluirCategoria(categoriaId, userId);

        return ResponseEntity.noContent()
                .build();
    }
}
