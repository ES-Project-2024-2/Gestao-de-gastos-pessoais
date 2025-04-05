package br.com.gestorfinanceiro.controllers.OrcamentoMensalControllerTest;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.gestorfinanceiro.controller.OrcamentoMensalController.OrcamentoMensalRequest;
import br.com.gestorfinanceiro.dto.orcamentomensal.OrcamentoMensalDTO;
import br.com.gestorfinanceiro.dto.user.LoginDTO;
import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.CategoriaType;
import br.com.gestorfinanceiro.models.enums.Roles;
import br.com.gestorfinanceiro.repositories.CategoriaRepository;
import br.com.gestorfinanceiro.repositories.OrcamentoMensalRepository;
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
class OrcamentoMensalControllerIntegrationTest {

    private static final String CATEGORIA_PADRAO = "Alimentacao";
    private static final BigDecimal VALOR_LIMITE_PADRAO = BigDecimal.valueOf(1000);
    private static final BigDecimal VALOR_LIMITE_ATUALIZADO = BigDecimal.valueOf(1500);
    private static final YearMonth PERIODO_PADRAO = YearMonth.now();
    private static final YearMonth PERIODO_ATUALIZADO = YearMonth.now().plusMonths(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrcamentoMensalRepository orcamentoMensalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity user;
    private String authHeader;

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

    @BeforeEach
    void setUp() throws Exception {
        limparBaseDeDados();
        user = criarUsuarioTest();
        criarCategoriaTest(CATEGORIA_PADRAO, user);
        authHeader = "Bearer " + obterTokenJwt(user.getEmail(), "123456");
    }

    private void limparBaseDeDados() {
        orcamentoMensalRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void deveCarregarContextoAplicacao() {
        assertNotNull(mockMvc, "O MockMvc não deveria ser nulo!");
    }

    @Nested
    class CriarOrcamentoTest {
        @Test
        void deveCriarOrcamento() throws Exception {
            OrcamentoMensalDTO dto = criarOrcamentoMensalDTO();
            
            mockMvc.perform(post("/orcamento-mensal")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value(CATEGORIA_PADRAO))
                .andExpect(jsonPath("$.valorLimite").value(VALOR_LIMITE_PADRAO.doubleValue()))
                .andExpect(header().exists("Location"));
        }

        @Test
        void erroAoCriarOrcamentoComValorInvalido() throws Exception {
            OrcamentoMensalDTO dto = criarOrcamentoMensalDTO();
            dto.setValorLimite(BigDecimal.ZERO);

            mockMvc.perform(post("/orcamento-mensal")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ListarOrcamentosTest {
        @Test
        void deveListarOrcamentosDoUsuario() throws Exception {
            criarOrcamentoNoBanco();

            mockMvc.perform(get("/orcamento-mensal")
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void deveListarOrcamentosPorPeriodo() throws Exception {
            criarOrcamentoNoBanco();

            mockMvc.perform(get("/orcamento-mensal/periodo/" + PERIODO_PADRAO)
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    class BuscarOrcamentoPorIdTest {
        @Test
        void deveBuscarOrcamentoPorId() throws Exception {
            OrcamentoMensalEntity orcamento = criarOrcamentoNoBanco();

            mockMvc.perform(get("/orcamento-mensal/" + orcamento.getUuid())
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(orcamento.getUuid()));
        }

        @Test
        void erroAoBuscarOrcamentoPorIdInexistente() throws Exception {
            String uuid = UUID.randomUUID().toString();

            mockMvc.perform(get("/orcamento-mensal/" + uuid)
                    .header("Authorization", authHeader))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class AtualizarOrcamentoTest {
        @Test
        void deveAtualizarOrcamento() throws Exception {
            OrcamentoMensalEntity orcamento = criarOrcamentoNoBanco();
            OrcamentoMensalRequest request = new OrcamentoMensalRequest(
                CATEGORIA_PADRAO, 
                VALOR_LIMITE_ATUALIZADO, 
                PERIODO_ATUALIZADO
            );

            mockMvc.perform(put("/orcamento-mensal/" + orcamento.getUuid())
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valorLimite").value(VALOR_LIMITE_ATUALIZADO.doubleValue()));
        }

        @Test
        void erroAoAtualizarOrcamentoInexistente() throws Exception {
            OrcamentoMensalRequest request = new OrcamentoMensalRequest(
                CATEGORIA_PADRAO, 
                VALOR_LIMITE_ATUALIZADO, 
                PERIODO_ATUALIZADO
            );
            String uuidInexistente = UUID.randomUUID().toString();

            mockMvc.perform(put("/orcamento-mensal/" + uuidInexistente)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ExcluirOrcamentoTest {
        @Test
        void deveExcluirOrcamento() throws Exception {
            OrcamentoMensalEntity orcamento = criarOrcamentoNoBanco();

            mockMvc.perform(delete("/orcamento-mensal/" + orcamento.getUuid())
                    .header("Authorization", authHeader))
                    .andExpect(status().isNoContent());

            assertFalse(orcamentoMensalRepository.findById(orcamento.getUuid()).isPresent());
        }

        @Test
        void erroAoExcluirOrcamentoInexistente() throws Exception {
            String uuidInexistente = UUID.randomUUID().toString();

            mockMvc.perform(delete("/orcamento-mensal/" + uuidInexistente)
                    .header("Authorization", authHeader))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class AutorizacaoTest {
        private UserEntity outroUsuario;
        private String authHeaderOutroUsuario;
        private OrcamentoMensalEntity orcamentoOutroUsuario;
        private OrcamentoMensalEntity orcamentoUsuarioPrincipal;
    
        @BeforeEach
        void setUpOutroUsuario() throws Exception {
            // Criar outro usuário
            outroUsuario = new UserEntity();
            outroUsuario.setUsername("OutroUsuario");
            outroUsuario.setEmail("outro@email.com");
            outroUsuario.setPassword(passwordEncoder.encode("654321"));
            outroUsuario.setRole(Roles.USER);
            userRepository.saveAndFlush(outroUsuario);
    
            // Criar categoria para o outro usuário
            CategoriaEntity categoriaOutro = new CategoriaEntity();
            categoriaOutro.setNome(CATEGORIA_PADRAO);
            categoriaOutro.setTipo(CategoriaType.DESPESAS);
            categoriaOutro.setUser(outroUsuario);
            categoriaRepository.saveAndFlush(categoriaOutro);
    
            // Criar orçamento para o outro usuário
            orcamentoOutroUsuario = new OrcamentoMensalEntity();
            orcamentoOutroUsuario.setCategoria(categoriaOutro);
            orcamentoOutroUsuario.setValorLimite(VALOR_LIMITE_PADRAO);
            orcamentoOutroUsuario.setPeriodo(PERIODO_PADRAO);
            orcamentoOutroUsuario.setUser(outroUsuario);
            orcamentoMensalRepository.saveAndFlush(orcamentoOutroUsuario);
    
            // Criar orçamento para o usuário principal
            orcamentoUsuarioPrincipal = criarOrcamentoNoBanco();
    
            // Obter token para o outro usuário
            authHeaderOutroUsuario = "Bearer " + obterTokenJwt(outroUsuario.getEmail(), "654321");
        }
    
        @Test
        void usuarioPrincipalNaoDeveAcessarOrcamentoDeOutroUsuario() throws Exception {
            mockMvc.perform(get("/orcamento-mensal/" + orcamentoOutroUsuario.getUuid())
                    .header("Authorization", authHeader))
                    .andExpect(status().isNotFound());
        }
    
        @Test
        void outroUsuarioNaoDeveAcessarOrcamentoDoUsuarioPrincipal() throws Exception {
            mockMvc.perform(get("/orcamento-mensal/" + orcamentoUsuarioPrincipal.getUuid())
                    .header("Authorization", authHeaderOutroUsuario))
                    .andExpect(status().isNotFound());
        }
    
        @Test
        void usuarioPrincipalNaoDeveAtualizarOrcamentoDeOutroUsuario() throws Exception {
            OrcamentoMensalRequest request = new OrcamentoMensalRequest(
                CATEGORIA_PADRAO, 
                VALOR_LIMITE_ATUALIZADO, 
                PERIODO_ATUALIZADO
            );
    
            mockMvc.perform(put("/orcamento-mensal/" + orcamentoOutroUsuario.getUuid())
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    
        @Test
        void outroUsuarioNaoDeveAtualizarOrcamentoDoUsuarioPrincipal() throws Exception {
            OrcamentoMensalRequest request = new OrcamentoMensalRequest(
                CATEGORIA_PADRAO, 
                VALOR_LIMITE_ATUALIZADO, 
                PERIODO_ATUALIZADO
            );
    
            mockMvc.perform(put("/orcamento-mensal/" + orcamentoUsuarioPrincipal.getUuid())
                    .header("Authorization", authHeaderOutroUsuario)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    
        @Test
        void usuarioPrincipalNaoDeveExcluirOrcamentoDeOutroUsuario() throws Exception {
            mockMvc.perform(delete("/orcamento-mensal/" + orcamentoOutroUsuario.getUuid())
                    .header("Authorization", authHeader))
                    .andExpect(status().isNotFound());
        }
    
        @Test
        void outroUsuarioNaoDeveExcluirOrcamentoDoUsuarioPrincipal() throws Exception {
            mockMvc.perform(delete("/orcamento-mensal/" + orcamentoUsuarioPrincipal.getUuid())
                    .header("Authorization", authHeaderOutroUsuario))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ValidacaoTest {
        @Test
        void erroAoCriarOrcamentoSemCategoria() throws Exception {
            OrcamentoMensalDTO dto = criarOrcamentoMensalDTO();
            dto.setCategoria(null);
            
            mockMvc.perform(post("/orcamento-mensal")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void erroAoCriarOrcamentoComPeriodoInvalido() throws Exception {
            OrcamentoMensalDTO dto = criarOrcamentoMensalDTO();
            dto.setPeriodo(null);
            
            mockMvc.perform(post("/orcamento-mensal")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void erroAoAtualizarOrcamentoComDadosInvalidos() throws Exception {
            OrcamentoMensalEntity orcamento = criarOrcamentoNoBanco();
            OrcamentoMensalRequest request = new OrcamentoMensalRequest(
                "", // Categoria vazia
                null, // Valor limite nulo
                null // Período nulo
            );

            mockMvc.perform(put("/orcamento-mensal/" + orcamento.getUuid())
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class EdgeCasesTest {
        @Test
        void deveLidarComValoresMuitoAltos() throws Exception {
            BigDecimal valorMuitoAlto = new BigDecimal("999999999999999.99");
            OrcamentoMensalDTO dto = criarOrcamentoMensalDTO();
            dto.setValorLimite(valorMuitoAlto);
            
            mockMvc.perform(post("/orcamento-mensal")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.valorLimite").value(valorMuitoAlto.doubleValue()));
        }
    
        @Test
        void deveLidarComPeriodosExtremos() throws Exception {
            YearMonth periodoFuturoExtremo = YearMonth.now().plusYears(10);
            YearMonth periodoPassadoExtremo = YearMonth.of(1900, 1);
    
            // Teste com período futuro extremo
            OrcamentoMensalDTO dtoFuturo = criarOrcamentoMensalDTO();
            dtoFuturo.setPeriodo(periodoFuturoExtremo);
            mockMvc.perform(post("/orcamento-mensal")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dtoFuturo)))
                    .andExpect(status().isCreated());
    
            // Teste com período passado extremo
            OrcamentoMensalDTO dtoPassado = criarOrcamentoMensalDTO();
            dtoPassado.setPeriodo(periodoPassadoExtremo);
            mockMvc.perform(post("/orcamento-mensal")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dtoPassado)))
                    .andExpect(status().isCreated());
        }
    
        @Test
        void deveLidarComCategoriasEspeciais() throws Exception {
            String categoriaEspecial = "Orçamento @#$% 123";
            criarCategoriaTest(categoriaEspecial, user);
            
            OrcamentoMensalDTO dto = criarOrcamentoMensalDTO();
            dto.setCategoria(categoriaEspecial);
            
            mockMvc.perform(post("/orcamento-mensal")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.categoria").value(categoriaEspecial));
        }
    }

    private UserEntity criarUsuarioTest() {
        UserEntity newUser = new UserEntity();
        newUser.setUsername("TestUser");
        newUser.setEmail("test@email.com");
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

    private OrcamentoMensalEntity criarOrcamentoNoBanco() {
        return criarOrcamentoNoBanco(CATEGORIA_PADRAO, VALOR_LIMITE_PADRAO, PERIODO_PADRAO);
    }
    
    private OrcamentoMensalEntity criarOrcamentoNoBanco(String categoriaNome, BigDecimal valorLimite, YearMonth periodo) {
        CategoriaEntity categoria = categoriaRepository.findByNomeAndTipoAndUserUuid(
                categoriaNome, CategoriaType.DESPESAS, user.getUuid())
            .orElseGet(() -> {
                CategoriaEntity novaCategoria = new CategoriaEntity();
                novaCategoria.setNome(categoriaNome);
                novaCategoria.setTipo(CategoriaType.DESPESAS);
                novaCategoria.setUser(user);
                return categoriaRepository.save(novaCategoria);
            });
    
        OrcamentoMensalEntity orcamento = new OrcamentoMensalEntity();
        orcamento.setValorLimite(valorLimite);
        orcamento.setPeriodo(periodo);
        orcamento.setCategoria(categoria);
        orcamento.setUser(user);
        
        return orcamentoMensalRepository.save(orcamento);
    }

    private OrcamentoMensalDTO criarOrcamentoMensalDTO() {
        OrcamentoMensalDTO dto = new OrcamentoMensalDTO();
        dto.setCategoria(CATEGORIA_PADRAO);
        dto.setValorLimite(VALOR_LIMITE_PADRAO);
        dto.setPeriodo(PERIODO_PADRAO);
        return dto;
    }
}