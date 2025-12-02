/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.systemManageService.impl;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemPanelConfDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemLoginConfDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemPanelConfDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemPanelOrderDTO;
import com.persee.workstation.uv_inexus.enums.systemManageEnums.SystemManageEnum;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemPanelConfPO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemPanelConfService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemLoginConfVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemPanelConfVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Slf4j
public class SystemPanelConfServiceImpl implements SystemPanelConfService {

    @Autowired
    SystemPanelConfDAO dao;

    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Value("${sys-manage-default.userId}")
    String userId;

    @Override
    public Boolean savePanelConf(SystemPanelConfDTO dto) {
        SystemPanelConfPO po = dao.selectById(userId);
        SystemPanelConfPO defaultPO = dao.selectById(defaultId);

        if (po == null) {
            po = new SystemPanelConfPO();
            BeanUtils.copyProperties(defaultPO, po);
            BeanUtils.copyProperties(dto, po);
            po.setId(userId);
            po.setCreateBy(dto.getUpdateBy());
            List<String> panelOrder = new ArrayList<>();
            if (po.getProjectStatisticsSwitch()) {
                panelOrder.add(SystemManageEnum.projectPanel.getName());
            }
            if (po.getMethodListSwitch()) {
                panelOrder.add(SystemManageEnum.methodPanel.getName());
            }
            if (po.getTaskListSwitch()) {
                panelOrder.add(SystemManageEnum.taskPanel.getName());
            }
            po.setPanelOrder(JSON.toJSONString(panelOrder));

            if (po.getQuickLoginSwitch() == null || !po.getQuickLoginSwitch()){
                po.setDefaultQuickLogin(false);
            }

            return dao.insert(po) > 0;
        } else {
            BeanUtils.copyProperties(dto, po);
            po.setId(userId);

            List<String> panelOrder = new ArrayList<>();

            if (StringUtils.hasText(po.getPanelOrder())) {
                panelOrder= JSON.parseArray(po.getPanelOrder(), String.class);
            }
            LinkedHashSet<String> set = dealPanelOrder(panelOrder, po);
            po.setPanelOrder(JSON.toJSONString(new ArrayList<>(set)));

            if (po.getQuickLoginSwitch() == null || !po.getQuickLoginSwitch()){
                po.setDefaultQuickLogin(false);
            }

            return dao.updateById(po) > 0;
        }
    }

    private static LinkedHashSet<String> dealPanelOrder(List<String> panelOrder, SystemPanelConfPO po) {
        LinkedHashSet<String> set = new LinkedHashSet<>(panelOrder);
        if (po.getProjectStatisticsSwitch() != null && po.getProjectStatisticsSwitch()) {
            set.add(SystemManageEnum.projectPanel.getName());
        } else {
            set.remove(SystemManageEnum.projectPanel.getName());
        }
        if (po.getMethodListSwitch() != null && po.getMethodListSwitch()) {
            set.add(SystemManageEnum.methodPanel.getName());
        } else {
            set.remove(SystemManageEnum.methodPanel.getName());
        }
        if (po.getTaskListSwitch() != null && po.getTaskListSwitch()) {
            set.add(SystemManageEnum.taskPanel.getName());
        } else {
            set.remove(SystemManageEnum.taskPanel.getName());
        }
        return set;
    }

    @Override
    public SystemPanelConfVO queryPanelConf() {

        SystemPanelConfPO po = dao.selectById(userId);
        SystemPanelConfPO confPO = dao.selectById(defaultId);
        SystemPanelConfVO vo = new SystemPanelConfVO();
        BeanUtils.copyProperties(confPO, vo);
        if (po != null) {
            BeanUtils.copyProperties(po, vo, getNullPropertyNames(po));
        }

        return vo;
    }
    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        return emptyNames.toArray(new String[0]);
    }

    @Override
    public Boolean resetPanelConf(String updateBy) {
        SystemPanelConfPO po = dao.selectById(userId);
        if (po != null) {
            return dao.deleteById(po) > 0;
        } else {
            log.info("没有用户记录");
        }
        return true;
    }

    @Override
    public List<String> queryPanelOrder() {
        List<String> ans = new ArrayList<>();
        SystemPanelConfPO po = dao.selectById(userId);
        SystemPanelConfPO confPO = dao.selectById(defaultId);
        if (po == null || !StringUtils.hasText(po.getPanelOrder())) {
            ans = JSON.parseArray(confPO.getPanelOrder(), String.class);
        } else {
            ans = JSON.parseArray(po.getPanelOrder(), String.class);
        }
        return ans;
    }

    @Override
    public R<Boolean> savePanelOrder(SystemPanelOrderDTO dto) {
        if (!StringUtils.hasText(dto.getUpdateBy())) {
            //用户未登录
            return R.fail(YamlMessageSource.getMessageStatic("00607001"));
        }

        SystemPanelConfPO po = dao.selectById(userId);
        if (po == null) {
            po = new SystemPanelConfPO();
        }
        po.setId(userId);
        po.setCreateBy(dto.getUpdateBy());
        po.setUpdateBy(dto.getUpdateBy());
        if (dto.getPanelOrder() == null || dto.getPanelOrder().isEmpty()) {
            po.setMethodListSwitch(false);
            po.setProjectStatisticsSwitch(false);
            po.setTaskListSwitch(false);
            po.setPanelOrder(JSON.toJSONString(new ArrayList<>()));
            dao.updateById(po);
            return R.ok();
        }
        po.setPanelOrder(JSON.toJSONString(dto.getPanelOrder()));
        po.setMethodListSwitch(false);
        po.setProjectStatisticsSwitch(false);
        po.setTaskListSwitch(false);

        for (String s : dto.getPanelOrder()) {
            if (s.equals(SystemManageEnum.methodPanel.getName())) {
                po.setMethodListSwitch(true);
            } else if (s.equals(SystemManageEnum.projectPanel.getName())) {
                po.setProjectStatisticsSwitch(true);
            } else if (s.equals(SystemManageEnum.taskPanel.getName())) {
                po.setTaskListSwitch(true);
            }
        }
        dao.updateById(po);

        return R.ok();
    }

    @Override
    public R<Boolean> changeLoginInfo(SystemLoginConfDTO dto) {

        SystemPanelConfDTO confDTO = new SystemPanelConfDTO();
        SystemPanelConfPO po = dao.selectById(userId);
        if (po == null) {
            po = dao.selectById(defaultId);
        }
        BeanUtils.copyProperties(po, confDTO);
        BeanUtils.copyProperties(dto, confDTO);
        Boolean b = this.savePanelConf(confDTO);

        if (b) {
            return R.ok();
        } else {
            return R.fail();
        }
    }

    @Override
    public R<SystemLoginConfVO> queryLoginInfo() {
        SystemLoginConfVO vo = new SystemLoginConfVO();
        SystemPanelConfPO po = dao.selectById(userId);
        SystemPanelConfPO confPO = dao.selectById(defaultId);
        if (po == null) {
            BeanUtils.copyProperties(confPO, vo);
        } else {
           BeanUtils.copyProperties(po, vo, getNullPropertyNames(po));
        }
        return R.ok(vo);
    }
}
