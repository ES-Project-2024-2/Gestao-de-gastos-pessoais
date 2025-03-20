package br.com.gestorfinanceiro.services.AuthServiceTest;

import br.com.gestorfinanceiro.TestDataUtil;
import br.com.gestorfinanceiro.exceptions.auth.UserOperationException;
import br.com.gestorfinanceiro.exceptions.auth.login.EmailNotFoundException;
import br.com.gestorfinanceiro.exceptions.auth.login.InvalidPasswordException;
import br.com.gestorfinanceiro.exceptions.auth.register.EmailAlreadyExistsException;
import br.com.gestorfinanceiro.exceptions.auth.register.UsernameAlreadyExistsException;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.Roles;
import br.com.gestorfinanceiro.repositories.UserRepository;
import br.com.gestorfinanceiro.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test") 
class AuthServiceUnitTest  {

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
        userRepository.deleteAll(); // Limpa o banco antes de cada teste para evitar inconsistências
    }

    //-------------------TESTES DO METODO REGISTER-------------------//

    @Test
    void deveRegistrarUsuario() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge");
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        int qtdUsersInicial = (int) userRepository.count(); // Conta a quantidade de usuários antes de registrar um novo
        authService.register(user);
        int qtdUsersFinal = (int) userRepository.count(); // Conta a quantidade de usuários após registrar um novo
        assertEquals(qtdUsersInicial + 1, qtdUsersFinal); // Verifica se a quantidade de usuários aumentou em 1 com o registro do novo usuário
    }

    @Test
    void verificarSenhaCriptografada() {
        UserEntity user = TestDataUtil.criarUsuarioEntityUtil("jorge"); //Adiciona um usuario no banco

        String senhaDada = user.getPassword(); //Pega a senha informada pelo usuário

        authService.register(user);

        UserEntity userSalvo = userRepository.findByEmail(user.getEmail()).get(); //Recupera o usuario salvo no banco
        String senhaSalva = userSalvo.getPassword(); //Pega a senha possivelmente modificada desse usuario salva no banco

        assertNotEquals(senhaDada, senhaSalva); // Verifica se a senha dada é diferente da senha salva no banco, por conta da criptografia
    }

    @Test
    void ErroAoRegistrarUsuarioComEmailJaCadastrado() {
        adicionarUsuario("jorge");

        UserEntity user2 = TestDataUtil.criarUsuarioEntityUtil("jorge");

        //Verifica se o assertThrows lançou a exceção esperada se sim a variável thrown recebera essa exceção
        EmailAlreadyExistsException thrown = assertThrows( EmailAlreadyExistsException.class, () -> authService.register(user2));
        //Se a variável thrown não for nula quer dizer que a exceção foi lançada como esperado
        assertNotNull(thrown); 
    }

    @Test
    void ErroAoRegistrarUsuarioComUsernameJaCadastrado() {
        adicionarUsuario("jorge");

        UserEntity user2 = TestDataUtil.criarUsuarioEntityUtil("jorge");
        user2.setEmail("aaaaa@gmail.com");
        UsernameAlreadyExistsException thrown = assertThrows( UsernameAlreadyExistsException.class, () -> authService.register(user2)); 
        assertNotNull(thrown);
    }

    @Test
    void ErroAoRegistrarUsuario() {
        UserEntity user = TestDataUtil.criarUsuarioEntityUtil("jorge");
        user.setUsername(null);

        UserOperationException thrown = assertThrows( UserOperationException.class, () -> authService.register(user)); 
        assertNotNull(thrown);
    }

    //---------------TESTES DO METODO LOGIN----------------//
    
     @Test
    void deveFazerLogin() {
        UserEntity user = adicionarUsuario("jorge");

        UserEntity userLogado = authService.login("jorge@gmail.com", "123456"); //Tenta fazer o login com as credenciais de um usuario cadastrado

        assertEquals(user, userLogado); //Verifica se o usuario retornado foi o mesmo que o cadastrado
        
    }        

    @Test
    void ErroAoFazerLoginComEmailInexistente() {
        //tenta login com um e-mail que não existe
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

        //Força um erro no login passando a senha como nula, ai o metodo de criptografia não vai conseguir comparar as senhas
        UserOperationException thrown = assertThrows( UserOperationException.class, () -> authService.login("jorge@gmail.com", null)); 
        assertNotNull(thrown);
    }

    //-------------------------------MÉTODOS AUXILIARES-------------------------------//

    public UserEntity adicionarUsuario(String nome) {
        UserEntity user = TestDataUtil.criarUsuarioEntityUtil(nome);

        authService.register(user); 

        return user;
    }
}
