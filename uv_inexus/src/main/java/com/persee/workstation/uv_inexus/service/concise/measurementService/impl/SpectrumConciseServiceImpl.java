package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.dto.concise.common.FAddGaugePointDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FPeakValleyMarkingDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FSeekingPeaksValleysDto;
import com.persee.workstation.uv_inexus.dto.concise.common.FUpdateDataColorDto;
import com.persee.workstation.uv_inexus.dto.concise.spectrum.FUseMethodDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureAnalysisData;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSample;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FSpectrumParameter;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.concise.common.FSampleListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 简洁模式: 光谱扫描服务业务层实现
 */

@Service("SpectrumConciseService")
public class SpectrumConciseServiceImpl implements SpectrumConciseService {

    @Autowired
    private FMeasureMethodService fMeasureMethodService;

    @Autowired
    private FMeasureTaskService fMeasureTaskService;

    @Autowired
    private FMeasureSampleService fMeasureSampleService;

    @Autowired
    private FMeasureAnalysisDataService fMeasureAnalysisDataService;


    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DevMessageUtil devMessageUtil;


    @Autowired
    private RestTemplate restTemplate;

    /**
     * 查询最近方法数据
     *
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result queryNearestMethodData(String updateBy, String measureType) {
        if (updateBy == null) {
            return Result.fail("更新人不能为空");
        }

        if (measureType == null) {
            return Result.fail("方法类型不能为空");
        }

        QueryWrapper<FMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator", updateBy);
        wrapper.eq("is_draft", "0");
        wrapper.eq("measurement_type", measureType);
        wrapper.orderByDesc("method_use_time");
        wrapper.last("LIMIT 1");

        FMeasureMethod method = fMeasureMethodService.getById(wrapper);

        if (method == null) {
            // todo 查询对应类型的默认方法

        }

        // 根据方法id查询出最近创建的任务
        QueryWrapper<FMeasureTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("method_id", method.getId());
        taskWrapper.orderByDesc("create_time");
        taskWrapper.last("LIMIT 1");
        FMeasureTask task = fMeasureTaskService.getOne(taskWrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("method", method);
        if (task != null) {
            map.put("taskId", task.getId());
        }
        return Result.ok(map);
    }

    /**
     * 查找方法按钮
     *
     * @param updateBy
     * @param measureType
     * @param methodName
     * @return
     */
    @Override
    public Result searchMethodButton(Integer pageNum, Integer pageSize, String updateBy, String measureType, String methodName) {
        // 对传入的参数进行校验
        if (updateBy == null) {
            return Result.fail("用户不能为空");
        }
        if (measureType == null) {
            return Result.fail("方法类型不能为空");
        }
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        // 设置查询条件
        QueryWrapper<TMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator", updateBy);
        wrapper.eq("measurement_type", measureType);
        if (methodName != null) {
            wrapper.like("method_name", methodName);
        }
        // 分页查询
        Page<TMeasureMethod> page = tMeasureMethodService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(page);
    }

    /**
     * 应用接口
     * @param paramDto
     * @return
     */
    @Override
    public Result use(FUseMethodDto paramDto) {
        return Result.ok();
    }

