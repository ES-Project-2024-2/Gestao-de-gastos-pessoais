package br.com.gestorfinanceiro.controllers.DashboardControllerTest;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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

import br.com.gestorfinanceiro.dto.user.LoginDTO;
import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.DespesaEntity;
import br.com.gestorfinanceiro.models.ReceitaEntity;
import br.com.gestorfinanceiro.models.UserEntity;
import br.com.gestorfinanceiro.models.enums.CategoriaType;
import br.com.gestorfinanceiro.models.enums.Roles;
import br.com.gestorfinanceiro.repositories.CategoriaRepository;
import br.com.gestorfinanceiro.repositories.DespesaRepository;
import br.com.gestorfinanceiro.repositories.ReceitaRepository;
import br.com.gestorfinanceiro.repositories.UserRepository;
import jakarta.transaction.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerIntegrationTest {

    private static final String CATEGORIA_DESPESA = "Alimentação";
    private static final String CATEGORIA_RECEITA = "Salário";
    private static final BigDecimal VALOR_DESPESA = BigDecimal.valueOf(500);
    private static final BigDecimal VALOR_RECEITA = BigDecimal.valueOf(3000);
    private static final YearMonth PERIODO_TESTE = YearMonth.now();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DespesaRepository despesaRepository;

    @Autowired
    private ReceitaRepository receitaRepository;

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
        criarCategoriasTest();
        criarDadosTeste();
        authHeader = "Bearer " + obterTokenJwt(user.getEmail(), "123456");
    }

    private void limparBaseDeDados() {
        despesaRepository.deleteAllInBatch();
        receitaRepository.deleteAllInBatch();
        categoriaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
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

    private void criarCategoriasTest() {
        // Categoria para despesas
        CategoriaEntity categoriaDespesa = new CategoriaEntity();
        categoriaDespesa.setNome(CATEGORIA_DESPESA);
        categoriaDespesa.setTipo(CategoriaType.DESPESAS);
        categoriaDespesa.setUser(user);
        categoriaRepository.save(categoriaDespesa);

        // Categoria para receitas
        CategoriaEntity categoriaReceita = new CategoriaEntity();
        categoriaReceita.setNome(CATEGORIA_RECEITA);
        categoriaReceita.setTipo(CategoriaType.RECEITAS);
        categoriaReceita.setUser(user);
        categoriaRepository.save(categoriaReceita);
    }

    private void criarDadosTeste() {
        // Criar despesas (já corrigido anteriormente)
        CategoriaEntity categoriaDespesa = categoriaRepository.findByNomeAndTipoAndUserUuid(
            CATEGORIA_DESPESA, CategoriaType.DESPESAS, user.getUuid()).get();
        
        DespesaEntity despesa1 = new DespesaEntity();
        despesa1.setValor(VALOR_DESPESA);
        despesa1.setData(PERIODO_TESTE.atDay(1));
        despesa1.setCategoria(categoriaDespesa);
        despesa1.setDestinoPagamento("Supermercado"); // Campo obrigatório
        despesa1.setObservacoes("Compras do mês");    // Campo obrigatório
        despesa1.setUser(user);
        despesaRepository.save(despesa1);
    
        // Criar receitas (correção dos novos campos obrigatórios)
        CategoriaEntity categoriaReceita = categoriaRepository.findByNomeAndTipoAndUserUuid(
            CATEGORIA_RECEITA, CategoriaType.RECEITAS, user.getUuid()).get();
        
        ReceitaEntity receita1 = new ReceitaEntity();
        receita1.setValor(VALOR_RECEITA);
        receita1.setData(PERIODO_TESTE.atDay(1));
        receita1.setCategoria(categoriaReceita);
        receita1.setOrigemDoPagamento("Salário");     // Novo campo obrigatório
        receita1.setObservacoes("Pagamento mensal");  // Campo obrigatório
        receita1.setUser(user);
        receitaRepository.save(receita1);
    }

    @Test
    void deveCarregarContextoAplicacao() {
        assertNotNull(mockMvc, "O MockMvc não deveria ser nulo!");
    }

    @Nested
    class SaldoTotalTest {
        @Test
        void deveRetornarSaldoTotal() throws Exception {
            BigDecimal saldoEsperado = VALOR_RECEITA.subtract(VALOR_DESPESA);
            
            mockMvc.perform(get("/dashboard/saldo-total")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodo").value(PERIODO_TESTE.toString()))
                    .andExpect(jsonPath("$.saldo").value(saldoEsperado.doubleValue()));
        }

        @Test
        void erroQuandoPeriodoInvalido() throws Exception {
            mockMvc.perform(get("/dashboard/saldo-total")
                    .param("periodo", "periodo-invalido")
                    .header("Authorization", authHeader))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class MaiorDespesaTest {
        @Test
        void deveRetornarMaiorDespesa() throws Exception {
            mockMvc.perform(get("/dashboard/maior-despesa")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valor").value(VALOR_DESPESA.doubleValue()))
                    .andExpect(jsonPath("$.categoria").value(CATEGORIA_DESPESA));
        }
    
        @Test
        void deveRetornarNotFoundQuandoNaoHaDespesas() throws Exception {
            // Limpa de forma segura mantendo as receitas
            despesaRepository.deleteAllInBatch();
            
            mockMvc.perform(get("/dashboard/maior-despesa")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeader))
                    .andExpect(status().isNotFound());
        }
    }
    
    @Nested
    class MaiorReceitaTest {
        @Test
        void deveRetornarMaiorReceita() throws Exception {
            mockMvc.perform(get("/dashboard/maior-receita")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valor").value(VALOR_RECEITA.doubleValue()))
                    .andExpect(jsonPath("$.categoria").value(CATEGORIA_RECEITA));
        }
    
        @Test
        void deveRetornarNotFoundQuandoNaoHaReceitas() throws Exception {
            // Limpa de forma segura mantendo as despesas
            receitaRepository.deleteAllInBatch();
            
            mockMvc.perform(get("/dashboard/maior-receita")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeader))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class CategoriaMaiorDespesaTest {
        @Test
        void deveRetornarCategoriaComMaiorDespesa() throws Exception {
            mockMvc.perform(get("/dashboard/categoria-maior-despesa")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$." + CATEGORIA_DESPESA).value(VALOR_DESPESA.doubleValue()));
        }
    }

    @Nested
    class CategoriaMaiorReceitaTest {
        @Test
        void deveRetornarCategoriaComMaiorReceita() throws Exception {
            mockMvc.perform(get("/dashboard/categoria-maior-receita")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$." + CATEGORIA_RECEITA).value(VALOR_RECEITA.doubleValue()));
        }
    }

    @Nested
    class TotalDespesasTest {
        @Test
        void deveRetornarTotalDespesas() throws Exception {
            mockMvc.perform(get("/dashboard/despesa-total")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodo").value(PERIODO_TESTE.toString()))
                    .andExpect(jsonPath("$.saldo").value(VALOR_DESPESA.doubleValue()));
        }
    }

    @Nested
    class TotalReceitasTest {
        @Test
        void deveRetornarTotalReceitas() throws Exception {
            mockMvc.perform(get("/dashboard/receita-total")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodo").value(PERIODO_TESTE.toString()))
                    .andExpect(jsonPath("$.saldo").value(VALOR_RECEITA.doubleValue()));
        }
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

        @Test
        void outroUsuarioNaoDeveAcessarDadosDoUsuarioPrincipal() throws Exception {
            mockMvc.perform(get("/dashboard/saldo-total")
                    .param("periodo", PERIODO_TESTE.toString())
                    .header("Authorization", authHeaderOutroUsuario))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.saldo").value(0.0));
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
}