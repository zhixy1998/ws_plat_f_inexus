package com.persee.workstation.uv_inexus.controller.measurementController;

import com.alibaba.fastjson.JSONObject;
import com.persee.workstation.uv_inexus.dto.instrumentDto.DeviceInitDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.CreateTaskParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ExperimentMethodDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.measurementService.ExperimentTestService;
import com.persee.workstation.uv_inexus.service.measurementService.SpectrumService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/spectrumScanning/")
@ApiModel("光谱扫描")
public class SpectrumController {

    @Autowired
    private SpectrumService spectrumService;

    @Autowired
    private ExperimentTestService experimentTestService;

    @GetMapping("queryAllProject")
    @ApiModelProperty("查询所有项目接口")
    public Result queryAllProject(@RequestParam String updateBy) {
        return spectrumService.queryAllProject(updateBy);
    }


    @GetMapping("queryEssentialInformation")
    @ApiModelProperty("查询项目任务方法接口")
    public Result queryProjectTaskMethod(@RequestParam String updateBy) {
        return spectrumService.queryProjectTaskMethod(updateBy);
    }

    @GetMapping("queryProjectAllMethods/{projectId}/{measureForm}")
    @ApiModelProperty("查询项目下的所有方法")
    public Result queryProjectAllMethods(@PathVariable String projectId, @PathVariable String measureForm) {
        return spectrumService.queryProjectAllMethods(projectId,measureForm);
    }

    @GetMapping("methodParameter/{method_id}")
    @ApiModelProperty("查询方法参数接口")
    public Result queryMethodParameter(@PathVariable String method_id) {
        return spectrumService.queryMethodParameter(method_id);
    }

    @PostMapping("newMethod")
    @ApiModelProperty("新建方法接口")
    public Result newMethod(@RequestBody ExperimentMethodDto methodDto) {
        return spectrumService.newMethod(methodDto);
    }
    @PostMapping("createTask")
    @ApiModelProperty("新建任务接口")
    public Result createTask(@RequestBody CreateTaskParamDto paramDto) {
        return experimentTestService.createTask(paramDto);
    }
    @PostMapping("saveMethodParameter")
    @ApiModelProperty("保存方法参数接口")
    public Result saveMethodParameter(@RequestBody ExperimentMethodDto methodDto) {
        return spectrumService.saveMethodParameter(methodDto);
    }

    @GetMapping("measurement/{task_id}")
    @ApiModelProperty("测量接口")
    public Result measurement(@RequestBody DeviceInitDTO initDTO,  @PathVariable String task_id) {
        return spectrumService.measurement();
    }

//    @PostMapping("createTask")
//    @ApiModelProperty("创建任务接口")
//    public  Result createTask(@RequestBody TMeasureTaskDto taskDto) {
//        return SpectrumService.createTask(taskDto);
//    }


