package com.ilhanozkan.libraryManagementSystem.model.mapper;

import com.ilhanozkan.libraryManagementSystem.model.dto.response.FineResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.Fine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FineResponseDTOMapper {
  FineResponseDTOMapper INSTANCE = Mappers.getMapper(FineResponseDTOMapper.class);

  @Mapping(target = "borrowingId", expression = "java(fine.getBorrowing() != null ? fine.getBorrowing().getId() : null)")
  FineResponseDTO toFineResponseDTO(Fine fine);

  List<FineResponseDTO> toFineResponseDTOList(List<Fine> fines);
}
