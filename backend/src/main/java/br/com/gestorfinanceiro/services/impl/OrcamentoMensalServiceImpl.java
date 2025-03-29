package br.com.gestorfinanceiro.services.impl;

import br.com.gestorfinanceiro.exceptions.OrcamentoMensal.OrcamentoMensalAlreadyExistsException;
import br.com.gestorfinanceiro.exceptions.OrcamentoMensal.OrcamentoMensalNotFoundException;
import br.com.gestorfinanceiro.exceptions.categoria.CategoriaNotFoundException;
import br.com.gestorfinanceiro.exceptions.user.UserNotFoundException;
import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.CategoriaType;
import br.com.gestorfinanceiro.repositories.CategoriaRepository;
import br.com.gestorfinanceiro.repositories.OrcamentoMensalRepository;
import br.com.gestorfinanceiro.repositories.UserRepository;
import br.com.gestorfinanceiro.services.OrcamentoMensalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
public class OrcamentoMensalServiceImpl implements OrcamentoMensalService {

    private final OrcamentoMensalRepository orcamentoMensalRepository;
    private final CategoriaRepository categoriaRepository;
    private final UserRepository userRepository;

    public OrcamentoMensalServiceImpl(OrcamentoMensalRepository orcamentoMensalRepository,
                                      UserRepository userRepository,
                                      CategoriaRepository categoriaRepository) {
        this.orcamentoMensalRepository = Objects.requireNonNull(orcamentoMensalRepository, "OrcamentoMensalRepository não pode ser nulo");
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository não pode ser nulo");
        this.categoriaRepository = Objects.requireNonNull(categoriaRepository, "CategoriaRepository não pode ser nulo");
    }

    @Override
    public List<OrcamentoMensalEntity> listarTodosPorUsuario(String userId) {
        List<OrcamentoMensalEntity> orcamentosMensais = orcamentoMensalRepository.findByUserId(userId);
        if (orcamentosMensais.isEmpty()) throw new OrcamentoMensalNotFoundException(userId);
        return orcamentosMensais;
    }

    @Override
    public List<OrcamentoMensalEntity> listarPorPeriodo(String userId, YearMonth periodo) {
        List<OrcamentoMensalEntity> orcamentosMensais = orcamentoMensalRepository.findByPeriodo(periodo);
        if (orcamentosMensais.isEmpty()) throw new OrcamentoMensalNotFoundException(userId);
        return orcamentosMensais;
    }

    @Override
    public OrcamentoMensalEntity buscarPorId(String userId, String uuid) {
        return orcamentoMensalRepository.findByUuidAndUserUuid(uuid, userId)
                .orElseThrow(() -> new OrcamentoMensalNotFoundException(uuid));
    }

    @Override
    @Transactional
    public OrcamentoMensalEntity criarOrcamentoMensal(String userId, String categoria, BigDecimal valorLimite, YearMonth periodo) {
        validarParametros(userId, categoria, valorLimite, periodo);

        UserEntity user = buscarUsuarioPorId(userId);

        // Busca a categoria de forma robusta, lançando exceção caso não encontre
        CategoriaEntity categoriaEntity = buscarCategoria(userId, categoria);

        // Verifica se já existe um orçamento para a categoria e o período
        verificarOrcamentoDuplicado(userId, categoriaEntity, periodo, null);

        // Cria o orçamento mensal
        OrcamentoMensalEntity orcamentoMensal = new OrcamentoMensalEntity();
        orcamentoMensal.setCategoria(categoriaEntity);
        orcamentoMensal.setValorLimite(valorLimite);
        orcamentoMensal.setPeriodo(periodo);
        orcamentoMensal.setUser(user);

        // Salva no repositório
        return orcamentoMensalRepository.save(orcamentoMensal);
    }


    @Override
    @Transactional
    public OrcamentoMensalEntity atualizarOrcamentoMensal(String userId, String uuid, String categoria, BigDecimal valorLimite, YearMonth periodo) {
        validarParametros(userId, categoria, valorLimite, periodo);
        Objects.requireNonNull(uuid, "UUID não pode ser nulo");

        OrcamentoMensalEntity orcamentoMensal = buscarPorId(userId, uuid);
        CategoriaEntity categoriaEntity = buscarCategoria(userId, categoria);

        verificarOrcamentoDuplicado(userId, categoriaEntity, periodo, uuid);

        orcamentoMensal.setCategoria(categoriaEntity);
        orcamentoMensal.setValorLimite(valorLimite);
        orcamentoMensal.setPeriodo(periodo);

        return orcamentoMensalRepository.save(orcamentoMensal);
    }

    @Override
    @Transactional
    public void excluirOrcamentoMensal(String userId, String uuid) {
        Objects.requireNonNull(userId, "UserId não pode ser nulo");
        Objects.requireNonNull(uuid, "UUID não pode ser nulo");

        OrcamentoMensalEntity orcamentoMensal = buscarPorId(userId, uuid);
        orcamentoMensalRepository.delete(orcamentoMensal);
    }

    /**
     * Valida os parâmetros de entrada.
     */
    private void validarParametros(String userId, String categoria, BigDecimal valorLimite, YearMonth periodo) {
        Objects.requireNonNull(userId, "UserId não pode ser nulo");
        Objects.requireNonNull(categoria, "Categoria não pode ser nula");
        Objects.requireNonNull(valorLimite, "ValorLimite não pode ser nulo");
        Objects.requireNonNull(periodo, "Período não pode ser nulo");

        if (valorLimite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("ValorLimite deve ser maior que zero");
        }
    }

    /**
     * Busca um usuário pelo ID.
     */
    private UserEntity buscarUsuarioPorId(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Busca uma categoria pelo nome e usuário.
     */
    private CategoriaEntity buscarCategoria(String userId, String categoriaNome) {
        return categoriaRepository.findByNomeAndUserUuid(categoriaNome, userId)
                .orElseThrow(() -> new CategoriaNotFoundException("Categoria não encontrada para o usuário: " + categoriaNome));
    }

    /**
     * Verifica se já existe um orçamento com a mesma categoria e período.
     */
    private void verificarOrcamentoDuplicado(String userId, CategoriaEntity categoria, YearMonth periodo, String uuidAtual) {
        orcamentoMensalRepository.findByCategoriaAndPeriodoAndUserUuid(categoria, periodo, userId)
                .filter(existing -> uuidAtual == null || !existing.getUuid().equals(uuidAtual))
                .ifPresent(existing -> {
                    throw new OrcamentoMensalAlreadyExistsException(categoria, periodo);
                });
    }
}
