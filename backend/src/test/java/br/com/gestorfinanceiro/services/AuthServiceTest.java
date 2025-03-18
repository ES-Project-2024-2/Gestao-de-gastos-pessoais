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
import br.com.gestorfinanceiro.exceptions.auth.login.EmailNotFoundException;
import br.com.gestorfinanceiro.exceptions.auth.login.InvalidPasswordException;
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
    

    //-------------------------------TESTES UNITÁRIOS-------------------------------//
    
                //------------TESTES DO MÉTODO REGISTER----------//
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

                //------------TESTES DO MÉTODO LOGIN----------//
    
     @Test
    public void deveFazerLogin() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge"); 
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        authService.register(user);

        UserEntity userLogado = authService.login("jorge@gmail.com", "123456"); //Tenta fazer o login com as credenciais de um usuario cadastrado

        assertEquals(user, userLogado); //Verifica se o usuario retornado foi o mesmo que o cadastrado
        
    }        

    @Test
    public void ErroAoFazerLoginComEmailInexistente() {

        //tenta login com um email que não existe
        EmailNotFoundException thrown = assertThrows( EmailNotFoundException.class, () -> authService.login("aaaaaa@gmail.com", "123456")); 
        assertNotNull(thrown);
    }

    @Test
    public void ErroAoFazerLoginComSenhaIncorreta() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge"); 
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        authService.register(user);

        //tenta fazer login com a senha errada
        InvalidPasswordException thrown = assertThrows( InvalidPasswordException.class, () -> authService.login("jorge@gmail.com", "333")); 
        assertNotNull(thrown);
    }

    @Test
    public void ErroDeLogin() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge"); 
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        authService.register(user);

        //Força um erro no login passando a senha como nula, ai o método de criptografia não vai conseguir comparar as senhas
        UserOperationException thrown = assertThrows( UserOperationException.class, () -> authService.login("jorge@gmail.com", null)); 
        assertNotNull(thrown);
    }

    //-------------------------------TESTES DE INTEGRAÇÃO-------------------------------//
                //------------TESTES DO MÉTODO REGISTER----------//

    @Test
    public void deveRegistrarERecuperarUsuario() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge"); 
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        int qtdUsersInicial = (int) userRepository.count(); // Conta a quantidade de usuários antes de registrar um novo
        authService.register(user); 

        UserEntity userSalvo = userRepository.findByEmail(user.getEmail()).get(); //Recupera o usuario salvo no banco

        int qtdUsersFinal = (int) userRepository.count(); // Conta a quantidade de usuários depois de tentar registrar um novo com email duplicado
        assertEquals(qtdUsersInicial + 1, qtdUsersFinal); // se for igual garante que foi registrado um novo usuario.

        assertEquals(user.getUsername(), userSalvo.getUsername()); //compara se o username do usuario salvo é igual ao username do usuario cadastrado
        assertEquals(user.getRole(), userSalvo.getRole()); //compara se o role do usuario salvo é igual ao role do usuario cadastrado
        assertEquals(user.getEmail(), userSalvo.getEmail()); //compara se o usuario salvo é igual ao usuario cadastrado
    }

    @Test
    public void ErroAoRegistrarUsuarioComEmailJaCadastradoVerifcandoSeForamSalvos() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge"); 
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        authService.register(user); 

        UserEntity user2 = new UserEntity();
        user2.setUsername("Paulo"); 
        user2.setEmail("paulo@gmail.com");
        user2.setPassword("123456");
        user2.setRole(Roles.USER);

        authService.register(user2);

        UserEntity user3 = new UserEntity();
        user3.setUsername("Paulo2"); 
        user3.setEmail("paulo@gmail.com");
        user3.setPassword("123456");
        user3.setRole(Roles.USER);

        UserEntity user4 = new UserEntity();
        user4.setUsername("Jorge2"); 
        user4.setEmail("jorge@gmail.com");
        user4.setPassword("123456");
        user4.setRole(Roles.USER);

        int qtdUsersInicial = (int) userRepository.count(); // Conta a quantidade de usuários antes de registrar um novo

        //Testa registrar dois usuario com email que já está cadastrado para garantir que não registrar um novo usuario
        EmailAlreadyExistsException thrown = assertThrows( EmailAlreadyExistsException.class, () -> authService.register(user3)); 
        assertNotNull(thrown);
        EmailAlreadyExistsException thrown2 = assertThrows( EmailAlreadyExistsException.class, () -> authService.register(user4)); 
        assertNotNull(thrown2); 

        int qtdUsersFinal = (int) userRepository.count(); // Conta a quantidade de usuários depois de tentar registrar um novo com email duplicado
        assertEquals(qtdUsersInicial, qtdUsersFinal); // se for igual garante que não foi registrado um novo usuario.
    }
    
}
