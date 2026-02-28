package com.vulnuris.IngestionService.model.ces;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Iocs {

    private List<String> domains;
    private List<String> ips;
    private List<String> urls;
    private List<String> hashes;
}
