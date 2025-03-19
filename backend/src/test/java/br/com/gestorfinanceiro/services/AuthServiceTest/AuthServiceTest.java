package br.com.gestorfinanceiro.services.AuthServiceTest;

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
import br.com.gestorfinanceiro.services.AuthService;


@SpringBootTest
@ActiveProfiles("test") 
class AuthServiceTest  {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test //teste para ver sw o AuthService foi carregado
    void deveCarregarAuthService() {
        assertNotNull(authService, "O AuthService não deveria ser nulo!");
    }

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        userRepository.deleteAll(); // Limpa o banco antes de cada teste para evitar inconcistencias
    }
    
    //-------------------TESTES DO MÉTODO REGISTER-------------------//

    @Test
    void deveRegistrarUsuario() {
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
    void verificarSenhaCriptografada() {
        UserEntity user = setarUsuario("jorge"); //Adiciona um usuario no banco

        String senhaDada = user.getPassword(); //Pega a senha informada pelo usuário

        authService.register(user);

        UserEntity userSalvo = userRepository.findByEmail(user.getEmail()).get(); //Recupera o usuario salvo no banco
        String senhaSalva = userSalvo.getPassword(); //Pega a senha possivelmente modificada desse usuario salva no banco

        assertNotEquals(senhaDada, senhaSalva); // Verifica se a senha dada é diferente da senha salva no banco, por conta da criptografia
    }

    @Test
    void ErroAoRegistrarUsuarioComEmailJaCadastrado() {
        adicionarUsuario("jorge");

        UserEntity user2 = setarUsuario("jorge");

        //Verifica se o assertThrows lançou a exceção esperada se sim a variavel thrown recebera essa execeção
        EmailAlreadyExistsException thrown = assertThrows( EmailAlreadyExistsException.class, () -> authService.register(user2)); 
        //Se a variavel thrown não for nula quer dizer que a exceção foi lançada como esperado
        assertNotNull(thrown); 
    }

    @Test
    void ErroAoRegistrarUsuarioComUsernameJaCadastrado() {
        adicionarUsuario("jorge");

        UserEntity user2 = setarUsuario("jorge");
        user2.setEmail("aaaaa@gmail.com");
        UsernameAlreadyExistsException thrown = assertThrows( UsernameAlreadyExistsException.class, () -> authService.register(user2)); 
        assertNotNull(thrown);
    }

    @Test
    void ErroAoRegistrarUsuario() {
        UserEntity user = setarUsuario("jorge");
        user.setUsername(null);

        UserOperationException thrown = assertThrows( UserOperationException.class, () -> authService.register(user)); 
        assertNotNull(thrown);
    }

    //---------------TESTES DO MÉTODO LOGIN----------------//
    
     @Test
    void deveFazerLogin() {
        UserEntity user = adicionarUsuario("jorge");

        UserEntity userLogado = authService.login("jorge@gmail.com", "123456"); //Tenta fazer o login com as credenciais de um usuario cadastrado

        assertEquals(user, userLogado); //Verifica se o usuario retornado foi o mesmo que o cadastrado
        
    }        

    @Test
    void ErroAoFazerLoginComEmailInexistente() {
        //tenta login com um email que não existe
        EmailNotFoundException thrown = assertThrows( EmailNotFoundException.class, () -> authService.login("aaaaaa@gmail.com", "123456")); 
        assertNotNull(thrown);
    }

    @Test
    void ErroAoFazerLoginComSenhaIncorreta() {
        adicionarUsuario("jorge");

        //tenta fazer login com a senha errada
        InvalidPasswordException thrown = assertThrows( InvalidPasswordException.class, () -> authService.login("jorge@gmail.com", "333")); 
        assertNotNull(thrown);
    }

    @Test
    void ErroDeLogin() {
        adicionarUsuario("jorge");

        //Força um erro no login passando a senha como nula, ai o método de criptografia não vai conseguir comparar as senhas
        UserOperationException thrown = assertThrows( UserOperationException.class, () -> authService.login("jorge@gmail.com", null)); 
        assertNotNull(thrown);
    }

    //-------------------------------MÉTODOS AUXILIARES-------------------------------//

    public UserEntity adicionarUsuario(String nome) {
        UserEntity user = setarUsuario(nome);

        authService.register(user); 

        return user;
    }

    public UserEntity setarUsuario(String nome) {
        UserEntity user = new UserEntity();
        user.setUsername(nome); 
        user.setEmail(nome+"@gmail.com");	
        user.setPassword("123456");
        user.setRole(Roles.USER);

        return user;
    }
                
}
