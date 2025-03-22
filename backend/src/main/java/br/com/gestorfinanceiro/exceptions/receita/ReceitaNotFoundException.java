package br.com.gestorfinanceiro.exceptions.receita;

public class ReceitaNotFoundException extends RuntimeException {
    public ReceitaNotFoundException(String uuid) {
        super("Nenhuma receita encontrada para o usuário de ID: " + uuid + ".");
    }

}