package com.persee.workstation.uv_inexus.dto.factory_inspection;

import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemplateCreationRequest extends BasePO {
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 32, message = "模板名称不能超过32个字符")
    private String templateName;
    @NotNull(message = "选中行不能为空")
    @ApiModelProperty("选中行,其中key是每一项的id")
    private Map<String, List<Map<String, Object>>> selectedRows;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("类型，新增为0 修改为 1")
    private String type;
    @ApiModelProperty("模板id(修改模板的时候传)")
    private String id;
    @ApiModelProperty("任务id")
    private String taskId;
}
