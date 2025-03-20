package br.com.gestorfinanceiro.controllers.AuthControllerTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import br.com.gestorfinanceiro.controller.AuthController;
import br.com.gestorfinanceiro.dto.LoginDTO;
import br.com.gestorfinanceiro.dto.UserDTO;
import br.com.gestorfinanceiro.exceptions.auth.login.InvalidPasswordException;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.repositories.UserRepository;

@SpringBootTest
@ActiveProfiles("test") 
public class AuthControllerIntegrationTest {

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
        userRepository.deleteAll(); // Limpa o banco antes de cada teste para evitar inconcistencias
    }

    //-------------------TESTES DO MÉTODO REGISTER-------------------//

    @Test
    void conferindoRequesicaoValidaCriandoUser() {
        UserDTO userDTO = setarUsuario("jorge");

        ResponseEntity<UserEntity> response = authController.register(userDTO); 

        //resposta tem que ser 201 created
        assertEquals(response.getStatusCode().toString(), "201 CREATED");

        UserEntity userSalvo = userRepository.findByEmail(userDTO.getEmail()).get();

        //conferir se o usuario salvo é igual ao usuario do DTO feito pela requisição
        assertEquals(userSalvo.getUsername(), userDTO.getUsername());
        assertEquals(userSalvo.getRole().toString(), userDTO.getRole());
    }   

    //-------------------TESTES DO MÉTODO LOGIN-------------------//

    @Test
    void conferirLoginComCredenciaisErradas() {
        adicionarUsuario("jorge");

        LoginDTO loginDTO = new LoginDTO("jorge@gmail.com", "1234567");

        //Se for lançado uma exceção, significa que as credenciais estão erradas e o metodo está funcionando 
        InvalidPasswordException thrown = assertThrows(InvalidPasswordException.class, () -> authController.login(loginDTO));
        assertNotNull(thrown); //Se a exceção for lançada, thrown não será nulo
    }      

    //-------------------------------MÉTODOS AUXILIARES-------------------------------//

    public UserDTO adicionarUsuario(String nome) {
        UserDTO userDTO = setarUsuario(nome);

        authController.register(userDTO); 

        return userDTO;
    }

    public UserDTO setarUsuario(String nome) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(nome); 
        userDTO.setEmail(nome+"@gmail.com");	
        userDTO.setPassword("123456");
        userDTO.setRole("USER");

        return userDTO;
    }
}
