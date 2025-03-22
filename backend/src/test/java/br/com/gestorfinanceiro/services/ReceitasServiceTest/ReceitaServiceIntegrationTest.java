package br.com.gestorfinanceiro.services.ReceitasServiceTest;

import br.com.gestorfinanceiro.dto.GraficoBarraDTO;
import br.com.gestorfinanceiro.dto.GraficoPizzaDTO;
import br.com.gestorfinanceiro.exceptions.InvalidDataException;
import br.com.gestorfinanceiro.exceptions.InvalidUserIdException;
import br.com.gestorfinanceiro.exceptions.InvalidUuidException;
import br.com.gestorfinanceiro.exceptions.receita.ReceitaNotFoundException;
import br.com.gestorfinanceiro.models.ReceitaEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.ReceitasCategorias;
import br.com.gestorfinanceiro.models.enums.Roles;
import br.com.gestorfinanceiro.repositories.ReceitaRepository;
import br.com.gestorfinanceiro.repositories.UserRepository;
import br.com.gestorfinanceiro.services.ReceitaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReceitaServiceIntegrationTest {

    @Autowired
    private ReceitaService receitaService;

    @Autowired
    private ReceitaRepository receitaRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        receitaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        receitaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }



    @Test
    void deveCarregarReceitaService() {
        assertNotNull(receitaService, "O ReceitaService não deveria ser nulo!");
    }

    @Nested
    class CriarReceitaTest {

        @Test
        void deveCriarReceita() {
            UserEntity user = criarUsuarioTest();
            ReceitaEntity receita = criarReceitaTest();

            ReceitaEntity receitaSalva = receitaService.criarReceita(receita, user.getUuid());

            assertNotNull(receitaSalva);
            assertEquals(receita.getValor(), receitaSalva.getValor());
            assertEquals(receita.getCategoria(), receitaSalva.getCategoria());
        }

        @Test
        void erroAoCriarReceitaNula() {
            UserEntity user = criarUsuarioTest();
            String userId = user.getUuid();
            assertThrows(InvalidDataException.class, () -> receitaService.criarReceita(null, userId));
        }

        @Test
        void erroAoCriarReceitaComValorInvalido() {
            UserEntity user = criarUsuarioTest();
            ReceitaEntity receita = criarReceitaTest();
            receita.setValor(BigDecimal.ZERO);
            String userId = user.getUuid();

            assertThrows(InvalidDataException.class, () -> receitaService.criarReceita(receita, userId));
        }

        @Test
        void erroAoCriarReceitaComValorNulo() {
            UserEntity user = criarUsuarioTest();
            ReceitaEntity receita = criarReceitaTest();
            receita.setValor(null);
            String userId = user.getUuid();

            assertThrows(InvalidDataException.class, () -> receitaService.criarReceita(receita, userId));
        }

        @Test
        void erroAoCriarReceitaComValorNegativo() {
            UserEntity user = criarUsuarioTest();
            ReceitaEntity receita = criarReceitaTest();
            receita.setValor(BigDecimal.valueOf(-100));
            String userId = user.getUuid();

            assertThrows(InvalidDataException.class, () -> receitaService.criarReceita(receita, userId));
        }

        @Test
        void erroAoCriarReceitaUsuarioNaoEncontrado() {
            ReceitaEntity receita = criarReceitaTest();
            String usuarioInexistente = UUID.randomUUID().toString();

            assertThrows(RuntimeException.class, () -> receitaService.criarReceita(receita, usuarioInexistente));
        }
    }

    @Nested
    class ListarReceitasTest {

        @Test
        void deveListarReceitasUsuario() {
            UserEntity user = criarUsuarioTest();
            receitaService.criarReceita(criarReceitaTest(), user.getUuid());

            List<ReceitaEntity> receitas = receitaService.listarReceitasUsuario(user.getUuid());
            assertFalse(receitas.isEmpty());
        }

        @Test
        void erroAoListarReceitasUsuarioComIdNuloOuVazio() {
            String uuid = null;
            assertThrows(InvalidUserIdException.class, () -> receitaService.listarReceitasUsuario(uuid));
            assertThrows(InvalidUserIdException.class, () -> receitaService.listarReceitasUsuario(""));
        }

        @Test
        void erroAoListarReceitasVazia() {
            UserEntity user = criarUsuarioTest();
            String userId = user.getUuid();

            assertThrows(ReceitaNotFoundException.class, () -> receitaService.listarReceitasUsuario(userId));
        }
    }

    @Nested
    class BuscarReceitaPorIdTest {

        @Test
        void deveBuscarReceitaPorId() {
            UserEntity user = criarUsuarioTest();
            ReceitaEntity receita = receitaService.criarReceita(criarReceitaTest(), user.getUuid());

            ReceitaEntity receitaEncontrada = receitaService.buscarReceitaPorId(receita.getUuid());
            assertEquals(receita.getUuid(), receitaEncontrada.getUuid());
        }

        @Test
        void erroAoBuscarReceitaPorIdInexistente() {
            String uuid = UUID.randomUUID().toString();
            assertThrows(ReceitaNotFoundException.class, () -> receitaService.buscarReceitaPorId(uuid));
        }

        @Test
        void erroAoBuscarReceitaPorIdNuloOuVazio() {
            String uuid = null;

            assertThrows(InvalidUuidException.class, () -> receitaService.buscarReceitaPorId(uuid));
            assertThrows(InvalidUuidException.class, () -> receitaService.buscarReceitaPorId(""));
        }
    }

    @Nested
    class AtualizarReceitaTest {

        @Test
        void deveAtualizarReceita() {
            UserEntity user = criarUsuarioTest();
            ReceitaEntity receita = receitaService.criarReceita(criarReceitaTest(), user.getUuid());

            receita.setValor(BigDecimal.valueOf(200));
            ReceitaEntity receitaAtualizada = receitaService.atualizarReceita(receita.getUuid(), receita);

            assertEquals(BigDecimal.valueOf(200), receitaAtualizada.getValor());
        }

        @Test
        void erroAoAtualizarReceitaInexistente() {
            ReceitaEntity receita = criarReceitaTest();
            String uuidInexistente = UUID.randomUUID().toString();

            assertThrows(ReceitaNotFoundException.class, () -> receitaService.atualizarReceita(uuidInexistente, receita));
        }

        @Test
        void erroAoAtualizarReceitaNula() {
            ReceitaEntity receita = null;
            String uuid = UUID.randomUUID().toString();

            assertThrows(InvalidDataException.class, () -> receitaService.atualizarReceita(uuid, receita));
        }

        @Test
        void erroAoAtualizarReceitaUuidNuloOuVazio() {
            ReceitaEntity receita = criarReceitaTest();
            String uuid = null;

            assertThrows(InvalidUuidException.class, () -> receitaService.atualizarReceita(uuid, receita));
            assertThrows(InvalidUuidException.class, () -> receitaService.atualizarReceita("", receita));
        }
    }

    @Nested
    class ExcluirReceitaTest {

        @Test
        void deveExcluirReceita() {
            UserEntity user = criarUsuarioTest();
            ReceitaEntity receita = receitaService.criarReceita(criarReceitaTest(), user.getUuid());

            receitaService.excluirReceita(receita.getUuid());
            assertFalse(receitaRepository.findById(receita.getUuid()).isPresent());
        }

        @Test
        void erroAoExcluirReceitaInexistente() {
            String uuidInexistente = UUID.randomUUID().toString();

            assertThrows(ReceitaNotFoundException.class, () -> receitaService.excluirReceita(uuidInexistente));
        }

        @Test
        void erroAoExcluirReceitaComUuidNuloOuVazio() {
            String uuidNull = null;

            assertThrows(InvalidUuidException.class, () -> receitaService.excluirReceita(uuidNull));
            assertThrows(InvalidUuidException.class, () -> receitaService.excluirReceita(""));
        }
    }

    @Nested
    class GerarGraficoTest {

        @Test
        void deveGerarGraficoBarras() {
            UserEntity user = criarUsuarioTest();

            // Criando receitas em meses diferentes
            criarReceitaComValorEData(user, BigDecimal.valueOf(100), LocalDate.of(2024, 1, 10));
            criarReceitaComValorEData(user, BigDecimal.valueOf(200), LocalDate.of(2024, 2, 15));
            criarReceitaComValorEData(user, BigDecimal.valueOf(150), LocalDate.of(2024, 2, 20));

            YearMonth inicio = YearMonth.of(2024, 1);
            YearMonth fim = YearMonth.of(2024, 2);

            GraficoBarraDTO grafico = receitaService.gerarGraficoBarras(user.getUuid(), inicio, fim);

            // Verifica se os valores por mês estão corretos
            assertNotNull(grafico);
            assertEquals(2, grafico.dadosMensais().size());
            assertEquals(
                    BigDecimal.valueOf(100).stripTrailingZeros(),
                    grafico.dadosMensais().get("janeiro 2024").stripTrailingZeros()
            );

            assertEquals(
                    BigDecimal.valueOf(350).stripTrailingZeros(),
                    grafico.dadosMensais().get("fevereiro 2024").stripTrailingZeros()
            );

        }

        @Test
        void deveGerarGraficoPizza() {
            UserEntity user = criarUsuarioTest();

            // Criando receitas de diferentes categorias
            criarReceitaComCategoriaEValor(user, BigDecimal.valueOf(300), ReceitasCategorias.SALARIO);
            criarReceitaComCategoriaEValor(user, BigDecimal.valueOf(200), ReceitasCategorias.BOLSA_DE_ESTUDOS);
            criarReceitaComCategoriaEValor(user, BigDecimal.valueOf(100), ReceitasCategorias.BONUS);

            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 12, 31);

            GraficoPizzaDTO grafico = receitaService.gerarGraficoPizza(user.getUuid(), inicio, fim);

            // Verifica se os valores por categoria estão corretos
            assertNotNull(grafico);
            assertEquals(3, grafico.categorias().size());
            assertEquals(BigDecimal.valueOf(300).stripTrailingZeros(), grafico.categorias().get("SALARIO").stripTrailingZeros());
            assertEquals(BigDecimal.valueOf(200).stripTrailingZeros(), grafico.categorias().get("BOLSA_DE_ESTUDOS").stripTrailingZeros());
            assertEquals(BigDecimal.valueOf(100).stripTrailingZeros(), grafico.categorias().get("BONUS").stripTrailingZeros());


        }
    }

    @Nested
    class BuscaAvancadaTest {

        @Test
        void deveBuscarReceitasPorIntervaloDeDatas() {
            UserEntity user = criarUsuarioTest();
            String userId = user.getUuid();

            // Criando receitas em datas diferentes
            criarReceitaComValorEData(user, BigDecimal.valueOf(100), LocalDate.of(2024, 1, 10));
            criarReceitaComValorEData(user, BigDecimal.valueOf(200), LocalDate.of(2024, 2, 15));
            criarReceitaComValorEData(user, BigDecimal.valueOf(150), LocalDate.of(2024, 2, 20));

            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 2, 18);

            List<ReceitaEntity> receitas = receitaService.buscarReceitasPorIntervaloDeDatas(userId, inicio, fim);

            // Verifica se as receitas estão corretas
            assertNotNull(receitas);
            assertEquals(2, receitas.size());
            assertEquals(BigDecimal.valueOf(100).stripTrailingZeros(), receitas.get(0).getValor().stripTrailingZeros());
            assertEquals(BigDecimal.valueOf(200).stripTrailingZeros(), receitas.get(1).getValor().stripTrailingZeros());
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeDatasComUserIdNuloOuVazio() {
            LocalDate inicio = LocalDate.of(2024, 1, 1);
            LocalDate fim = LocalDate.of(2024, 2, 18);

            assertThrows(InvalidUserIdException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas(null, inicio, fim));
            assertThrows(InvalidUserIdException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas("", inicio, fim));
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeDatasComDatasNulas() {
            UserEntity user = criarUsuarioTest();
            String userId = user.getUuid();
            LocalDate inicio = null;
            LocalDate inicio2 = LocalDate.of(2024, 1, 1);
            LocalDate fim = null;
            LocalDate fim2 = LocalDate.of(2024, 2, 18);

            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas(userId, inicio, fim2));
            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas(userId, inicio2, fim));
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeDatasComInicioDepoisDoFim() {
            UserEntity user = criarUsuarioTest();
            String userId = user.getUuid();
            LocalDate inicio = LocalDate.of(2024, 1, 10);
            LocalDate fim = LocalDate.of(2024, 1, 5);

            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeDatas(userId, inicio, fim));
        }

        @Test
        void deveBuscarReceitasPorIntervaloDeValores() {
            UserEntity user = criarUsuarioTest();
            String userId = user.getUuid();

            // Criando receitas com valores diferentes
            criarReceitaComValorEData(user, BigDecimal.valueOf(100), LocalDate.of(2024, 1, 10));
            criarReceitaComValorEData(user, BigDecimal.valueOf(200), LocalDate.of(2024, 2, 15));
            criarReceitaComValorEData(user, BigDecimal.valueOf(150), LocalDate.of(2024, 2, 20));

            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            List<ReceitaEntity> receitas = receitaService.buscarReceitasPorIntervaloDeValores(userId, min, max);

            // Verifica se as receitas estão corretas
            assertNotNull(receitas);
            assertEquals(2, receitas.size());
            assertEquals(BigDecimal.valueOf(100).stripTrailingZeros(), receitas.get(0).getValor().stripTrailingZeros());
            assertEquals(BigDecimal.valueOf(150).stripTrailingZeros(), receitas.get(1).getValor().stripTrailingZeros());
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeValoresComUserIdNuloOuVazio() {
            BigDecimal min = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.valueOf(150);

            assertThrows(InvalidUserIdException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(null, min, max));
            assertThrows(InvalidUserIdException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores("", min, max));
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeValoresComValoresNulos() {
            UserEntity user = criarUsuarioTest();
            String userId = user.getUuid();
            BigDecimal min = null;
            BigDecimal min2 = BigDecimal.valueOf(100);
            BigDecimal max = null;
            BigDecimal max2 = BigDecimal.valueOf(150);

            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, min, max2));
            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, min2, max));
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeValoresComValoresMenoresOuIguaisAZero() {
            UserEntity user = criarUsuarioTest();
            String userId = user.getUuid();
            BigDecimal min = BigDecimal.ZERO;
            BigDecimal min2 = BigDecimal.valueOf(100);
            BigDecimal max = BigDecimal.ZERO;
            BigDecimal max2 = BigDecimal.valueOf(150);

            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, min, max2));
            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, min2, max));
        }

        @Test
        void erroAoBuscarReceitasPorIntervaloDeValoresComMinMaiorQueMax() {
            UserEntity user = criarUsuarioTest();
            String userId = user.getUuid();
            BigDecimal min = BigDecimal.valueOf(200);
            BigDecimal max = BigDecimal.valueOf(100);

            assertThrows(InvalidDataException.class, () -> receitaService.buscarReceitasPorIntervaloDeValores(userId, min, max));
        }
    }

    //----------------- Métodos Auxiliares -----------------//

    private UserEntity criarUsuarioTest() {
        UserEntity user = new UserEntity();
        user.setUsername("Jorge");
        user.setEmail("jorge@gmail.com");
        user.setPassword("123456");
        user.setRole(Roles.USER);
        return userRepository.save(user);
    }

    private ReceitaEntity criarReceitaTest() {
        ReceitaEntity receita = new ReceitaEntity();
        receita.setData(LocalDate.now());
        receita.setValor(BigDecimal.valueOf(100));
        receita.setCategoria(ReceitasCategorias.SALARIO);
        receita.setOrigemDoPagamento("Teste");
        receita.setObservacoes("Teste");
        return receita;
    }

    private void criarReceitaComValorEData(UserEntity user, BigDecimal valor, LocalDate data) {
        ReceitaEntity receita = new ReceitaEntity();
        receita.setData(data);
        receita.setValor(valor);
        receita.setCategoria(ReceitasCategorias.SALARIO);
        receita.setOrigemDoPagamento("Teste");
        receita.setObservacoes("Salario teste");
        receita.setUser(user);
        receitaService.criarReceita(receita, user.getUuid());
    }

    private void criarReceitaComCategoriaEValor(UserEntity user, BigDecimal valor, ReceitasCategorias categoria) {
        ReceitaEntity receita = new ReceitaEntity();
        receita.setData(LocalDate.of(2024, 1, 15));
        receita.setValor(valor);
        receita.setCategoria(categoria);
        receita.setOrigemDoPagamento("Teste");
        receita.setObservacoes("Salario teste");
        receita.setUser(user);
        receitaService.criarReceita(receita, user.getUuid());
    }

}
