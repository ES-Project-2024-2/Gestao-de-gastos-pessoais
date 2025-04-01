package br.com.gestorfinanceiro.controllers.DespesaControllerTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.gestorfinanceiro.dto.despesa.DespesaCreateDTO;
import br.com.gestorfinanceiro.dto.despesa.DespesaUpdateDTO;
import br.com.gestorfinanceiro.dto.user.LoginDTO;
import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.DespesaEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.CategoriaType;
import br.com.gestorfinanceiro.models.enums.Roles;
import br.com.gestorfinanceiro.repositories.CategoriaRepository;
import br.com.gestorfinanceiro.repositories.DespesaRepository;
import br.com.gestorfinanceiro.repositories.UserRepository;
import jakarta.transaction.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.hamcrest.Matchers.hasSize;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;


@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled
class DespesaControllerIntegrationTest {

    private static final String CATEGORIA_PADRAO = "Alimentacao";
    private static final String DESTINO_PAGAMENTO_PADRAO = "Mercado";
    private static final String OBSERVACOES_PADRAO = "Compras do mês";
    private static final BigDecimal VALOR_PADRAO = BigDecimal.valueOf(100);
    private static final BigDecimal VALOR_ATUALIZADO = BigDecimal.valueOf(200);
    private static final BigDecimal VALOR_NEGATIVO = BigDecimal.valueOf(-100);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DespesaRepository despesaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity user;
    private String authHeader;

    @BeforeEach
    void setUp() throws Exception {
        limparBaseDeDados();
        user = criarUsuarioTest();
        criarCategoriaTest(CATEGORIA_PADRAO, user);
        
        authHeader = "Bearer " + obterTokenJwt();
    }

    private String obterTokenJwt() throws Exception {
        LoginDTO loginDTO = new LoginDTO(user.getEmail(), "123456");
        
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();
        
        return objectMapper.readValue(result.getResponse().getContentAsString(), 
            new TypeReference<Map<String, String>>() {}).get("token");
    }

