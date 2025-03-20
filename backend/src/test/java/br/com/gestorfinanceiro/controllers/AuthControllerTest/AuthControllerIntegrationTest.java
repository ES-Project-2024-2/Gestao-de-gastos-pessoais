package br.com.gestorfinanceiro.controllers.AuthControllerTest;

import br.com.gestorfinanceiro.TestDataUtil;
import br.com.gestorfinanceiro.controller.AuthController;
import br.com.gestorfinanceiro.dto.LoginDTO;
import br.com.gestorfinanceiro.dto.UserDTO;
import br.com.gestorfinanceiro.exceptions.auth.login.InvalidPasswordException;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") 
class AuthControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthController authController;

    @Test //teste para ver sw o AuthController foi carregado
    void deveCarregarAuthController() {
        assertNotNull(authController, "O AuthController não deveria ser nulo!");
    }

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        userRepository.deleteAll(); // Limpa o banco antes de cada teste para evitar inconsistências
    }

    //-------------------TESTES DO METODO REGISTER-------------------//

    @Test
    void conferindoRequisicaoValidaCriandoUser() {
        UserDTO userDTO = TestDataUtil.criarUsuarioDtoUtil("jorge");

        ResponseEntity<UserEntity> response = authController.register(userDTO); 

        //resposta tem que ser 201 created
        assertEquals("201 CREATED", response.getStatusCode().toString());

        UserEntity userSalvo = authController.findByEmail(userDTO.getEmail()).getBody();
        assertNotNull(userSalvo); //conferir se o usuario foi salvo

        //conferir se o usuario salvo é igual ao usuario do DTO feito pela requisição
        assertEquals(userSalvo.getUsername(), userDTO.getUsername());
        assertEquals(userSalvo.getRole().toString(), userDTO.getRole());
    }

    //-------------------TESTES DO METODO LOGIN-------------------//

    @Test
    void conferirLoginComCredenciaisErradas() {
        adicionarUsuario("jorge");

        LoginDTO loginDTO = new LoginDTO("jorge@gmail.com", "senhaErrada");

        //Se for lançado uma exceção, significa que as credenciais estão erradas e o metodo está funcionando 
        InvalidPasswordException thrown = assertThrows(InvalidPasswordException.class, () -> authController.login(loginDTO));
        assertNotNull(thrown); //Se a exceção for lançada, thrown não será nulo
    }

    //-------------------------------MÉTODOS AUXILIARES-------------------------------//

    public UserDTO adicionarUsuario(String nome) {
        UserDTO userDTO = TestDataUtil.criarUsuarioDtoUtil(nome);

        authController.register(userDTO);

        return userDTO;
    }
}
