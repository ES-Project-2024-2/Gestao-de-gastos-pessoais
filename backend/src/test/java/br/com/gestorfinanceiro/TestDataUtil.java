package br.com.gestorfinanceiro;

import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.Roles;

public class TestDataUtil {
    public static UserEntity criarUsuarioEntityUtil(String nome) {
        UserEntity user = new UserEntity();
        user.setUsername(nome);
        user.setEmail(nome + "@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);

        return user;
    }
}
