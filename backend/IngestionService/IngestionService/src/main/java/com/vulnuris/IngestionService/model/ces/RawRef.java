package com.vulnuris.IngestionService.model.ces;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawRef {

    private String file;
    private Long offset;
}
