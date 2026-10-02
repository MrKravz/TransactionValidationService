package by.ares.transaction_validation_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TwelveDataResponseDto(
        @JsonProperty("meta")
        MetaDto meta,

        @JsonProperty("values")
        List<ValueDto> values,

        @JsonProperty("status")
        String status
) {
    public record MetaDto(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("interval") String interval
    ) {}

    public record ValueDto(
            @JsonProperty("datetime") String datetime,
            @JsonProperty("open") String open,
            @JsonProperty("high") String high,
            @JsonProperty("low") String low,
            @JsonProperty("close") String close
    ) {}
}
