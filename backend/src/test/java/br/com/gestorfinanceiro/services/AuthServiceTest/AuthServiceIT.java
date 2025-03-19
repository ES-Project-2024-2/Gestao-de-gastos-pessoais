package br.com.gestorfinanceiro.services.AuthServiceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.gestorfinanceiro.exceptions.auth.register.EmailAlreadyExistsException;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.Roles;
import br.com.gestorfinanceiro.repositories.UserRepository;
import br.com.gestorfinanceiro.services.AuthService;


@SpringBootTest
@ActiveProfiles("test") 
class AuthServiceIT {

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
                
    //------------------TESTES DO MÉTODO REGISTER----------------------//

    @Test
    void deveRegistrarERecuperarUsuario() {
        UserEntity user = setarUsuario("jorge");

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
    void ErroAoRegistrarUsuarioComEmailJaCadastradoVerifcandoSeForamSalvos() {
        adicionarUsuario("jorge");

        adicionarUsuario("paulo");

        UserEntity user3 = setarUsuario("paulo");
        user3.setUsername("paulo2"); 

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
    
    //------------------TESTES DO MÉTODO LOGIN----------------------//
    @Test
    void deveFazerLoginERecuperarUsuario() {
        UserEntity user = adicionarUsuario("jorge");

        UserEntity userLogado = authService.login("jorge@gmail.com", "123456"); //faz login de um user cadastrado 

        assertEquals(user.getUsername(), userLogado.getUsername()); //compara se o username do usuario salvo é igual ao username do usuario logado
        assertEquals(user.getRole(), userLogado.getRole()); //compara se o role do usuario salvo é igual ao role do usuario logado
        assertEquals(user.getEmail(), userLogado.getEmail()); //compara se o usuario salvo é igual ao usuario logado
        //Se todos passarem, prova que dá para recuperar os dados
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
