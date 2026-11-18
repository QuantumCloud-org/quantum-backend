package com.alpha.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DictTypeUpdateRequest extends DictTypeCreateRequest {

    @NotNull
    private Long id;

    @NotNull
    private Long version;
}