    private List<double[]> mockImageData() {
        List<double[]> data = new ArrayList<>();
        // 模拟nm、361nm数据、1组2号数据三列
        data.add(new double[]{800.00, 1.0132, 1.3476});
        data.add(new double[]{799.00, 1.0102, 1.3882});
        data.add(new double[]{798.00, 1.0068, 1.3680});
        data.add(new double[]{797.00, 1.0028, 1.3667});
        data.add(new double[]{796.00, 1.0057, 1.3643});
        data.add(new double[]{795.00, 1.0071, 1.4045});
        data.add(new double[]{794.00, 1.0136, 1.3763});
        data.add(new double[]{793.00, 1.0207, 1.3882});
        data.add(new double[]{792.00, 1.0093, 1.4018});
        data.add(new double[]{791.00, 1.0060, 1.3888});
        data.add(new double[]{790.00, 1.0083, 1.3867});
        data.add(new double[]{789.00, 1.0104, 1.4249});
        data.add(new double[]{788.00, 1.0138, 1.3917});
        data.add(new double[]{787.00, 1.0246, 1.4293});
        data.add(new double[]{786.00, 1.0122, 1.3501});
        data.add(new double[]{785.00, 1.0084, 1.3648});
        data.add(new double[]{784.00, 1.0104, 1.3940});
        data.add(new double[]{783.00, 1.0126, 1.3822});
        data.add(new double[]{782.00, 1.0125, 1.4327});
        data.add(new double[]{781.00, 1.0158, 1.4248});
        data.add(new double[]{780.00, 1.0160, 1.4067});
        data.add(new double[]{779.00, 1.0168, 1.3582});
        data.add(new double[]{778.00, 1.0147, 1.3596});
        data.add(new double[]{777.00, 1.0160, 1.3743});
        data.add(new double[]{776.00, 1.0176, 1.3601});
        data.add(new double[]{775.00, 1.0179, 1.3789});
        data.add(new double[]{774.00, 1.0159, 1.4169});
        data.add(new double[]{773.00, 1.0143, 1.3658});
        data.add(new double[]{772.00, 1.0140, 1.3992});
        data.add(new double[]{771.00, 1.0154, 1.3895});
        data.add(new double[]{770.00, 1.0180, 1.4462});
        data.add(new double[]{769.00, 1.0206, 1.4207});
        data.add(new double[]{768.00, 1.0200, 1.4051});
        data.add(new double[]{767.00, 1.0161, 1.3955});
        data.add(new double[]{766.00, 1.0223, 1.3934});
        data.add(new double[]{765.00, 1.0286, 1.3578});
        data.add(new double[]{764.00, 1.0336, 1.3680});
        data.add(new double[]{763.00, 1.0273, 1.3751});
        data.add(new double[]{762.00, 1.0259, 1.3438});
        data.add(new double[]{761.00, 1.0330, 1.3815});
        data.add(new double[]{760.00, 1.0255, 1.4190});
        data.add(new double[]{759.00, 1.0226, 1.4037});
        data.add(new double[]{758.00, 1.0255, 1.4183});
        data.add(new double[]{757.00, 1.0305, 1.3703});
        data.add(new double[]{756.00, 1.0252, 1.3640});
        data.add(new double[]{755.00, 1.0273, 1.3907});
        data.add(new double[]{754.00, 1.0329, 1.3417});
        data.add(new double[]{753.00, 1.0253, 1.4056});
        data.add(new double[]{752.00, 1.0267, 1.3539});
        data.add(new double[]{751.00, 1.0299, 1.3725});
        data.add(new double[]{750.00, 1.0286, 1.3658});
        data.add(new double[]{749.00, 1.0333, 1.3296});
        data.add(new double[]{748.00, 1.0334, 1.3564});
        data.add(new double[]{747.00, 1.0291, 1.3733});
        data.add(new double[]{746.00, 1.0339, 1.3455});
        data.add(new double[]{745.00, 1.0357, 1.3675});
        data.add(new double[]{744.00, 1.0279, 1.3597});
        data.add(new double[]{743.00, 1.0313, 1.3916});
        data.add(new double[]{742.00, 1.0307, 1.3381});
        data.add(new double[]{741.00, 1.0308, 1.3656});
        data.add(new double[]{740.00, 1.0317, 1.3604});
        data.add(new double[]{739.00, 1.0302, 1.3487});
        data.add(new double[]{738.00, 1.0306, 1.3324});
        data.add(new double[]{737.00, 1.0316, 1.3314});
        data.add(new double[]{736.00, 1.0313, 1.3686});
        data.add(new double[]{735.00, 1.0396, 1.3446});
        data.add(new double[]{734.00, 1.0365, 1.3530});
        data.add(new double[]{733.00, 1.0352, 1.3436});
        data.add(new double[]{732.00, 1.0338, 1.3540});
        data.add(new double[]{731.00, 1.0336, 1.3529});
        data.add(new double[]{730.00, 1.0329, 1.3347});
        data.add(new double[]{729.00, 1.0345, 1.3448});
        data.add(new double[]{728.00, 1.0390, 1.3808});
        data.add(new double[]{727.00, 1.0433, 1.3532});
        data.add(new double[]{726.00, 1.0452, 1.3459});
        data.add(new double[]{725.00, 1.0398, 1.3181});
        data.add(new double[]{724.00, 1.0389, 1.3346});
        data.add(new double[]{723.00, 1.0378, 1.3302});
        data.add(new double[]{722.00, 1.0450, 1.3395});
        data.add(new double[]{721.00, 1.0399, 1.3211});
        data.add(new double[]{720.00, 1.0403, 1.3454});
        data.add(new double[]{719.00, 1.0391, 1.3537});
        data.add(new double[]{718.00, 1.0417, 1.3227});
        data.add(new double[]{717.00, 1.0379, 1.3383});
        data.add(new double[]{716.00, 1.0474, 1.3346});
        data.add(new double[]{715.00, 1.0434, 1.3358});
        data.add(new double[]{714.00, 1.0388, 1.3293});
        data.add(new double[]{713.00, 1.0514, 1.3174});
        data.add(new double[]{712.00, 1.0409, 1.3204});
        data.add(new double[]{711.00, 1.0397, 1.3257});
        data.add(new double[]{710.00, 1.0446, 1.3127});
        data.add(new double[]{709.00, 1.0419, 1.3337});
        data.add(new double[]{708.00, 1.0395, 1.3223});
        data.add(new double[]{707.00, 1.0479, 1.3159});
        data.add(new double[]{706.00, 1.0424, 1.3162});
        data.add(new double[]{705.00, 1.0403, 1.3138});
        data.add(new double[]{704.00, 1.0484, 1.3322});
        data.add(new double[]{703.00, 1.0421, 1.3071});
        data.add(new double[]{702.00, 1.0460, 1.3054});
        data.add(new double[]{701.00, 1.0452, 1.2950});
        data.add(new double[]{700.00, 1.0427, 1.3239});
        data.add(new double[]{699.00, 1.0548, 1.3167});
        data.add(new double[]{698.00, 1.0437, 1.3006});
        data.add(new double[]{697.00, 1.0419, 1.3166});
        data.add(new double[]{696.00, 1.0467, 1.2965});
        data.add(new double[]{695.00, 1.0422, 1.2947});
        data.add(new double[]{694.00, 1.0536, 1.2987});
        data.add(new double[]{693.00, 1.0469, 1.2923});
        data.add(new double[]{692.00, 1.0461, 1.2972});
        data.add(new double[]{691.00, 1.0485, 1.3013});
        data.add(new double[]{690.00, 1.0464, 1.3013});
        data.add(new double[]{689.00, 1.0500, 1.3038});
        data.add(new double[]{688.00, 1.0449, 1.3006});
        data.add(new double[]{687.00, 1.0569, 1.2887});
        data.add(new double[]{686.00, 1.0491, 1.3020});
        data.add(new double[]{685.00, 1.0479, 1.3013});
        data.add(new double[]{684.00, 1.0584, 1.2912});
        data.add(new double[]{683.00, 1.0502, 1.2941});
        data.add(new double[]{682.00, 1.0519, 1.2868});
        data.add(new double[]{681.00, 1.0522, 1.2932});
        data.add(new double[]{680.00, 1.0477, 1.3052});
        data.add(new double[]{679.00, 1.0594, 1.2996});
        data.add(new double[]{678.00, 1.0527, 1.3033});
        data.add(new double[]{677.00, 1.0567, 1.2930});
        data.add(new double[]{676.00, 1.0532, 1.2994});
        data.add(new double[]{675.00, 1.0512, 1.3003});
        data.add(new double[]{674.00, 1.0618, 1.2799});
        data.add(new double[]{673.00, 1.0562, 1.2836});
        data.add(new double[]{672.00, 1.0528, 1.3010});
        data.add(new double[]{671.00, 1.0587, 1.3089});
        data.add(new double[]{670.00, 1.0579, 1.2985});
        data.add(new double[]{669.00, 1.0563, 1.2962});
        data.add(new double[]{668.00, 1.0637, 1.2832});
        data.add(new double[]{667.00, 1.0575, 1.2952});
        data.add(new double[]{666.00, 1.0563, 1.2940});
        data.add(new double[]{665.00, 1.0607, 1.2919});
        data.add(new double[]{664.00, 1.0590, 1.3050});
        data.add(new double[]{663.00, 1.0569, 1.2860});
        data.add(new double[]{662.00, 1.0663, 1.2870});
        data.add(new double[]{661.00, 1.0603, 1.2878});
        data.add(new double[]{660.00, 1.0605, 1.2935});
        data.add(new double[]{659.00, 1.0670, 1.2957});
        data.add(new double[]{658.00, 1.0616, 1.3006});
        data.add(new double[]{657.00, 1.0614, 1.2968});
        data.add(new double[]{656.00, 1.0694, 1.2970});
        data.add(new double[]{655.00, 1.0684, 1.2908});
        data.add(new double[]{654.00, 1.0655, 1.2892});
        data.add(new double[]{653.00, 1.0625, 1.2968});
        data.add(new double[]{652.00, 1.0619, 1.3004});
        data.add(new double[]{651.00, 1.0689, 1.3104});
        data.add(new double[]{650.00, 1.0733, 1.3039});
        data.add(new double[]{649.00, 1.0689, 1.3017});
        data.add(new double[]{648.00, 1.0766, 1.2920});
        data.add(new double[]{647.00, 1.0727, 1.3023});
        data.add(new double[]{646.00, 1.0702, 1.3014});
        data.add(new double[]{645.00, 1.0685, 1.2959});
        data.add(new double[]{644.00, 1.0659, 1.2922});
        data.add(new double[]{643.00, 1.0789, 1.2961});
        data.add(new double[]{642.00, 1.0725, 1.3004});
        data.add(new double[]{641.00, 1.0701, 1.2927});
        data.add(new double[]{640.00, 1.0789, 1.2906});
        data.add(new double[]{639.00, 1.0788, 1.2995});
        data.add(new double[]{638.00, 1.0745, 1.3078});
        data.add(new double[]{637.00, 1.0734, 1.3047});
        data.add(new double[]{636.00, 1.0764, 1.3118});
        data.add(new double[]{635.00, 1.0776, 1.2983});
        data.add(new double[]{634.00, 1.0762, 1.3017});
        data.add(new double[]{633.00, 1.0766, 1.3078});
        data.add(new double[]{632.00, 1.0824, 1.2940});
        data.add(new double[]{631.00, 1.0822, 1.3093});
        data.add(new double[]{630.00, 1.0776, 1.3160});
        data.add(new double[]{629.00, 1.0772, 1.3204});
        data.add(new double[]{628.00, 1.0855, 1.3013});
        data.add(new double[]{627.00, 1.0805, 1.3125});
        data.add(new double[]{626.00, 1.0789, 1.3007});
        data.add(new double[]{625.00, 1.0890, 1.3114});
        data.add(new double[]{624.00, 1.0790, 1.3194});
        data.add(new double[]{623.00, 1.0870, 1.3185});
        data.add(new double[]{622.00, 1.0841, 1.3191});
        data.add(new double[]{621.00, 1.0921, 1.2923});
        data.add(new double[]{620.00, 1.0854, 1.3205});
        data.add(new double[]{619.00, 1.0829, 1.3259});
        data.add(new double[]{618.00, 1.0830, 1.3111});
        data.add(new double[]{617.00, 1.0888, 1.2916});
        data.add(new double[]{616.00, 1.0860, 1.3047});
        data.add(new double[]{615.00, 1.0955, 1.3232});
        data.add(new double[]{614.00, 1.0876, 1.3042});
        data.add(new double[]{613.00, 1.0870, 1.2912});
        data.add(new double[]{612.00, 1.0939, 1.3206});
        data.add(new double[]{611.00, 1.0895, 1.2972});
        data.add(new double[]{610.00, 1.1006, 1.3078});
        data.add(new double[]{609.00, 1.0909, 1.3172});
        data.add(new double[]{608.00, 1.0918, 1.3168});
        data.add(new double[]{607.00, 1.0952, 1.3016});
        data.add(new double[]{606.00, 1.0918, 1.3249});
        data.add(new double[]{605.00, 1.0889, 1.3077});
        data.add(new double[]{604.00, 1.1031, 1.3078});
        data.add(new double[]{603.00, 1.0983, 1.3103});
        data.add(new double[]{602.00, 1.0949, 1.3230});
        data.add(new double[]{601.00, 1.0935, 1.3206});
        data.add(new double[]{600.00, 1.0977, 1.3206});
        data.add(new double[]{599.00, 1.1048, 1.3111});
        data.add(new double[]{598.00, 1.0966, 1.3179});
        data.add(new double[]{597.00, 1.0957, 1.3214});
        data.add(new double[]{596.00, 1.1064, 1.3150});
        data.add(new double[]{595.00, 1.1047, 1.3219});
        data.add(new double[]{594.00, 1.0967, 1.3090});
        data.add(new double[]{593.00, 1.1059, 1.3101});
        data.add(new double[]{592.00, 1.1020, 1.3249});
        data.add(new double[]{591.00, 1.1009, 1.3116});
        data.add(new double[]{590.00, 1.1046, 1.3172});
        data.add(new double[]{589.00, 1.1040, 1.3295});
        data.add(new double[]{588.00, 1.1003, 1.3035});
        data.add(new double[]{587.00, 1.1070, 1.3273});
        data.add(new double[]{586.00, 1.1036, 1.3325});
        data.add(new double[]{585.00, 1.1052, 1.3149});
        data.add(new double[]{584.00, 1.1109, 1.3228});
        data.add(new double[]{583.00, 1.1054, 1.3194});
        data.add(new double[]{582.00, 1.1053, 1.3177});
        data.add(new double[]{581.00, 1.1181, 1.3252});
        data.add(new double[]{580.00, 1.1071, 1.3162});
        data.add(new double[]{579.00, 1.1069, 1.3371});
        data.add(new double[]{578.00, 1.1125, 1.3334});
        data.add(new double[]{577.00, 1.1105, 1.3262});
        data.add(new double[]{576.00, 1.1078, 1.3190});
        data.add(new double[]{575.00, 1.1129, 1.3233});
        data.add(new double[]{574.00, 1.1109, 1.3280});
        data.add(new double[]{573.00, 1.1194, 1.3353});
        data.add(new double[]{572.00, 1.1129, 1.3128});
        data.add(new double[]{571.00, 1.1103, 1.3296});
        data.add(new double[]{570.00, 1.1241, 1.3194});
        data.add(new double[]{569.00, 1.1149, 1.3305});
        data.add(new double[]{568.00, 1.1155, 1.3198});
        data.add(new double[]{567.00, 1.1147, 1.3227});
        data.add(new double[]{566.00, 1.1211, 1.3233});
        data.add(new double[]{565.00, 1.1266, 1.3192});
        data.add(new double[]{564.00, 1.1277, 1.3301});
        data.add(new double[]{563.00, 1.1282, 1.3313});
        data.add(new double[]{562.00, 1.1295, 1.3231});
        data.add(new double[]{561.00, 1.1296, 1.3305});
        data.add(new double[]{560.00, 1.1340, 1.3299});
        data.add(new double[]{559.00, 1.1314, 1.3323});
        data.add(new double[]{558.00, 1.1202, 1.3411});
        data.add(new double[]{557.00, 1.1217, 1.3232});
        data.add(new double[]{556.00, 1.1231, 1.3278});
        data.add(new double[]{555.00, 1.1233, 1.3293});
        data.add(new double[]{554.00, 1.1243, 1.3302});
        data.add(new double[]{553.00, 1.1251, 1.3186});
        data.add(new double[]{552.00, 1.1250, 1.3322});
        data.add(new double[]{551.00, 1.1258, 1.3383});
        data.add(new double[]{550.00, 1.1316, 1.3391});
        data.add(new double[]{549.00, 1.1341, 1.3359});
        data.add(new double[]{548.00, 1.1321, 1.3217});
        data.add(new double[]{547.00, 1.1311, 1.3346});
        data.add(new double[]{546.00, 1.1337, 1.3355});
        data.add(new double[]{545.00, 1.1362, 1.3268});
        data.add(new double[]{544.00, 1.1438, 1.3328});
        data.add(new double[]{543.00, 1.1414, 1.3403});
        data.add(new double[]{542.00, 1.1400, 1.3264});
        data.add(new double[]{541.00, 1.1447, 1.3362});
        data.add(new double[]{540.00, 1.1469, 1.3480});
        data.add(new double[]{539.00, 1.1435, 1.3287});
        data.add(new double[]{538.00, 1.1399, 1.3341});
        data.add(new double[]{537.00, 1.1397, 1.3299});
        data.add(new double[]{536.00, 1.1419, 1.3340});
        data.add(new double[]{535.00, 1.1497, 1.3304});
        data.add(new double[]{534.00, 1.1476, 1.3383});
        data.add(new double[]{533.00, 1.1495, 1.3535});
        data.add(new double[]{532.00, 1.1535, 1.3315});
        data.add(new double[]{531.00, 1.1554, 1.3224});
        data.add(new double[]{530.00, 1.1406, 1.3322});
        data.add(new double[]{529.00, 1.1406, 1.3458});
        data.add(new double[]{528.00, 1.1415, 1.3322});
        data.add(new double[]{527.00, 1.1441, 1.3228});
        data.add(new double[]{526.00, 1.1461, 1.3434});
        data.add(new double[]{525.00, 1.1562, 1.3321});
        data.add(new double[]{524.00, 1.1550, 1.3381});
        data.add(new double[]{523.00, 1.1471, 1.3305});
        data.add(new double[]{522.00, 1.1468, 1.3189});
        data.add(new double[]{521.00, 1.1543, 1.3314});
        data.add(new double[]{520.00, 1.1607, 1.3434});
        data.add(new double[]{519.00, 1.1479, 1.3233});
        data.add(new double[]{518.00, 1.1522, 1.3216});
        data.add(new double[]{517.00, 1.1632, 1.3371});
        data.add(new double[]{516.00, 1.1508, 1.3372});
        data.add(new double[]{515.00, 1.1519, 1.3325});
        data.add(new double[]{514.00, 1.1666, 1.3386});
        data.add(new double[]{513.00, 1.1515, 1.3464});
        data.add(new double[]{512.00, 1.1666, 1.3382});
        data.add(new double[]{511.00, 1.1534, 1.3267});
        data.add(new double[]{510.00, 1.1623, 1.3249});
        data.add(new double[]{509.00, 1.1547, 1.3382});
        data.add(new double[]{508.00, 1.1615, 1.3242});
        data.add(new double[]{507.00, 1.1556, 1.3296});
        data.add(new double[]{506.00, 1.1642, 1.3299});
        data.add(new double[]{505.00, 1.1613, 1.3414});
        data.add(new double[]{504.00, 1.1595, 1.3450});
        data.add(new double[]{503.00, 1.1740, 1.3337});
        data.add(new double[]{502.00, 1.1593, 1.3289});
        data.add(new double[]{501.00, 1.1634, 1.3363});
        data.add(new double[]{500.00, 1.1757, 1.3334});
        data.add(new double[]{499.00, 1.1596, 1.3236});
        data.add(new double[]{498.00, 1.1775, 1.3430});
        data.add(new double[]{497.00, 1.1623, 1.3300});
        data.add(new double[]{496.00, 1.1712, 1.3399});
        data.add(new double[]{495.00, 1.1639, 1.3357});
        data.add(new double[]{494.00, 1.1751, 1.3348});
        data.add(new double[]{493.00, 1.1701, 1.3297});
        data.add(new double[]{492.00, 1.1645, 1.3468});
        data.add(new double[]{491.00, 1.1728, 1.3346});
        data.add(new double[]{490.00, 1.1702, 1.3395});
        data.add(new double[]{489.00, 1.1650, 1.3295});
        data.add(new double[]{488.00, 1.1816, 1.3322});
        data.add(new double[]{487.00, 1.1680, 1.3257});
        data.add(new double[]{486.00, 1.1849, 1.3332});
        data.add(new double[]{485.00, 1.1691, 1.3325});
        data.add(new double[]{484.00, 1.1742, 1.3419});
        data.add(new double[]{483.00, 1.1852, 1.3242});
        data.add(new double[]{482.00, 1.1679, 1.3401});
        data.add(new double[]{481.00, 1.1693, 1.3347});
        data.add(new double[]{480.00, 1.1730, 1.3434});
        data.add(new double[]{479.00, 1.1722, 1.3310});
        data.add(new double[]{478.00, 1.1743, 1.3443});
        data.add(new double[]{477.00, 1.1750, 1.3278});
        data.add(new double[]{476.00, 1.1767, 1.3351});
        data.add(new double[]{475.00, 1.1806, 1.3374});
        data.add(new double[]{474.00, 1.1801, 1.3346});
        data.add(new double[]{473.00, 1.1857, 1.3355});
        data.add(new double[]{472.00, 1.1911, 1.3313});
        data.add(new double[]{471.00, 1.1840, 1.3364});
        data.add(new double[]{470.00, 1.1750, 1.3300});
        data.add(new double[]{469.00, 1.1809, 1.3354});
        data.add(new double[]{468.00, 1.1906, 1.3266});
        data.add(new double[]{467.00, 1.1766, 1.3255});
        data.add(new double[]{466.00, 1.1766, 1.3431});
        data.add(new double[]{465.00, 1.1780, 1.3322});
        data.add(new double[]{464.00, 1.1848, 1.3198});
        data.add(new double[]{463.00, 1.1879, 1.3374});
        data.add(new double[]{462.00, 1.1747, 1.3150});
        data.add(new double[]{461.00, 1.1794, 1.3353});
        data.add(new double[]{460.00, 1.1780, 1.3370});
        data.add(new double[]{459.00, 1.1773, 1.3237});
        data.add(new double[]{458.00, 1.1760, 1.3144});
        data.add(new double[]{457.00, 1.1806, 1.3220});
        data.add(new double[]{456.00, 1.1808, 1.3271});
        data.add(new double[]{455.00, 1.1769, 1.3296});
        data.add(new double[]{454.00, 1.1803, 1.3276});
        data.add(new double[]{453.00, 1.1800, 1.3359});
        data.add(new double[]{452.00, 1.1767, 1.3326});
        data.add(new double[]{451.00, 1.1789, 1.3410});
        data.add(new double[]{450.00, 1.1803, 1.3272});
        data.add(new double[]{449.00, 1.1844, 1.3401});
        data.add(new double[]{448.00, 1.1911, 1.3598});
        data.add(new double[]{447.00, 1.1981, 1.3455});
        data.add(new double[]{446.00, 1.1828, 1.3478});
        data.add(new double[]{445.00, 1.1848, 1.3500});
        data.add(new double[]{444.00, 1.2012, 1.3584});
        data.add(new double[]{443.00, 1.1859, 1.3489});
        data.add(new double[]{442.00, 1.1987, 1.3404});
        data.add(new double[]{441.00, 1.1850, 1.3464});
        data.add(new double[]{440.00, 1.1986, 1.3484});
        data.add(new double[]{439.00, 1.1876, 1.3367});
        data.add(new double[]{438.00, 1.2052, 1.3469});
        data.add(new double[]{437.00, 1.1903, 1.3394});
        data.add(new double[]{436.00, 1.1879, 1.3428});
        data.add(new double[]{435.00, 1.2052, 1.3297});
        data.add(new double[]{434.00, 1.1997, 1.3396});
        data.add(new double[]{433.00, 1.1892, 1.3455});
        data.add(new double[]{432.00, 1.1960, 1.3425});
        data.add(new double[]{431.00, 1.2077, 1.3448});
        data.add(new double[]{430.00, 1.2068, 1.3330});
        data.add(new double[]{429.00, 1.1972, 1.3487});
        data.add(new double[]{428.00, 1.1958, 1.3430});
        data.add(new double[]{427.00, 1.1924, 1.3508});
        data.add(new double[]{426.00, 1.1989, 1.3459});
        data.add(new double[]{425.00, 1.2103, 1.3328});
        data.add(new double[]{424.00, 1.2149, 1.3354});
        data.add(new double[]{423.00, 1.1971, 1.3328});
        data.add(new double[]{422.00, 1.1993, 1.3451});
        data.add(new double[]{421.00, 1.1944, 1.3369});
        data.add(new double[]{420.00, 1.1993, 1.3336});
        data.add(new double[]{419.00, 1.2107, 1.3373});
        data.add(new double[]{418.00, 1.2024, 1.3358});
        data.add(new double[]{417.00, 1.1945, 1.3552});
        data.add(new double[]{416.00, 1.1948, 1.3492});
        data.add(new double[]{415.00, 1.2035, 1.3426});
        data.add(new double[]{414.00, 1.1999, 1.3436});
        data.add(new double[]{413.00, 1.1990, 1.3397});
        data.add(new double[]{412.00, 1.2040, 1.3482});
        data.add(new double[]{411.00, 1.2000, 1.3438});
        data.add(new double[]{410.00, 1.2000, 1.3460});
        data.add(new double[]{409.00, 1.2125, 1.3324});
        data.add(new double[]{408.00, 1.2204, 1.3458});
        data.add(new double[]{407.00, 1.2068, 1.3496});
        data.add(new double[]{406.00, 1.2014, 1.3251});
        data.add(new double[]{405.00, 1.2163, 1.3364});
        data.add(new double[]{404.00, 1.2055, 1.3449});
        data.add(new double[]{403.00, 1.2037, 1.3555});
        data.add(new double[]{402.00, 1.2099, 1.3503});
        data.add(new double[]{401.00, 1.2149, 1.3432});
        data.add(new double[]{400.00, 1.2122, 1.3535});
        data.add(new double[]{399.00, 1.2177, 1.3391});
        data.add(new double[]{398.00, 1.2090, 1.3453});
        data.add(new double[]{397.00, 1.2033, 1.3550});
        data.add(new double[]{396.00, 1.2060, 1.3398});
        data.add(new double[]{395.00, 1.2097, 1.3561});
        data.add(new double[]{394.00, 1.2122, 1.3495});
        data.add(new double[]{393.00, 1.2141, 1.3436});
        data.add(new double[]{392.00, 1.2102, 1.3423});
        data.add(new double[]{391.00, 1.2119, 1.3438});
        data.add(new double[]{390.00, 1.2149, 1.3516});
        data.add(new double[]{389.00, 1.2194, 1.3485});
        data.add(new double[]{388.00, 1.2082, 1.3442});
        data.add(new double[]{387.00, 1.2031, 1.3376});
        data.add(new double[]{386.00, 1.2208, 1.3458});
        data.add(new double[]{385.00, 1.2184, 1.3573});
        data.add(new double[]{384.00, 1.2031, 1.3395});
        data.add(new double[]{383.00, 1.2119, 1.3543});
        data.add(new double[]{382.00, 1.2170, 1.3516});
        data.add(new double[]{381.00, 1.2093, 1.3389});
        data.add(new double[]{380.00, 1.2108, 1.3606});
        data.add(new double[]{379.00, 1.2173, 1.3273});
        data.add(new double[]{378.00, 1.2080, 1.3517});
        data.add(new double[]{377.00, 1.2015, 1.3555});
        data.add(new double[]{376.00, 1.2081, 1.3492});
        data.add(new double[]{375.00, 1.2263, 1.3443});
        data.add(new double[]{374.00, 1.2054, 1.3462});
        data.add(new double[]{373.00, 1.2204, 1.3593});
        data.add(new double[]{372.00, 1.2191, 1.3571});
        data.add(new double[]{371.00, 1.2113, 1.3386});
        data.add(new double[]{370.00, 1.2239, 1.3581});
        data.add(new double[]{369.00, 1.2115, 1.3524});
        data.add(new double[]{368.00, 1.2210, 1.3519});
        data.add(new double[]{367.00, 1.2111, 1.3461});
        data.add(new double[]{366.00, 1.2170, 1.3501});
        data.add(new double[]{365.00, 1.2201, 1.3590});
        data.add(new double[]{364.00, 1.2270, 1.3544});
        data.add(new double[]{363.00, 1.2200, 1.3410});
        data.add(new double[]{362.00, 1.2258, 1.3598});
        data.add(new double[]{361.00, 1.4459, 1.2300});
        data.add(new double[]{360.00, 1.2123, 1.2335});
        data.add(new double[]{359.00, 1.2223, 1.2185});
        data.add(new double[]{358.00, 1.2114, 1.2447});
        data.add(new double[]{357.00, 1.2301, 1.2359});
        data.add(new double[]{356.00, 1.2132, 1.2302});
        data.add(new double[]{355.00, 1.2241, 1.2199});
        data.add(new double[]{354.00, 1.2160, 1.2406});
        data.add(new double[]{353.00, 1.2283, 1.2175});
        data.add(new double[]{352.00, 1.2190, 1.2190});
        data.add(new double[]{351.00, 1.2320, 1.2248});
        data.add(new double[]{350.00, 1.2231, 1.2391});
        data.add(new double[]{349.00, 1.2190, 1.2110});
        data.add(new double[]{348.00, 1.2297, 1.2375});
        data.add(new double[]{347.00, 1.2190, 1.2027});
        data.add(new double[]{346.00, 1.2284, 1.2335});
        data.add(new double[]{345.00, 1.2200, 1.2271});
        data.add(new double[]{344.00, 1.2216, 1.2133});
        data.add(new double[]{343.00, 1.2185, 1.2150});
        data.add(new double[]{342.00, 1.2270, 1.2158});
        data.add(new double[]{341.00, 1.2238, 1.2098});
        data.add(new double[]{340.00, 1.2260, 1.2245});
        data.add(new double[]{339.00, 1.2295, 1.2072});
        data.add(new double[]{338.00, 1.2251, 1.2310});
        data.add(new double[]{337.00, 1.2369, 1.2144});
        data.add(new double[]{336.00, 1.2216, 1.2100});
        data.add(new double[]{335.00, 1.2269, 1.2233});
        data.add(new double[]{334.00, 1.2261, 1.2262});
        data.add(new double[]{333.00, 1.2271, 1.2257});
        data.add(new double[]{332.00, 1.2263, 1.2117});
        data.add(new double[]{331.00, 1.2244, 1.2322});
        data.add(new double[]{330.00, 1.2414, 1.2289});
        data.add(new double[]{329.00, 1.2199, 1.2141});
        data.add(new double[]{328.00, 1.2292, 1.2387});
        data.add(new double[]{327.00, 1.2351, 1.2407});
        data.add(new double[]{326.00, 1.2290, 1.2113});
        data.add(new double[]{325.00, 1.2198, 1.2392});
        data.add(new double[]{324.00, 1.2297, 1.2287});
        data.add(new double[]{323.00, 1.2378, 1.2330});
        data.add(new double[]{322.00, 1.2445, 1.2244});
        data.add(new double[]{321.00, 1.2447, 1.2364});
        data.add(new double[]{320.00, 1.2405, 1.2174});
        data.add(new double[]{319.00, 1.2485, 1.2182});
        data.add(new double[]{318.00, 1.2530, 1.2204});
        data.add(new double[]{317.00, 1.2480, 1.2295});
        data.add(new double[]{316.00, 1.2425, 1.2148});
        data.add(new double[]{315.00, 1.2447, 1.2140});
        data.add(new double[]{314.00, 1.2445, 1.2220});
        data.add(new double[]{313.00, 1.2487, 1.2198});
        data.add(new double[]{312.00, 1.2522, 1.2227});
        data.add(new double[]{311.00, 1.2497, 1.2060});
        data.add(new double[]{310.00, 1.2499, 1.2028});
        data.add(new double[]{309.00, 1.2550, 1.2250});
        data.add(new double[]{308.00, 1.2495, 1.2254});
        data.add(new double[]{307.00, 1.2356, 1.2080});
        data.add(new double[]{306.00, 1.2311, 1.2253});
        data.add(new double[]{305.00, 1.2357, 1.2102});
        data.add(new double[]{304.00, 1.2456, 1.2088});
        data.add(new double[]{303.00, 1.2431, 1.2050});
        data.add(new double[]{302.00, 1.2488, 1.2109});
        data.add(new double[]{301.00, 1.2389, 1.2032});
        data.add(new double[]{300.00, 1.2343, 1.2076});
        data.add(new double[]{299.00, 1.2318, 1.2178});
        data.add(new double[]{298.00, 1.2290, 1.2033});
        data.add(new double[]{297.00, 1.2348, 1.2000});
        data.add(new double[]{296.00, 1.2448, 1.2189});
        data.add(new double[]{295.00, 1.2431, 1.2118});
        data.add(new double[]{294.00, 1.2314, 1.2152});
        data.add(new double[]{293.00, 1.2296, 1.2177});
        data.add(new double[]{292.00, 1.2347, 1.2205});
        data.add(new double[]{291.00, 1.2481, 1.2099});
        data.add(new double[]{290.00, 1.2412, 1.2071});
        data.add(new double[]{289.00, 1.2345, 1.2043});
        data.add(new double[]{288.00, 1.2301, 1.2098});
        data.add(new double[]{287.00, 1.2392, 1.2174});
        data.add(new double[]{286.00, 1.2544, 1.2100});
        data.add(new double[]{285.00, 1.2415, 1.2142});
        data.add(new double[]{284.00, 1.2323, 1.2147});
        data.add(new double[]{283.00, 1.2311, 1.2059});
        data.add(new double[]{282.00, 1.2394, 1.2126});
        data.add(new double[]{281.00, 1.2295, 1.2051});
        data.add(new double[]{280.00, 1.2347, 1.2104});
        data.add(new double[]{279.00, 1.2497, 1.1998});
        data.add(new double[]{278.00, 1.2295, 1.1938});
        data.add(new double[]{277.00, 1.2290, 1.2144});
        data.add(new double[]{276.00, 1.2440, 1.1952});
        data.add(new double[]{275.00, 1.2392, 1.2054});
        data.add(new double[]{274.00, 1.2257, 1.1935});
        data.add(new double[]{273.00, 1.2222, 1.1878});
        data.add(new double[]{272.00, 1.2316, 1.1957});
        data.add(new double[]{271.00, 1.2391, 1.1956});
        data.add(new double[]{270.00, 1.2287, 1.1935});
        data.add(new double[]{269.00, 1.2264, 1.2094});
        data.add(new double[]{268.00, 1.2191, 1.1969});
        data.add(new double[]{267.00, 1.2214, 1.1949});
        data.add(new double[]{266.00, 1.2174, 1.2051});
        data.add(new double[]{265.00, 1.2156, 1.1968});
        data.add(new double[]{264.00, 1.2072, 1.1989});
        data.add(new double[]{263.00, 1.2070, 1.1883});
        data.add(new double[]{262.00, 1.2067, 1.2047});
        data.add(new double[]{261.00, 1.2031, 1.1951});
        data.add(new double[]{260.00, 1.2018, 1.1972});
        data.add(new double[]{259.00, 1.1933, 1.2098});
        data.add(new double[]{258.00, 1.1955, 1.1999});
        data.add(new double[]{258.00, 1.1955, 1.1999});
        data.add(new double[]{257.00, 1.1956, 1.2076});
        data.add(new double[]{256.00, 1.1925, 1.1983});
        data.add(new double[]{255.00, 1.1933, 1.2019});
        data.add(new double[]{254.00, 1.1867, 1.2062});
        data.add(new double[]{253.00, 1.1826, 1.2143});
        data.add(new double[]{252.00, 1.1794, 1.2130});
        data.add(new double[]{251.00, 1.1606, 1.1928});
        data.add(new double[]{250.00, 1.1432, 1.1863});
        data.add(new double[]{249.00, 1.1410, 1.2099});
        data.add(new double[]{248.00, 1.1352, 1.2063});
        data.add(new double[]{247.00, 1.1240, 1.2046});
        data.add(new double[]{246.00, 1.1188, 1.2070});
        data.add(new double[]{245.00, 1.1115, 1.1897});
        data.add(new double[]{244.00, 1.1115, 1.1960});
        data.add(new double[]{243.00, 1.0935, 1.1876});
        data.add(new double[]{242.00, 1.0907, 1.2163});
        data.add(new double[]{241.00, 1.0814, 1.1802});
        data.add(new double[]{240.00, 1.0729, 1.1722});
        data.add(new double[]{239.00, 1.0668, 1.1872});
        data.add(new double[]{238.00, 1.0600, 1.1793});
        data.add(new double[]{237.00, 1.0569, 1.1819});
        data.add(new double[]{236.00, 1.0456, 1.1682});
        data.add(new double[]{235.00, 1.0470, 1.1686});
        data.add(new double[]{234.00, 1.0371, 1.1723});
        data.add(new double[]{233.00, 1.0368, 1.1647});
        data.add(new double[]{232.00, 1.0293, 1.1804});
        data.add(new double[]{231.00, 1.0338, 1.1701});
        data.add(new double[]{230.00, 1.0237, 1.1433});
        data.add(new double[]{229.00, 1.0224, 1.1692});
        data.add(new double[]{228.00, 1.0245, 1.1378});
        data.add(new double[]{227.00, 1.0213, 1.1280});
        data.add(new double[]{226.00, 1.0233, 1.1552});
        data.add(new double[]{225.00, 1.0249, 1.1218});
        data.add(new double[]{224.00, 1.0252, 1.1453});
        data.add(new double[]{223.00, 1.0273, 1.1270});
        data.add(new double[]{222.00, 1.0307, 1.1283});
        data.add(new double[]{221.00, 1.0359, 1.1127});
        data.add(new double[]{220.00, 1.0324, 1.0991});
        data.add(new double[]{219.00, 1.0353, 1.1314});
        data.add(new double[]{218.00, 1.0346, 1.1215});
        data.add(new double[]{217.00, 1.0406, 1.1074});
        data.add(new double[]{216.00, 1.0535, 1.1441});
        data.add(new double[]{215.00, 1.0530, 1.1122});
        data.add(new double[]{214.00, 1.0447, 1.1168});
        data.add(new double[]{213.00, 1.0522, 1.1260});
        data.add(new double[]{212.00, 1.0597, 1.0852});
        data.add(new double[]{211.00, 1.0562, 1.1391});
        data.add(new double[]{210.00, 1.0569, 1.1365});
        data.add(new double[]{209.00, 1.0592, 1.1041});
        data.add(new double[]{208.00, 1.0430, 1.1261});
        data.add(new double[]{207.00, 1.0458, 1.1504});
        data.add(new double[]{206.00, 1.0403, 1.1408});
        data.add(new double[]{205.00, 1.0261, 1.1539});
        data.add(new double[]{204.00, 1.0426, 1.1351});
        data.add(new double[]{203.00, 1.0295, 1.1708});
        data.add(new double[]{202.00, 1.0336, 1.1537});
        data.add(new double[]{201.00, 1.0261, 1.1272});
        data.add(new double[]{200.00, 0.9938, 1.1639});

        return data;
    }



    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @GetMapping(path = "/sse-data", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSseMvc() {
        SseEmitter emitter = new SseEmitter(60_000L); // 超时时间60秒
        List<double[]> imageData = mockImageData();

        executorService.execute(() -> {
            try {
                for (int i = 0; i < imageData.size(); i++) {
                    double[] point = imageData.get(i);
                    JSONObject json = new JSONObject();
                    json.put("nm", point[0]);
                    json.put("col2", point[1]);
                    json.put("col3", point[2]);

                    // 发送SSE消息
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(i))
                            .name("data")
                            .data(json.toJSONString()));

                    // 控制数据推送速度，500ms一个点
                    Thread.sleep(500);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

}
