package com.vulnuris.IngestionService.model.ces;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrelationKeys {

    private String user;
    private String host;
    private String srcIp;
    private String dstIp;
    private String object;
}
