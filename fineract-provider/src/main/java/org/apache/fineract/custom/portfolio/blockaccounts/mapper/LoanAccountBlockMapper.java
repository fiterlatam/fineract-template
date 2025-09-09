package org.apache.fineract.custom.portfolio.blockaccounts.mapper;

import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface LoanAccountBlockMapper {

    LoanAccountBlockMapper INSTANCE = Mappers.getMapper(LoanAccountBlockMapper.class);

    @Mapping(expression = "java(entity.getLoan() != null ? entity.getLoan().getId() : null)", target = "loanId")
    @Mapping(source = "blockingReasonSetting.id", target = "blockingReasonId")
    @Mapping(source = "blockingReasonSetting.nameOfReason", target = "blockingReasonName")
    LoanAccountBlockDTO toDto(LoanAccountBlock entity);

    LoanAccountBlock toEntity(LoanAccountBlockDTO dto);
}