    private void limparBaseDeDados() {
        despesaRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void deveCarregarContextoAplicacao() {
        assertNotNull(mockMvc, "O MockMvc não deveria ser nulo!");
    }

    @Nested
    class CriarDespesaTest {
        @Test
        void deveCriarDespesa() throws Exception {
            criarCategoriaTest(CATEGORIA_PADRAO, user);
            DespesaCreateDTO dto = criarDespesaCreateDTO();
            
            mockMvc.perform(post("/despesas")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value(CATEGORIA_PADRAO)); // Verifica Categoria como String
        }

        @Test
        void erroAoCriarDespesaComValorInvalido() throws Exception {
            DespesaCreateDTO despesaDTO = criarDespesaCreateDTO(
                    BigDecimal.ZERO, LocalDate.now(), CATEGORIA_PADRAO
            );

            mockMvc.perform(post("/despesas")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(despesaDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ListarDespesasTest {
        @Test
        void deveListarDespesasUsuario() throws Exception {
            criarDespesaNoBanco();

            mockMvc.perform(get("/despesas")
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    class BuscarDespesaPorIdTest {
        @Test
        void deveBuscarDespesaPorId() throws Exception {
            DespesaEntity despesa = criarDespesaNoBanco();

            mockMvc.perform(get("/despesas/" + despesa.getUuid())
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(despesa.getUuid()));
        }

        @Test
        void erroAoBuscarDespesaPorIdInexistente() throws Exception {
            String uuid = UUID.randomUUID().toString();

            mockMvc.perform(get("/despesas/" + uuid)
                    .header("Authorization", authHeader))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class AtualizarDespesaTest {
        @Test
        void deveAtualizarDespesa() throws Exception {
            DespesaEntity despesa = criarDespesaNoBanco();
            DespesaUpdateDTO despesaUpdateDTO = criarDespesaUpdateDTO(despesa);

            mockMvc.perform(put("/despesas/" + despesa.getUuid())
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(despesaUpdateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valor").value(VALOR_ATUALIZADO.doubleValue()));
        }

        @Test
        void erroAoAtualizarDespesaInexistente() throws Exception {
            DespesaUpdateDTO despesa = criarDespesaUpdateDTO();
            String uuidInexistente = UUID.randomUUID().toString();

            mockMvc.perform(put("/despesas/" + uuidInexistente)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(despesa)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ExcluirDespesaTest {
        @Test
        void deveExcluirDespesa() throws Exception {
            DespesaEntity despesa = criarDespesaNoBanco();

            mockMvc.perform(delete("/despesas/" + despesa.getUuid())
                    .header("Authorization", authHeader))
                    .andExpect(status().isNoContent());

            assertFalse(despesaRepository.findById(despesa.getUuid()).isPresent());
        }

        @Test
        void erroAoExcluirDespesaInexistente() throws Exception {
            String uuidInexistente = UUID.randomUUID().toString();

            mockMvc.perform(delete("/despesas/" + uuidInexistente)
                    .header("Authorization", authHeader))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GerarGraficosTest {
        @Test
        void deveGerarGraficoBarras() throws Exception {
            YearMonth mesAtual = YearMonth.now();
            criarDespesaNoBanco(VALOR_PADRAO, mesAtual.atDay(1));
            
            mockMvc.perform(get("/despesas/grafico-barras")
                    .header("Authorization", authHeader)
                    .param("inicio", mesAtual.minusMonths(1).toString())
                    .param("fim", mesAtual.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dadosMensais").exists())
                    .andExpect(jsonPath("$.dadosMensais").isMap())
                    .andExpect(jsonPath("$.dadosMensais").isNotEmpty());
        }

        @Test
        void deveGerarGraficoPizza() throws Exception {
            despesaRepository.deleteAll();
            categoriaRepository.deleteAll();
            
            CategoriaEntity categoria = criarCategoriaTest(CATEGORIA_PADRAO, user);
            criarDespesaNoBanco(VALOR_PADRAO, LocalDate.now(), categoria.getNome());
            
            mockMvc.perform(get("/despesas/grafico-pizza")
                .header("Authorization", authHeader)
                .param("inicio", LocalDate.now().minusMonths(1).toString())
                .param("fim", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias." + CATEGORIA_PADRAO).exists());
        }
    }

    @Nested
    class BuscaAvancadaTest {
        @Test
        void deveBuscarDespesasPorIntervaloDeDatas() throws Exception {
            LocalDate hoje = LocalDate.now();
            criarDespesaNoBanco(VALOR_PADRAO, hoje);
            
            mockMvc.perform(get("/despesas/por-intervalo-de-datas")
                .header("Authorization", authHeader)
                .param("inicio", hoje.minusDays(1).toString())
                .param("fim", hoje.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void deveBuscarDespesasPorIntervaloDeValores() throws Exception {
            criarDespesaNoBanco(BigDecimal.valueOf(100), LocalDate.now());
            criarDespesaNoBanco(BigDecimal.valueOf(200), LocalDate.now());
            
            mockMvc.perform(get("/despesas/por-intervalo-de-valores")
                    .header("Authorization", authHeader)
                    .param("min", "100")
                    .param("max", "150"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].valor").value(100.0));
        }
    }

    @Test
    void erroAoCriarDespesaSemCategoria() throws Exception {
        DespesaCreateDTO dto = criarDespesaCreateDTO(VALOR_PADRAO, LocalDate.now(), null);
        mockMvc.perform(post("/despesas")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void erroAoCriarDespesaComValorNegativo() throws Exception {
        DespesaCreateDTO dto = criarDespesaCreateDTO(VALOR_NEGATIVO, LocalDate.now(), CATEGORIA_PADRAO);
        mockMvc.perform(post("/despesas")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarVariasDespesas() throws Exception {
        criarDespesaNoBanco();
        criarDespesaNoBanco(VALOR_ATUALIZADO, LocalDate.now().minusDays(1));
        
        mockMvc.perform(get("/despesas")
                .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void erroAoAtualizarDespesaComDadosInvalidos() throws Exception {
        DespesaEntity despesa = criarDespesaNoBanco();
        DespesaUpdateDTO despesaUpdateDTO = criarDespesaUpdateDTO();
        despesaUpdateDTO.setValor(null);

        mockMvc.perform(put("/despesas/" + despesa.getUuid())
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(despesaUpdateDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void erroBuscaAvancadaComDatasInvalidas() throws Exception {
        mockMvc.perform(get("/despesas/por-intervalo-de-datas")
                .header("Authorization", authHeader)
                .param("inicio", "2025-13-01")
                .param("fim", "2025-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void erroAoGerarGraficoComIntervaloInvalido() throws Exception {
        mockMvc.perform(get("/despesas/grafico-barras")
                .header("Authorization", authHeader)
                .param("inicio", "2025-01-01")
                .param("fim", "2024-12-31"))
                .andExpect(status().isBadRequest());
    }

    @Nested
    class AutorizacaoTest {
        private UserEntity outroUsuario;
        private String authHeaderOutroUsuario;
    
        @BeforeEach
        void setUpOutroUsuario() throws Exception {
            outroUsuario = new UserEntity();
            outroUsuario.setUsername("OutroUsuario");
            outroUsuario.setEmail("outro@email.com");
            outroUsuario.setPassword(passwordEncoder.encode("654321"));
            outroUsuario.setRole(Roles.USER);
            userRepository.saveAndFlush(outroUsuario);
    
            authHeaderOutroUsuario = "Bearer " + obterTokenJwt(outroUsuario.getEmail(), "654321");
        }
    
        private String obterTokenJwt(String email, String senha) throws Exception {
            LoginDTO loginDTO = new LoginDTO(email, senha);
            
            MvcResult result = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk())
                    .andReturn();
            
            return objectMapper.readValue(result.getResponse().getContentAsString(), 
                new TypeReference<Map<String, String>>() {}).get("token");
        }
    
        @Test
        void erroQuandoUsuarioTentaAcessarDespesaDeOutroUsuario() throws Exception {
            DespesaEntity despesa = criarDespesaNoBanco();
    
            mockMvc.perform(get("/despesas/" + despesa.getUuid())
                    .header("Authorization", authHeaderOutroUsuario))
                    .andExpect(status().isForbidden());
        }
    
        @Test
        void erroQuandoUsuarioTentaAtualizarDespesaDeOutroUsuario() throws Exception {
            DespesaEntity despesa = criarDespesaNoBanco();
            DespesaUpdateDTO despesaUpdateDTO = criarDespesaUpdateDTO(despesa);
    
            mockMvc.perform(put("/despesas/" + despesa.getUuid())
                    .header("Authorization", authHeaderOutroUsuario)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(despesaUpdateDTO)))
                    .andExpect(status().isForbidden());
        }
    
        @Test
        void erroQuandoUsuarioTentaExcluirDespesaDeOutroUsuario() throws Exception {
            DespesaEntity despesa = criarDespesaNoBanco();
    
            mockMvc.perform(delete("/despesas/" + despesa.getUuid())
                    .header("Authorization", authHeaderOutroUsuario))
                    .andExpect(status().isForbidden());
        }
    }
    
    @Nested
    class EdgeCasesTest {
        @Test
        void deveLidarComValoresMuitoAltos() throws Exception {
            BigDecimal valorMuitoAlto = new BigDecimal("999999999999999.99");
            DespesaCreateDTO dto = criarDespesaCreateDTO(valorMuitoAlto, LocalDate.now(), CATEGORIA_PADRAO);
            
            mockMvc.perform(post("/despesas")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.valor").value(valorMuitoAlto.doubleValue()));
        }
    
        @Test
        void deveLidarComDatasExtremas() throws Exception {
            LocalDate dataFuturaExtrema = LocalDate.now().plusYears(10);
            LocalDate dataPassadoExtremo = LocalDate.of(1900, 1, 1);
    
            // Teste com data futura extrema
            DespesaCreateDTO dtoFuturo = criarDespesaCreateDTO(VALOR_PADRAO, dataFuturaExtrema, CATEGORIA_PADRAO);
            mockMvc.perform(post("/despesas")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dtoFuturo)))
                    .andExpect(status().isCreated());
    
            // Teste com data passado extrema
            DespesaCreateDTO dtoPassado = criarDespesaCreateDTO(VALOR_PADRAO, dataPassadoExtremo, CATEGORIA_PADRAO);
            mockMvc.perform(post("/despesas")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dtoPassado)))
                    .andExpect(status().isCreated());
        }
    
        @Test
        void deveLidarComCategoriasEspeciais() throws Exception {
            String categoriaEspecial = "Despesa @#$% 123";
            criarCategoriaTest(categoriaEspecial, user);
            
            DespesaCreateDTO dto = criarDespesaCreateDTO(VALOR_PADRAO, LocalDate.now(), categoriaEspecial);
            
            mockMvc.perform(post("/despesas")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.categoria").value(categoriaEspecial));
        }
    
        @Test
        void deveLidarComObservacoesLongas() throws Exception {
            String observacoesLongas = "a".repeat(1000);
            DespesaCreateDTO dto = criarDespesaCreateDTO();
            dto.setObservacoes(observacoesLongas);
            
            mockMvc.perform(post("/despesas")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.observacoes").value(observacoesLongas));
        }
    
        @Test
        void deveLidarComFormasPagamentoEspeciais() throws Exception {
            String formaPagamentoEspecial = "Pagamento @#$% 123";
            DespesaCreateDTO dto = criarDespesaCreateDTO();
            dto.setDestinoPagamento(formaPagamentoEspecial);
            
            mockMvc.perform(post("/despesas")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.destinoPagamento").value(formaPagamentoEspecial));
        }
    }

    private UserEntity criarUsuarioTest() {
        UserEntity newUser = new UserEntity();
        newUser.setUsername("Lana");
        newUser.setEmail("lana@gmail.com");
        newUser.setPassword(passwordEncoder.encode("123456"));
        newUser.setRole(Roles.USER);
        
        return userRepository.saveAndFlush(newUser);
    }

    private CategoriaEntity criarCategoriaTest(String nome, UserEntity user) {
        return categoriaRepository.findByNomeAndTipoAndUserUuid(nome, CategoriaType.DESPESAS, user.getUuid())
            .orElseGet(() -> {
                CategoriaEntity categoria = new CategoriaEntity();
                categoria.setNome(nome);
                categoria.setTipo(CategoriaType.DESPESAS);
                categoria.setUser(user);
                return categoriaRepository.save(categoria);
            });
    }

    private DespesaEntity criarDespesaNoBanco() {
        return criarDespesaNoBanco(VALOR_PADRAO, LocalDate.now(), CATEGORIA_PADRAO);
    }
    
    private DespesaEntity criarDespesaNoBanco(BigDecimal valor, LocalDate data) {
        return criarDespesaNoBanco(valor, data, CATEGORIA_PADRAO);
    }
    
    private DespesaEntity criarDespesaNoBanco(BigDecimal valor, LocalDate data, String categoriaNome) {
        CategoriaEntity categoria = categoriaRepository.findByNomeAndTipoAndUserUuid(
                categoriaNome, CategoriaType.DESPESAS, user.getUuid())
            .orElseGet(() -> {
                CategoriaEntity novaCategoria = new CategoriaEntity();
                novaCategoria.setNome(categoriaNome);
                novaCategoria.setTipo(CategoriaType.DESPESAS);
                novaCategoria.setUser(user);
                return categoriaRepository.save(novaCategoria);
            });
    
        DespesaEntity despesa = new DespesaEntity();
        despesa.setValor(valor);
        despesa.setData(data);
        despesa.setCategoria(categoria);
        despesa.setDestinoPagamento(DESTINO_PAGAMENTO_PADRAO);
        despesa.setObservacoes(OBSERVACOES_PADRAO);
        despesa.setUser(user);
        
        return despesaRepository.save(despesa);
    }

    private DespesaCreateDTO criarDespesaCreateDTO(BigDecimal valor, LocalDate data, String categoria) {
        DespesaCreateDTO dto = new DespesaCreateDTO();
        dto.setValor(valor);
        dto.setData(data);
        dto.setCategoria(categoria);
        dto.setDestinoPagamento(DESTINO_PAGAMENTO_PADRAO);
        dto.setObservacoes(OBSERVACOES_PADRAO);
        return dto;
    }

    private DespesaCreateDTO criarDespesaCreateDTO() {
        return criarDespesaCreateDTO(VALOR_PADRAO, LocalDate.now(), CATEGORIA_PADRAO);
    }

    private DespesaUpdateDTO criarDespesaUpdateDTO(DespesaEntity despesa) {
        DespesaUpdateDTO dto = new DespesaUpdateDTO();
        dto.setValor(VALOR_ATUALIZADO);
        dto.setData(despesa.getData());
        dto.setCategoria(despesa.getCategoria().getNome());
        dto.setDestinoPagamento(despesa.getDestinoPagamento());
        dto.setObservacoes(despesa.getObservacoes());
        return dto;
    }

    private DespesaUpdateDTO criarDespesaUpdateDTO() {
        DespesaUpdateDTO dto = new DespesaUpdateDTO();
        dto.setValor(VALOR_ATUALIZADO);
        dto.setData(LocalDate.now());
        dto.setCategoria(CATEGORIA_PADRAO);
        dto.setDestinoPagamento(DESTINO_PAGAMENTO_PADRAO);
        dto.setObservacoes(OBSERVACOES_PADRAO);
        return dto;
    }
}