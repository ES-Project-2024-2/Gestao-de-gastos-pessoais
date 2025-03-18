package br.com.gestorfinanceiro.services;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.gestorfinanceiro.exceptions.auth.UserOperationException;
import br.com.gestorfinanceiro.exceptions.auth.register.EmailAlreadyExistsException;
import br.com.gestorfinanceiro.exceptions.auth.register.UsernameAlreadyExistsException;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.Roles;
import br.com.gestorfinanceiro.repositories.UserRepository;



@SpringBootTest
@ActiveProfiles("test") 
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test //teste para ver sw o AuthService foi carregado
    public void deveCarregarAuthService() {
        assertNotNull(authService, "O AuthService não deveria ser nulo!");
    }

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll(); // Limpa o banco antes de cada teste para evitar inconcistencias
    }
    

    //----------------------------TESTES UNITÁRIOS-------------------------//
    
    @Test
    public void deveRegistrarUsuario() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge");
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        int qtdUsersInicial = (int) userRepository.count(); // Conta a quantidade de usuários antes de registrar um novo
        authService.register(user);
        int qtdUsersFinal = (int) userRepository.count(); // Conta a quantidade de usuários depois de registrar um novo
        assertEquals(qtdUsersInicial + 1, qtdUsersFinal); // Verifica se a quantidade de usuários aumentou em 1 com o registro do novo usuário
    }

    @Test
    public void verificarSenhaCriptografada() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge");
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        String senhaDada = user.getPassword(); //Pega a senha informada pelo usuário
        authService.register(user);

        UserEntity userSalvo = userRepository.findByEmail(user.getEmail()).get(); //Recupera o usuario salvo no banco
        String senhaSalva = userSalvo.getPassword(); //Pega a senha possivelmente modificada desse usuario salva no banco

        assertNotEquals(senhaDada, senhaSalva); // Verifica se a senha dada é diferente da senha salva no banco, por conta da criptografia
    }

    @Test
    public void ErroAoRegistrarUsuarioComEmailJaCadastrado() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge");
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        authService.register(user);

        UserEntity user2 = new UserEntity();
        user2.setUsername("Jorge2");
        user2.setEmail("jorge@gmail.com");
        user2.setPassword("123456");
        user2.setRole(Roles.USER);

        //Verifica se o assertThrows lançou a exceção esperada se sim a variavel thrown recebera essa execeção
        EmailAlreadyExistsException thrown = assertThrows( EmailAlreadyExistsException.class, () -> authService.register(user2)); 
        //Se a variavel thrown não for nula quer dizer que a exceção foi lançada como esperado
        assertNotNull(thrown); 
    }

    @Test
    public void ErroAoRegistrarUsuarioComUsernameJaCadastrado() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge");
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        authService.register(user);

        UserEntity user2 = new UserEntity();
        user2.setUsername("Jorge");
        user2.setEmail("jorginho2@gmail.com");
        user2.setPassword("123456");
        user2.setRole(Roles.USER);

        UsernameAlreadyExistsException thrown = assertThrows( UsernameAlreadyExistsException.class, () -> authService.register(user2)); 
        assertNotNull(thrown);
    }

    @Test
    public void ErroAoRegistrarUsuario() {
        UserEntity user = new UserEntity();
        user.setUsername(null); //Anula o nome pra forçar um erro no registro, pois nome não pode ser nulo
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        UserOperationException thrown = assertThrows( UserOperationException.class, () -> authService.register(user)); 
        assertNotNull(thrown);
    }
}
