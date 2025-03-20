package br.com.gestorfinanceiro;

import br.com.gestorfinanceiro.models.UserEntity;

public class TestDataUtil {
    public static UserEntity criarUsuarioEntityUtil(String nome) {
        UserEntity user = new UserEntity();
        user.setUsername(nome + "@gmail.com");
        user.setPassword("123456");
        return user;
    }
}
