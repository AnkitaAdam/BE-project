package com.vulnuris.IngestionService.model.ces;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CesEvent {

    @NotNull
    private Instant tsUtc;          // @ts_utc
    private String tsOriginal;      // @ts_original
    private String tzOffset;        // +05:30 or minutes

    private SourceType sourceType;

    private String host;
    private String user;

    private String srcIp;
    private Integer srcPort;

    private String dstIp;
    private Integer dstPort;

    private String protocol;

    private String action;
    private String object;

    private ResultType result;

    @Min(0)
    @Max(10)
    private Integer severity;

    private String message;

    private Iocs iocs;
    private CorrelationKeys correlationKeys;

    private RawRef rawRef;
}
