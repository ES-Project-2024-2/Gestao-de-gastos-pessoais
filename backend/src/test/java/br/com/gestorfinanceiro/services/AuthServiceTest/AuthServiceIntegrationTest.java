package br.com.gestorfinanceiro.services.AuthServiceTest;

import br.com.gestorfinanceiro.TestDataUtil;
import br.com.gestorfinanceiro.exceptions.auth.register.EmailAlreadyExistsException;
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
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
        //teste para ver sw o AuthService foi carregado
    void deveCarregarAuthService() {
        assertNotNull(authService, "O AuthService não deveria ser nulo!");
    }

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        userRepository.deleteAll(); // Limpa o banco antes de cada teste para evitar inconsistências
    }

    //------------------TESTES DO METODO REGISTER----------------------//

    @Test
    void deveRegistrarERecuperarUsuario() {
        UserEntity user = TestDataUtil.criarUsuarioEntityUtil("jorge");

        int qtdUsersInicial = (int) userRepository.count(); // Conta a quantidade de usuários antes de registrar um novo
        authService.register(user);

        UserEntity userSalvo = authService.findUserByEmail(user.getEmail()); //Recupera o usuario salvo no banco

        int qtdUsersFinal = (int) userRepository.count(); // Conta a quantidade de usuários após tentar registrar um novo com e-mail duplicado
        assertEquals(qtdUsersInicial + 1, qtdUsersFinal); // se for igual garante que foi registrado um novo usuario.

        assertEquals(user.getUsername(), userSalvo.getUsername()); //compara se o username do usuario salvo é igual ao username do usuario cadastrado
        assertEquals(user.getRole(), userSalvo.getRole()); //compara se o role do usuario salvo é igual ao role do usuario cadastrado
        assertEquals(user.getEmail(), userSalvo.getEmail()); //compara se o usuario salvo é igual ao usuario cadastrado
    }

    @Test
    void ErroAoRegistrarUsuarioComEmailJaCadastradoVerificandoSeForamSalvos() {
        adicionarUsuario("jorge");

        adicionarUsuario("paulo");

        UserEntity user3 = TestDataUtil.criarUsuarioEntityUtil("paulo");
        user3.setUsername("paulo2");

        UserEntity user4 = new UserEntity();
        user4.setUsername("Jorge2");
        user4.setEmail("jorge@gmail.com");
        user4.setPassword("123456");
        user4.setRole(Roles.USER);

        int qtdUsersInicial = (int) userRepository.count(); // Conta a quantidade de usuários antes de registrar um novo

        //Testa registrar dois usuario com e-mail já cadastrado para garantir que não registrar um novo usuario
        EmailAlreadyExistsException thrown = assertThrows(EmailAlreadyExistsException.class, () -> authService.register(user3));
        assertNotNull(thrown);
        EmailAlreadyExistsException thrown2 = assertThrows(EmailAlreadyExistsException.class, () -> authService.register(user4));
        assertNotNull(thrown2);

        int qtdUsersFinal = (int) userRepository.count(); // Conta a quantidade de usuários após tentar registrar um novo com e-mail duplicado
        assertEquals(qtdUsersInicial, qtdUsersFinal); // se for igual garante que não foi registrado um novo usuario.
    }

    //------------------TESTES DO METODO LOGIN----------------------//
    @Test
    void deveFazerLoginERecuperarUsuario() {
        UserEntity user = adicionarUsuario("jorge");

        UserEntity userLogado = authService.login("jorge@gmail.com", "123456"); //faz login de um user cadastrado 

        assertEquals(user.getUsername(), userLogado.getUsername()); //compara se o username do usuario salvo é igual ao username do usuario logado
        assertEquals(user.getRole(), userLogado.getRole()); //compara se o role do usuario salvo é igual ao role do usuario logado
        assertEquals(user.getEmail(), userLogado.getEmail()); //compara se o usuario salvo é igual ao usuario logado
        //Se todos passarem, prova que dá para recuperar os dados
    }

    //---------------TESTES DO METODO FIND USER BY EMAIL----------------//
    @Test
    void deveRecuperarUsuarioPorEmail() {
        UserEntity user = adicionarUsuario("jorge");

        UserEntity userSalvo = authService.findUserByEmail("jorge@gmail.com"); //Recupera o usuario salvo no banco

        assertEquals(user.getUsername(), userSalvo.getUsername()); //compara se o username do usuario salvo é igual ao username do usuario salvo
        assertEquals(user.getRole(), userSalvo.getRole()); //compara se o role do usuario salvo é igual ao role do usuario logado
        assertEquals(user.getEmail(), userSalvo.getEmail()); //compara se o usuario salvo é igual ao usuario logado
    }

    //-------------------------------MÉTODOS AUXILIARES-------------------------------//

    public UserEntity adicionarUsuario(String nome) {
        UserEntity user = TestDataUtil.criarUsuarioEntityUtil(nome);

        authService.register(user);

        return user;
    }
}