    /**
     * 查询样品数据列表
     *
     * @param taskId
     * @param type
     * @param digit
     * @return
     */
    @Override
    public Result querySampleDataList(String taskId, String type, Integer digit) {
        if (taskId == null) {
            return Result.fail("方法id不能为空");
        }

        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("该方法下没有草稿,请新建草稿");
        }
        // 根据方法id查询方法
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            return Result.fail("方法不存在");
        }
        FSpectrumParameter parameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        List<FTaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        List<FSampleListVo> listVos = new ArrayList<>();
        // 判断是否是重复模式
        if ("不重复".equals(parameter.getRepetitionPattern())) {
            // 不重复模式
            for (FTaskSpecimen taskSpeciman : taskSpecimen) {
                FSampleListVo vo = new FSampleListVo();
                // 根据样品id查询样品数据
                FMeasureSample sample = fMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    return Result.fail("样品不存在");
                }
                // 如果没有测量数据则设置为无测量数据的参数返回
                if (taskSpeciman.getIsTested().equals("0")) {
                    vo.setTitle(sample.getSampleName());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    vo.setAnalysisId(null);
                    vo.setAnalysisNumber(1);
                    vo.setColor(null);
                    vo.setData(null);
                } else {
                    QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", task.getId());
                    queryWrapper.eq("sample_key", taskSpeciman.getSpecimenKey());
                    queryWrapper.eq("analysis_type", "5");
                    queryWrapper.orderByDesc("analysis_number");
                    if (type == null) {
                        queryWrapper.eq("is_delete", "0");
                    }
                    queryWrapper.last("limit 1");
                    FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getOne(queryWrapper);
                    if (measureData == null) {
                        return Result.fail("无测量数据");
                    }
                    vo.setTitle(sample.getSampleName());
                    vo.setAnalysisId(measureData.getId());
                    vo.setSampleId(taskSpeciman.getSpecimenKey());
                    vo.setAnalysisNumber(measureData.getAnalysisNumber());
                    vo.setColor(measureData.getColor());
                    if (measureData.getAnalysisResult() == null) {
                        vo.setData(null);
                    } else {
                        vo.setData(getSampleData(measureData, digit));
                    }
                }
                vo.setType("0");
                vo.setDeleteType(taskSpeciman.getDeleteType());
                listVos.add(vo);
            }
        } else {
            // 重复模式
            if (parameter.getRepetitionTime() == null || parameter.getRepetitionTime().equals("")) {
                return Result.fail("请填写重复次数");
            }
            Integer count = Integer.valueOf(parameter.getRepetitionTime());
            for (FTaskSpecimen taskSpeciman : taskSpecimen) {
                if (type == null && taskSpeciman.getDeleteType().equals("1")) {
                    continue;
                }
                // 根据样品id查询样品数据
                FMeasureSample sample = fMeasureSampleService.getById(taskSpeciman.getSpecimenKey());
                if (sample == null) {
                    return Result.fail("样品不存在");
                }
                int key = 0;
                // 根据方法中的方法参数的重复次数进行循环
                for (int i = 0; i < count; i++) {
                    FSampleListVo vo = new FSampleListVo();
                    QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("task_id", task.getId());
                    queryWrapper.eq("sample_key", taskSpeciman.getSpecimenKey());
                    queryWrapper.eq("analysis_type", "5");
                    if (type == null) {
                        queryWrapper.eq("is_delete", "0");
                    }
                    queryWrapper.eq("analysis_number", (i + 1));
                    queryWrapper.last("limit 1");
                    FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getOne(queryWrapper);
                    if (measureData == null && taskSpeciman.getIsTested().equals("0")) {
                        vo.setTitle(sample.getSampleName() + "-" + (i + 1));
                        vo.setAnalysisId(null);
                        vo.setSampleId(taskSpeciman.getSpecimenKey());
                        vo.setAnalysisNumber(i + 1);
                        vo.setColor(null);
                        vo.setData(null);
                        vo.setDeleteType("0");
                    } else {
                        vo.setTitle(sample.getSampleName() + "-" + (i + 1));
                        vo.setAnalysisId(measureData.getId());
                        vo.setSampleId(taskSpeciman.getSpecimenKey());
                        vo.setAnalysisNumber(measureData.getAnalysisNumber());
                        vo.setColor(measureData.getColor());
                        if (measureData.getAnalysisResult() == null) {
                            vo.setData(null);
                        } else {
                            vo.setData(getSampleData(measureData, digit));
                        }
                        vo.setDeleteType(measureData.getIsDelete());
                    }
                    vo.setType("0");

                    listVos.add(vo);
                }
            }
        }
        return Result.ok(listVos);
    }

    /**
     * 删除样品列表
     *
     * @param taskId
     * @param sampleId
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteSampleListPort(String taskId, String sampleId, String analysisId) {
        // 校验传过来的参数
        if (taskId == null || sampleId == null || analysisId == null) {
            return Result.fail("参数错误");
        }
        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        List<FTaskSpecimen> taskSpecimenList = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        // 根据任务id查询方法表数据并取出方法参数
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        FSpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), FSpectrumParameter.class);
        // 判断是否是重复模式
        if (spectrumParameter.getRepetitionPattern().equals("不重复")) {
            // 根据分析id查询分析数据
            FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                return Result.fail("分析数据不存在");
            }
            byId.setIsDelete("1");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
            // 循环遍历样品列表找到该样品
            for (FTaskSpecimen taskSpecimen : taskSpecimenList) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setDeleteType("1");
                }
            }
            task.setSampleKey(JSON.toJSONString(taskSpecimenList));
            boolean update1 = fMeasureTaskService.updateById(task);
            if (update && update1) {
                return Result.ok("成功删除数据");
            } else {
                return Result.fail("删除数据失败");
            }
        } else {
            // 根据分析id查询分析数据
            FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                return Result.fail("分析数据不存在");
            }
            byId.setIsDelete("1");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
            // 查询跟这个样品和任务id关联的分析数据
            List<FMeasureAnalysisData> list = fMeasureAnalysisDataService.list(new QueryWrapper<FMeasureAnalysisData>().eq("task_id", taskId).eq("sample_key", sampleId).eq("is_delete", "0"));
            if (list.size() == 0) {
                // 循环遍历样品列表找到该样品
                for (FTaskSpecimen taskSpecimen : taskSpecimenList) {
                    if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                        taskSpecimen.setDeleteType("1");
                        break;
                    }
                }
                task.setSampleKey(JSON.toJSONString(taskSpecimenList));
                boolean update1 = fMeasureTaskService.updateById(task);
                if (update && update1) {
                    return Result.ok("成功删除数据");
                } else {
                    return Result.fail("删除数据失败");
                }
            }
            if (update) {
                return Result.ok("成功删除数据");
            } else {
                return Result.fail("删除数据失败");
            }
        }
    }

    /**
     * 查询分析数据列表
     *
     * @param taskId
     * @param type
     * @param digit
     * @return
     */
    @Override
    public Result queryAnalysisDataList(String taskId, String type, Integer digit) {
        if (taskId == null) {
            return Result.fail("方法id不能为空");
        }


        FMeasureTask task = fMeasureTaskService.getById(taskId);
        if (task == null) {
            return Result.fail("该方法下没有草稿,请新建草稿");
        }
        // 根据方法id查询方法
        FMeasureMethod method = fMeasureMethodService.getById(task.getMethodKey());
        if (method == null) {
            return Result.fail("方法不存在");
        }

        // 根据任务id去查询分析数据
        QueryWrapper<FMeasureAnalysisData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", task.getId());
        queryWrapper.ne("analysis_type", "5");
        if (type == null || type.isEmpty()) {
            queryWrapper.eq("is_delete", "0");
        }
        List<FMeasureAnalysisData> list = fMeasureAnalysisDataService.list(queryWrapper);
        List<FSampleListVo> listVos = new ArrayList<>();
        for (FMeasureAnalysisData fMeasureAnalysisData : list) {
            FSampleListVo vo = new FSampleListVo();
            vo.setAnalysisId(fMeasureAnalysisData.getId());
            vo.setTitle(fMeasureAnalysisData.getAnalysisName());
            vo.setColor(fMeasureAnalysisData.getColor());
            vo.setDeleteType(fMeasureAnalysisData.getIsDelete());
            List<String> results = JSON.parseArray(fMeasureAnalysisData.getAnalysisResult(), String.class);
            List<FPeakValleyMarking> gauges = JSON.parseArray(fMeasureAnalysisData.getGaugePoint(), FPeakValleyMarking.class);
            List<FPeakValleyMarking> peaks = JSON.parseArray(fMeasureAnalysisData.getPeak(), FPeakValleyMarking.class);
            List<FPeakValleyMarking> grades = JSON.parseArray(fMeasureAnalysisData.getGrain(), FPeakValleyMarking.class);
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (String s : results) {
                String[] split = s.split(",");
                Map<String, Object> map = new HashMap<>();
                if (digit != null) {
                    BigDecimal value = new BigDecimal(split[1]);
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    map.put("x", split[0]);
                    map.put("y", value.stripTrailingZeros().toPlainString());
                } else {
                    map.put("x", split[0]);
                    map.put("y", split[1]);
                }
                if (gauges != null) {
                    for (FPeakValleyMarking gauge : gauges) {
                        if (split[0].equals(gauge.getX())) {
                            map.put("marked", "1");
                            break;
                        } else {
                            map.put("marked", "0");
                        }
                    }
                } else {
                    map.put("marked", "0");
                }
                if (grades != null) {
                    for (FPeakValleyMarking peak : peaks) {
                        if (split[0].equals(peak.getX())) {
                            map.put("feng", "1");
                            break;
                        } else {
                            map.put("feng", "0");
                        }
                    }
                } else {
                    map.put("feng", "0");
                }
                if (grades != null) {
                    for (FPeakValleyMarking grade : grades) {
                        if (split[0].equals(grade.getX())) {
                            map.put("gu", "1");
                            break;
                        } else {
                            map.put("gu", "0");
                        }
                    }
                } else {
                    map.put("gu", "0");
                }
                resultList.add(map);
            }
            vo.setData(resultList);
            vo.setType("1");
            listVos.add(vo);
        }
        return Result.ok(listVos);
    }

    /**
     * 删除分析数据列表接口
     *
     * @param analysisId
     * @return
     */
    @Override
    public Result deleteChangeListPort(String analysisId) {
        // 根据analysisId查询测量数据
        FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
        if (byId == null) {
            return Result.fail("分析数据不存在");
        }
        // 删除测量数据
        byId.setIsDelete("1");
        boolean update = fMeasureAnalysisDataService.updateById(byId);
        if (update) {
            return Result.ok(true);
        } else {
            return Result.fail(false);
        }
    }

    /**
     * 恢复删除的数据
     *
     * @param taskId
     * @param sampleId
     * @param type
     * @param analysisId
     * @return
     **/
    @Override
    public Result restoreDeletedListData(String taskId, String sampleId, String type, String analysisId) {
        // 根据类型判断是样品数据还是变换数据
        if (type.equals("0")) {
            // 根据任务id查询任务数据
            FMeasureTask task = fMeasureTaskService.getById(taskId);
            if (task == null) {
                return Result.fail("任务不存在");
            }
            List<FTaskSpecimen> sampleList = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
            for (FTaskSpecimen taskSpecimen : sampleList) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setDeleteType("0");
                    break;
                }
            }
            task.setSampleKey(JSON.toJSONString(sampleList));
            boolean update1 = fMeasureTaskService.updateById(task);
            // 根据分析id查询分析数据
            FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                return Result.fail("分析数据不存在");
            }
            byId.setIsDelete("0");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
            if (update1 && update) {
                return Result.ok(true);
            } else {
                return Result.fail(false);
            }
        } else {
            // 根据变换数据id查询变换数据
            FMeasureAnalysisData byId = fMeasureAnalysisDataService.getById(analysisId);
            if (byId == null) {
                return Result.fail("变换数据不存在");
            }
            byId.setIsDelete("0");
            boolean update = fMeasureAnalysisDataService.updateById(byId);
            if (update) {
                return Result.ok(update);
            } else {
                return Result.fail(false);
            }
        }
    }

    /**
     * 更新数据颜色接口
     *
     * @param paramDto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateDataColor(FUpdateDataColorDto paramDto) {
        // 参数校验
        if (paramDto == null || paramDto.getDataId() == null || paramDto.getDataId().trim().isEmpty()) {
            return Result.fail("参数不能为空");
        }

        String dataId = paramDto.getDataId();
        String color = paramDto.getColor();

        boolean updated = false;


        // 分析数据
        FMeasureAnalysisData analysis = fMeasureAnalysisDataService.getById(dataId);
        if (analysis == null) {
            return Result.fail("样品数据不存在");
        }
        analysis.setColor(color);
        updated = fMeasureAnalysisDataService.updateById(analysis);


        if (!updated) {
            return Result.fail("更新失败");
        }

        return Result.ok();
    }

    /**
     * 获取分析标记点接口
     *
     * @param dataId
     * @param digit
     * @return
     */
    @Override
    public Result queryGaugePoint(String dataId, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<FPeakValleyMarking> gaugePoints = new ArrayList<>();
        FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getById(dataId);
        if (measureData == null) {
            return Result.ok(new ArrayList<>());
        }
        if (measureData.getGaugePoint() != null) {
            gaugePoints = JSON.parseArray(measureData.getGaugePoint(), FPeakValleyMarking.class);
            if (digit != null) {
                for (FPeakValleyMarking data : gaugePoints) {
                    BigDecimal value = new BigDecimal(data.getY());
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    data.setY(value.stripTrailingZeros().toPlainString());
                }
            }
        }
        return Result.ok(gaugePoints);
    }

    /**
     * 添加标记点接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePointPort(FAddGaugePointDto paramDto) {
        if (paramDto.getGaugePoints() == null || paramDto.getGaugePoints().isEmpty()) {
            return Result.fail("请选择标记点");
        }
        // 读取原数据
        String gaugePointJson;
        List<String> rawData;
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData == null) {
            return Result.fail("分析数据不存在");
        }
        gaugePointJson = analysisData.getGaugePoint();
        rawData = JSON.parseArray(analysisData.getAnalysisResult(), String.class);


        // 原标记点集合
        List<FPeakValleyMarking> oldList = gaugePointJson == null
                ? new ArrayList<>()
                : JSON.parseArray(gaugePointJson, FPeakValleyMarking.class);

        // 先构造原有标记点 Map（X -> 对象）
        Map<String, FPeakValleyMarking> oldMap = oldList.stream()
                .collect(Collectors.toMap(
                        m -> normalizeNumberString(m.getX()),
                        m -> m,
                        (a, b) -> a
                ));

        // 构造新的最终集合（保证顺序）
        Map<String, FPeakValleyMarking> finalMap = new LinkedHashMap<>();

        // 遍历前端传的 X 值
        for (String xStr : paramDto.getGaugePoints()) {
            String normX = normalizeNumberString(xStr);
            FPeakValleyMarking existing = oldMap.get(normX);

            if (existing != null) {
                // 已存在则直接用旧值
                finalMap.put(normX, existing);
            } else {
                // 新增点时，从原始数据中查找对应 Y
                for (String s : rawData) {
                    String[] split = s.split(",");
                    String rawNormX = normalizeNumberString(split[0]);
                    if (normX.equals(rawNormX)) {
                        FPeakValleyMarking newMark = new FPeakValleyMarking();
                        newMark.setX(split[0]);
                        newMark.setY(split[1]);
                        finalMap.put(normX, newMark);
                        break;
                    }
                }
            }
        }

        // 保存更新
        String updatedJson = JSON.toJSONString(new ArrayList<>(finalMap.values()));
        FMeasureAnalysisData analysisData1 = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        analysisData1.setGaugePoint(updatedJson);
        boolean update = fMeasureAnalysisDataService.updateById(analysisData1);
        return update ? Result.ok(true) : Result.fail(false);
    }

    /**
     * 添加标记点备注接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result addGaugePointRemarks(FPeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            return Result.fail("参数不能为空");
        }
        if (paramDto.getRemark() == null) {
            return Result.fail("请先添加备注");
        }
        // 根据测量id去分析数据表中进行查询
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData == null) {
            return Result.fail("分析数据不存在");
        }
        List<FPeakValleyMarking> list = JSON.parseArray(tMeasureData.getGaugePoint(), FPeakValleyMarking.class);
        // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
        for (FPeakValleyMarking marking : list) {
            if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                marking.setRemark(paramDto.getRemark());
                break;
            }
        }
        tMeasureData.setGaugePoint(JSON.toJSONString(list));
        boolean update = fMeasureAnalysisDataService.updateById(tMeasureData);
        return update ? Result.ok() : Result.fail();
    }

    /**
     * 获取峰谷接口
     *
     * @param dataId
     * @param digit
     * @return
     */
    @Override
    public Result queryPeakAndValleysPort(String dataId, Integer digit) {
        if ((dataId == null || dataId.equals(""))) {
            return Result.ok(new ArrayList<>());
        }
        List<FPeakValleyMarking> peaks = new ArrayList<>();
        List<FPeakValleyMarking> grains = new ArrayList<>();
        List<FPeakValleyMarking> dataAll = new ArrayList<>();
        FMeasureAnalysisData measureData = fMeasureAnalysisDataService.getById(dataId);
        if (measureData == null) {
            return Result.ok(new ArrayList<>());
        }
        if (measureData.getPeak() != null) {
            peaks = JSON.parseArray(measureData.getPeak(), FPeakValleyMarking.class);
            dataAll.addAll(peaks);
        }
        if (measureData.getGrain() != null) {
            grains = JSON.parseArray(measureData.getGrain(), FPeakValleyMarking.class);
            dataAll.addAll(grains);
        }
        if (dataAll != null && dataAll.size() != 0) {
            if (digit != null) {
                for (FPeakValleyMarking data : dataAll) {
                    BigDecimal value = new BigDecimal(data.getY());
                    value = value.setScale(digit, RoundingMode.HALF_UP);
                    data.setY(value.stripTrailingZeros().toPlainString());
                }
            }
        }
        return Result.ok(dataAll);
    }

    /**
     * 寻峰寻谷接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result peakSearchingPort(FSeekingPeaksValleysDto paramDto) {
        paramDto.setHeight(paramDto.getHeight() / 100);
        FMeasureAnalysisData fMeasureAnalysisData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (fMeasureAnalysisData == null) {
            return Result.fail("测量数据不存在");
        }
        List<String> list = JSON.parseArray(fMeasureAnalysisData.getAnalysisResult(), String.class);
        // 分割测量数据
        List<String> xList = new ArrayList<>();
        List<String> yList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            xList.add(split[0]);
            yList.add(split[1]);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("x_data", xList);
        body.put("y_data", yList);
        body.put("peak_height", paramDto.getHeight());
        body.put("valley_height", paramDto.getHeight());
        body.put("distance", paramDto.getDistance());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING_AND_VALLEY_SEARCHING, request, Map.class).getBody();
        List<FPeakValleyMarking> peakData = new ArrayList<>();
        List<Double> xPeak = (List<Double>) resultMap.get("peak_x");
        List<String> yPeak = (List<String>) resultMap.get("peak_y");
        if (xPeak == null || yPeak == null) {
            return Result.fail("height参数太高");
        }
        for (int i = 0; i < xPeak.size(); i++) {
            FPeakValleyMarking marking = new FPeakValleyMarking();
            marking.setX(xPeak.get(i).toString());
            marking.setY(yPeak.get(i));
            marking.setType("波峰");
            peakData.add(marking);
        }
        List<FPeakValleyMarking> valleyData = new ArrayList<>();
        List<Double> xValley = (List<Double>) resultMap.get("valley_x");
        List<String> yValley = (List<String>) resultMap.get("valley_y");
        for (int i = 0; i < xValley.size(); i++) {
            FPeakValleyMarking marking = new FPeakValleyMarking();
            marking.setX(xValley.get(i).toString());
            marking.setY(yValley.get(i));
            marking.setType("波谷");
            valleyData.add(marking);
        }
        Boolean flag;
        FMeasureAnalysisData analysisData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (analysisData == null) {
            return Result.fail("分析数据不存在");
        }
        analysisData.setPeak(JSON.toJSONString(peakData));
        analysisData.setGrain(JSON.toJSONString(valleyData));
        flag = fMeasureAnalysisDataService.updateById(analysisData);
        return flag ? Result.ok() : Result.fail("数据保存失败");
    }

    /**
     * 添加峰谷备注接口
     *
     * @param paramDto
     * @return
     */
    @Override
    public Result addPeakValleyRemarks(FPeakValleyMarkingDto paramDto) {
        if (paramDto == null) {
            return Result.fail("参数不能为空");
        }
        if (paramDto.getRemark() == null) {
            return Result.fail("请先添加备注");
        }
        // 根据测量id去分析数据表中进行查询
        FMeasureAnalysisData tMeasureData = fMeasureAnalysisDataService.getById(paramDto.getDataId());
        if (tMeasureData == null) {
            return Result.fail("测量数据不存在");
        }
        // 判断添加的备注是峰该是谷,如果是峰则取出峰的数据,如果是谷则取出谷的数据
        List<FPeakValleyMarking> list;
        if (paramDto.getType().equals("波峰")) {
            list = JSON.parseArray(tMeasureData.getPeak(), FPeakValleyMarking.class);
        } else {
            list = JSON.parseArray(tMeasureData.getGrain(), FPeakValleyMarking.class);
        }
        // 根据传过来的x和y进行比较,如果比较成功的话就将备注存入数据库
        for (FPeakValleyMarking marking : list) {
            if (marking.getX().equals(paramDto.getX()) && marking.getY().equals(paramDto.getY())) {
                marking.setRemark(paramDto.getRemark());
                break;
            }
        }
        if (paramDto.getType().equals("波峰")) {
            tMeasureData.setPeak(JSON.toJSONString(list));
        } else {
            tMeasureData.setGrain(JSON.toJSONString(list));
        }
        boolean update = fMeasureAnalysisDataService.updateById(tMeasureData);
        return update ? Result.ok() : Result.fail();
    }

    /**
     * 设置测量参数接口
     *
     * @param methodId
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public Result setUpParametersPort(String methodId, String deviceSerialNumber) {
        // 根据序列号查询设备ip
        String ip = deviceInfoService.getIp(deviceSerialNumber);
        // 根据methodId去方法表中查询数据
        FMeasureMethod tMeasureMethod = fMeasureMethodService.getOne(new QueryWrapper<FMeasureMethod>().eq("id", methodId).eq("measurement_type", "光谱扫描"));
        if (tMeasureMethod == null) {
            return Result.fail("方法不存在");
        }

        FSpectrumParameter scanParamInfo = JSON.parseObject(tMeasureMethod.getMeasurementParamInfo(), FSpectrumParameter.class);

        List<Command> commands = new ArrayList<>();

        // 光度模式
        Command com1 = new Command();
        com1.setCmdId(SetParamEnum.SetPhotoMode.getComId());
        com1.setParams(new Object[]{scanParamInfo.getLuminosity()});
        commands.add(com1);
        // 开始波长
        Command com2 = new Command();
        com2.setCmdId(SetParamEnum.SetStartWl.getComId());
        com2.setParams(new Object[]{scanParamInfo.getStartWaveLength()});
        commands.add(com2);
        // 结束波长
        Command com3 = new Command();
        com3.setCmdId(SetParamEnum.SetEndWl.getComId());
        com3.setParams(new Object[]{scanParamInfo.getEndWaveLength()});
        commands.add(com3);
        // 波长间隔
        Command com4 = new Command();
        com4.setCmdId(SetParamEnum.SetScanWlInterval.getComId());
        com4.setParams(new Object[]{scanParamInfo.getWaveLengthInterval()});
        commands.add(com4);
        // 扫描速度
        Command com5 = new Command();
        com5.setCmdId(SetParamEnum.SetScanSpeed.getComId());
        com5.setParams(new Object[]{scanParamInfo.getScanSpeed()});
        commands.add(com5);
        // 换灯波长
        Command com6 = new Command();
        com6.setCmdId(SetParamEnum.SetChangeLampWl.getComId());
        com6.setParams(new Object[]{scanParamInfo.getChangeLampWaveLength()});
        commands.add(com6);
        // 能量灯
        Command com7 = new Command();
        com7.setCmdId(SetParamEnum.SetLampPos.getComId());
        com7.setParams(new Object[]{scanParamInfo.getEnergyLamp()});
        commands.add(com7);
        // 增益
        Command com8 = new Command();
        com8.setCmdId(SetParamEnum.SetGain.getComId());
        com8.setParams(new Object[]{scanParamInfo.getGain()});
        commands.add(com8);
        R<String> stringR = devMessageUtil.spectrumScanSet(commands, ip);
        return Result.ok(stringR.getData());
    }

    /**
     * 查询历史方法接口
     *
     * @param pageNum
     * @param pageSize
     * @param updateBy
     * @param measureType
     * @param methodName
     * @return
     */
    @Override
    public Result queryHistoryMethod(Integer pageNum, Integer pageSize, String updateBy, String measureType, String methodName) {
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        // 根据方法的用户名,测量类型,分页查询,如果有方法名称就加个方法名的模糊查询,如果没有就不加这个条件
        Page<FMeasureMethod> page = new Page<>(pageNum, pageSize);
        QueryWrapper<FMeasureMethod> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("update_by", updateBy)
                .eq("measurement_type", measureType)
                .orderByDesc("method_use_time");
        if (methodName != null) {
            queryWrapper.like("method_name", methodName);
        }
        Page<FMeasureMethod> methodPage = fMeasureMethodService.page(page, queryWrapper);
        if (methodPage.getTotal() == 0){
            return Result.fail("没有查询到数据");
        }

        return Result.ok(methodPage);
    }

    /**
     * 查询历史任务接口
     * @param pageNum
     * @param pageSize
     * @param updateBy
     * @param measureType
     * @param taskName
     * @return
     */
    @Override
    public Result queryHistoryTask(Integer pageNum, Integer pageSize, String updateBy, String measureType, String taskName) {
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        // 根据任务的创建人,测量类型,分页查询,如果有任务名称就加个任务名称的模糊查询,如果没有就不加这个条件
        Page<FMeasureTask> page = new Page<>(pageNum, pageSize);
        QueryWrapper<FMeasureTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("create_by", updateBy)
                .eq("measurement_type", measureType)
                .orderByDesc("create_time");
        if (taskName != null) {
            queryWrapper.like("task_name", taskName);
        }
        Page<FMeasureTask> taskPage = fMeasureTaskService.page(page, queryWrapper);
        return Result.ok(taskPage);
    }
















    /**
     * 获取样品的详细数据
     *
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(FMeasureAnalysisData measureData, Integer digit) {
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        List<FPeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), FPeakValleyMarking.class);
        List<FPeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), FPeakValleyMarking.class);
        List<FPeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), FPeakValleyMarking.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            Map<String, Object> map = new HashMap<>();
            if (digit != null) {
                BigDecimal value = new BigDecimal(split[1]);
                value = value.setScale(digit, RoundingMode.HALF_UP);
                map.put("x", split[0]);
                map.put("y", value.stripTrailingZeros().toPlainString());
            } else {
                map.put("x", split[0]);
                map.put("y", split[1]);
            }
            if (gauges != null && peaks.size() != 0) {
                for (FPeakValleyMarking gauge : gauges) {
                    if (split[0].equals(gauge.getX())) {
                        map.put("marked", "1");
                        break;
                    } else {
                        map.put("marked", "0");
                    }
                }
            } else {
                map.put("marked", "0");
            }
            if (peaks != null && peaks.size() != 0) {
                for (FPeakValleyMarking peak : peaks) {
                    if (split[0].equals(peak.getX())) {
                        map.put("feng", "1");
                        break;
                    } else {
                        map.put("feng", "0");
                    }
                }
            } else {
                map.put("feng", "0");
            }
            if (grades != null && peaks.size() != 0) {
                for (FPeakValleyMarking grade : grades) {
                    if (split[0].equals(grade.getX())) {
                        map.put("gu", "1");
                        break;
                    } else {
                        map.put("gu", "0");
                    }
                }
            } else {
                map.put("gu", "0");
            }
            resultList.add(map);
        }
        return resultList;
    }


    /**
     * 数字字符串归一化（防止 360 / 360.0 / 3.6E2 比较失败）
     */
    private String normalizeNumberString(String numStr) {
        return new BigDecimal(numStr)
                .stripTrailingZeros()
                .toPlainString();
    }
}
