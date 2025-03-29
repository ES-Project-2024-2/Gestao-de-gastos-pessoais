package br.com.gestorfinanceiro.mappers.impl;

import br.com.gestorfinanceiro.dto.OrcamentoMensal.OrcamentoMensalDTO;
import br.com.gestorfinanceiro.mappers.Mapper;
import br.com.gestorfinanceiro.models.CategoriaEntity;
import br.com.gestorfinanceiro.models.OrcamentoMensalEntity;
import br.com.gestorfinanceiro.repositories.CategoriaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class OrcamentoMensalMapperImpl implements Mapper<OrcamentoMensalEntity, OrcamentoMensalDTO> {

    private final ModelMapper modelMapper;
    private final CategoriaRepository categoriaRepository; // Adicionando o repositório

    public OrcamentoMensalMapperImpl(ModelMapper modelMapper, CategoriaRepository categoriaRepository) {
        this.modelMapper = modelMapper;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public OrcamentoMensalDTO mapTo(OrcamentoMensalEntity orcamentoMensalEntity) {
        OrcamentoMensalDTO dto = modelMapper.map(orcamentoMensalEntity, OrcamentoMensalDTO.class);
        dto.setCategoria(orcamentoMensalEntity.getCategoria().getNome());
        return dto;
    }

    @Override
    public OrcamentoMensalEntity mapFrom(OrcamentoMensalDTO orcamentoMensalDTO) {
        OrcamentoMensalEntity entity = modelMapper.map(orcamentoMensalDTO, OrcamentoMensalEntity.class);

        CategoriaEntity categoria = categoriaRepository.findByNome(orcamentoMensalDTO.getCategoria())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + orcamentoMensalDTO.getCategoria()));

        entity.setCategoria(categoria);
        return entity;
    }
}
