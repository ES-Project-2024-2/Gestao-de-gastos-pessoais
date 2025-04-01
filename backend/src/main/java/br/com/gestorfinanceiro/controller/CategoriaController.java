package br.com.gestorfinanceiro.controller;

import br.com.gestorfinanceiro.config.security.JwtUtil;
import br.com.gestorfinanceiro.dto.categoria.CategoriaCreateDTO;
import br.com.gestorfinanceiro.dto.categoria.CategoriaDTO;
import br.com.gestorfinanceiro.dto.categoria.CategoriaUpdateDTO;
import br.com.gestorfinanceiro.exceptions.categoria.CategoriaAcessDeniedException;
import br.com.gestorfinanceiro.exceptions.categoria.CategoriaAlreadyExistsException;
import br.com.gestorfinanceiro.exceptions.categoria.CategoriaIdNotFoundException;
import br.com.gestorfinanceiro.exceptions.categoria.CategoriaOperationException;
import br.com.gestorfinanceiro.mappers.Mapper;
import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.services.CategoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
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
        public ResponseEntity<?> criarCategoria(@Valid @RequestBody CategoriaCreateDTO categoriaCreateDTO, 
                                        HttpServletRequest request) {
        try {
                String authHeader = request.getHeader(AUTHORIZATION_HEADER);
                if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                String token = authHeader.replace(BEARER_PREFIX, "");
                String userId = jwtUtil.extractUserId(token);

                CategoriaEntity novaCategoria = categoriaService.criarCategoria(categoriaCreateDTO, userId);

                URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(novaCategoria.getUuid())
                        .toUri();

                return ResponseEntity.created(location)
                        .body(categoriaMapper.mapTo(novaCategoria));
                        
        } catch (CategoriaAlreadyExistsException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponse(ex.getMessage()));
        }
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
    public ResponseEntity<?> atualizarCategoria(
            @PathVariable String categoriaId,
            @Valid @RequestBody CategoriaUpdateDTO categoriaUpdateDTO,
            HttpServletRequest request) {
        
        try {
            String token = request.getHeader(AUTHORIZATION_HEADER)
                    .replace(BEARER_PREFIX, "");
            String userId = jwtUtil.extractUserId(token);
    
            CategoriaEntity categoriaAtualizada = categoriaService.atualizarCategoria(
                    categoriaId, categoriaUpdateDTO, userId);
    
            return ResponseEntity.ok(categoriaMapper.mapTo(categoriaAtualizada));
            
        } catch (CategoriaIdNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(ex.getMessage()));
        } catch (CategoriaAcessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(ex.getMessage()));
        }
    }

        @DeleteMapping("/{categoriaId}")
        public ResponseEntity<?> deletarCategoria(@PathVariable String categoriaId, 
                                                HttpServletRequest request) {
        try {
                String token = request.getHeader(AUTHORIZATION_HEADER)
                        .replace(BEARER_PREFIX, "");
                String userId = jwtUtil.extractUserId(token);
                
                categoriaService.excluirCategoria(categoriaId, userId);
                return ResponseEntity.noContent().build();
                
        } catch (CategoriaIdNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
        } catch (CategoriaAcessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ex.getMessage()));
        } catch (CategoriaOperationException ex) {
                return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage()));
        } catch (Exception ex) {
                return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao excluir categoria"));
        }
        }

    public record ErrorResponse(String message, Instant timestamp) {
    public ErrorResponse(String message) {
        this(message, Instant.now());
        }
    }
}
