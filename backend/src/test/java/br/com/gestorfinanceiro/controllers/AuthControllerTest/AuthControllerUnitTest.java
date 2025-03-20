package br.com.gestorfinanceiro.controllers.AuthControllerTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import br.com.gestorfinanceiro.exceptions.auth.register.EmailAlreadyExistsException;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.repositories.UserRepository;


@SpringBootTest
@ActiveProfiles("test") 
public class AuthControllerUnitTest {
    
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
    void conferirConversaoDTOUserEntity() {
        UserDTO userDTO = adicionarUsuario("jorge");

        //Recupera o usuario salvo no banco com o email dado do DTO, significando que são os mesmos
        UserEntity userSalvo = userRepository.findByEmail(userDTO.getEmail()).get();

        assertEquals(userDTO.getUsername(), userSalvo.getUsername());  
        assertEquals(userDTO.getRole(), userSalvo.getRole().toString());   //Converte o enum em string para comparar
    }           
    
    
    @Test
    void conferirServiceChamadoCorretamente(){        
        UserDTO userDTO = setarUsuario("jorge");
        assertDoesNotThrow(() -> authController.register(userDTO)); //Primeira requisição o service não pode lançar exceção


        UserDTO userDTO2 = setarUsuario("jorge2");
        userDTO2.setEmail("jorge@gmail.com");

        //Se o service for convocado corretamente, ele lançara uma execção de email já existente pois o email já foi cadastrado na requesição anterior
        EmailAlreadyExistsException thrown = assertThrows( EmailAlreadyExistsException.class, () -> authController.register(userDTO2)); 
        assertNotNull(thrown); //Se a exceção for lançada, thrown não será nulo
    }

    //-------------------TESTES DO MÉTODO LOGIN-------------------//

    @Test
    void conferirParametrosLoginDTO() {
        adicionarUsuario("jorge");

        LoginDTO loginDTO = new LoginDTO("jorge@gmail.com", "123456");

        ResponseEntity<Map<String, String>> response = authController.login(loginDTO);
        //Se o status da operação for 200 OK, o login foi bem sucedido portanto os parametros foram passados corretamente
        assertEquals(response.getStatusCode().toString(), "200 OK");
    }           
    
    
    @Test
    void conferirGeracaoDoToken(){  
        adicionarUsuario("jorge");

        LoginDTO loginDTO = new LoginDTO("jorge@gmail.com", "123456");

        ResponseEntity<Map<String, String>> response = authController.login(loginDTO);

        Map<String, String> responseBody = response.getBody();
        assertNotNull(responseBody); //Verifica se teve resposta
        
        String token = responseBody.get("token");
        assertNotNull(token); //Se o token for gerado, ele não será nulo, portanto ocorreu tudo corretamente
        
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
