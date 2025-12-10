package com.persee.workstation.uv_inexus.dto.factory_inspection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemplateUpdateRequest {
    private String templateCode;
    private List<String> selectedFields;
    // getters and setters
}
