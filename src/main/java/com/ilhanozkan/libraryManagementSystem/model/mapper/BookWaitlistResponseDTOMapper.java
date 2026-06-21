package com.ilhanozkan.libraryManagementSystem.model.mapper;

import com.ilhanozkan.libraryManagementSystem.model.dto.response.BookWaitlistResponseDTO;
import com.ilhanozkan.libraryManagementSystem.model.entity.BookWaitlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookWaitlistResponseDTOMapper {
  BookWaitlistResponseDTOMapper INSTANCE = Mappers.getMapper(BookWaitlistResponseDTOMapper.class);

  @Mapping(target = "bookId", expression = "java(w.getBook() != null ? w.getBook().getId() : null)")
  @Mapping(target = "bookName", expression = "java(w.getBook() != null ? w.getBook().getName() : null)")
  @Mapping(target = "isbn", expression = "java(w.getBook() != null ? w.getBook().getIsbn() : null)")
  @Mapping(target = "userName", expression = "java(w.getUser() != null ? w.getUser().getUsername() : null)")
  BookWaitlistResponseDTO toResponseDTO(BookWaitlist w);

  List<BookWaitlistResponseDTO> toResponseDTOList(List<BookWaitlist> waitlist);
}
