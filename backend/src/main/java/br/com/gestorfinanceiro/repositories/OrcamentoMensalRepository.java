package br.com.gestorfinanceiro.repositories;

import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.repositories.custom.OrcamentoMensalRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrcamentoMensalRepository extends JpaRepository<OrcamentoMensalEntity, String>, OrcamentoMensalRepositoryCustom {

}
