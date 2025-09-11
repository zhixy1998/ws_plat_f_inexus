package com.persee.workstation.uv_inexus.po.guide.measurementPo;

import com.persee.workstation.uv_inexus.dto.measurementDto.TMeasureTaskDto;
import lombok.Data;

import java.util.List;

@Data
public class UnFinishTasksAndNum {
    List<TMeasureTaskDto> tasks;

}
