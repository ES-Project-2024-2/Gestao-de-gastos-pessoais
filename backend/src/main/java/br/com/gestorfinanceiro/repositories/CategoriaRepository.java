package br.com.gestorfinanceiro.repositories;

import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.enums.CategoriaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, String> {
    List<CategoriaEntity> findAllByUserUuid(String userId);

    // Encontrar categoria por nome, tipo, e id do usuário
    Optional<CategoriaEntity> findByNomeAndTipoAndUserUuid(String nome, CategoriaType tipo, String userId);
    Optional<CategoriaEntity> findByNome(String nome);

    Optional<CategoriaEntity> findByNomeAndUserUuid(String categoriaNome, String userId);
}
