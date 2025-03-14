package br.com.gestorfinanceiro.controller;

import br.com.gestorfinanceiro.dto.LoginDTO;
import br.com.gestorfinanceiro.dto.UserDTO;
import br.com.gestorfinanceiro.mappers.Mapper;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.services.AuthService;
import br.com.gestorfinanceiro.services.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    private final Mapper<UserEntity, UserDTO> userMapper;

    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, Mapper<UserEntity, UserDTO> userMapper, JwtUtil jwtUtil) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<UserEntity> register(@Valid @RequestBody UserDTO userDTO) {
        UserEntity userEntity = userMapper.mapFrom(userDTO);
        UserEntity registeredUser = this.authService.register(userEntity);

        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDTO loginDTO) {
        UserEntity userEntity = authService.login(loginDTO.email(), loginDTO.password());

        // Obtém a role do usuário autenticado
        String role = userEntity.getRole().name();

        // Gera o token JWT com username e role
        String token = jwtUtil.generateToken(userEntity.getUuid(), userEntity.getUsername(),userEntity.getEmail(), role);

        return ResponseEntity.ok(Map.of("token", token));
    }
}