package com.usthe.manager.service.impl;

import com.usthe.common.entity.job.Configmap;
import com.usthe.common.entity.job.Job;
import com.usthe.common.entity.message.CollectRep;
import com.usthe.common.util.CommonConstants;
import com.usthe.common.util.SnowFlakeIdGenerator;
import com.usthe.manager.dao.MonitorDao;
import com.usthe.manager.dao.ParamDao;
import com.usthe.manager.pojo.dto.MonitorDto;
import com.usthe.manager.pojo.entity.Monitor;
import com.usthe.manager.pojo.entity.Param;
import com.usthe.manager.service.AppService;
import com.usthe.manager.service.MonitorService;
import com.usthe.manager.support.exception.MonitorDatabaseException;
import com.usthe.manager.support.exception.MonitorDetectException;
import com.usthe.scheduler.JobScheduling;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 监控管理服务实现
 *
 *
 */
@Service
@Slf4j
public class MonitorServiceImpl implements MonitorService {

    private static final Long MONITOR_ID_TMP = 1000000000L;

    @Autowired
    private AppService appService;

    @Autowired
    private JobScheduling jobScheduling;

    @Autowired
    private MonitorDao monitorDao;

    @Autowired
    private ParamDao paramDao;

    @Override
    @Transactional(readOnly = true)
    public void detectMonitor(Monitor monitor, List<Param> params) throws MonitorDetectException {
        Long monitorId = monitor.getId();
        if (monitorId == null || monitorId == 0) {
            monitorId = MONITOR_ID_TMP;
        }
        Job appDefine = appService.getAppDefine(monitor.getApp());
        appDefine.setMonitorId(monitorId);
        appDefine.setCyclic(false);
        appDefine.setTimestamp(System.currentTimeMillis());
        List<Configmap> configmaps = params.stream().map(param ->
                new Configmap(param.getField(), param.getValue(), param.getType())).collect(Collectors.toList());
        appDefine.setConfigmap(configmaps);
        List<CollectRep.MetricsData> collectRep = jobScheduling.addSyncCollectJob(appDefine);
        // 判断探测结果 失败则抛出探测异常
        if (collectRep == null || collectRep.isEmpty()) {
            throw new MonitorDetectException("No collector response");
        }
        if (collectRep.get(0).getCode() != CollectRep.Code.SUCCESS) {
            throw new MonitorDetectException(collectRep.get(0).getMsg());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMonitor(Monitor monitor, List<Param> params) throws RuntimeException {
        // 申请 monitor id
        long monitorId = SnowFlakeIdGenerator.generateId();
        // 构造采集任务Job实体
        Job appDefine = appService.getAppDefine(monitor.getApp());
        appDefine.setMonitorId(monitorId);
        appDefine.setInterval(monitor.getIntervals());
        appDefine.setCyclic(true);
        appDefine.setTimestamp(System.currentTimeMillis());
        List<Configmap> configmaps = params.stream().map(param -> {
            param.setMonitorId(monitorId);
            param.setGmtCreate(null);
            param.setGmtUpdate(null);
            return new Configmap(param.getField(), param.getValue(), param.getType());
        }).collect(Collectors.toList());
        appDefine.setConfigmap(configmaps);
        // 下发采集任务得到jobId
        long jobId = jobScheduling.addAsyncCollectJob(appDefine);
        // 下发成功后刷库
        try {
            monitor.setId(monitorId);
            monitor.setJobId(jobId);
            monitor.setStatus(CommonConstants.AVAILABLE);
            monitor.setGmtCreate(null);
            monitor.setGmtUpdate(null);
            monitorDao.save(monitor);
            paramDao.saveAll(params);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // 刷库异常取消之前的下发任务
            jobScheduling.cancelAsyncCollectJob(jobId);
            throw new MonitorDatabaseException(e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validate(MonitorDto monitorDto, boolean isModify) throws IllegalArgumentException {

    }

    @Override
    public void modifyMonitor(Monitor monitor, List<Param> params) throws RuntimeException {
        long monitorId = monitor.getId();
        // 查判断monitorId对应的此监控是否存在
        Optional<Monitor> queryOption = monitorDao.findById(monitorId);
        if (!queryOption.isPresent()) {
            throw new IllegalArgumentException("The Monitor " + monitorId + " not exists");
        }
        Monitor preMonitor = queryOption.get();
        if (!preMonitor.getApp().equals(monitor.getApp()) || !preMonitor.getHost().equals(monitor.getHost())) {
            // 监控的 类型和host不能修改
            throw new IllegalArgumentException("Can not modify monitor's app or host");
        }
        // 构造采集任务Job实体
        Job appDefine = appService.getAppDefine(monitor.getApp());
        appDefine.setId(preMonitor.getJobId());
        appDefine.setMonitorId(monitorId);
        appDefine.setInterval(monitor.getIntervals());
        appDefine.setCyclic(true);
        appDefine.setTimestamp(System.currentTimeMillis());
        List<Configmap> configmaps = params.stream().map(param -> {
            param.setMonitorId(monitorId);
            param.setGmtCreate(null);
            param.setGmtUpdate(null);
            return new Configmap(param.getField(), param.getValue(), param.getType());
        }).collect(Collectors.toList());
        appDefine.setConfigmap(configmaps);
        // 更新采集任务
        jobScheduling.updateAsyncCollectJob(appDefine);
        // 下发更新成功后刷库
        try {
            monitor.setJobId(preMonitor.getJobId());
            monitor.setStatus(preMonitor.getStatus());
            monitor.setGmtCreate(null);
            monitor.setGmtUpdate(null);
            monitorDao.save(monitor);
            paramDao.saveAll(params);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MonitorDatabaseException(e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMonitor(long id) throws RuntimeException {
        Optional<Monitor> monitorOptional = monitorDao.findById(id);
        if (monitorOptional.isPresent()) {
            Monitor monitor = monitorOptional.get();
            monitorDao.deleteById(id);
            paramDao.deleteParamsByMonitorId(id);
            jobScheduling.cancelAsyncCollectJob(monitor.getJobId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MonitorDto getMonitor(long id) throws RuntimeException {
        Optional<Monitor> monitorOptional = monitorDao.findById(id);
        if (monitorOptional.isPresent()) {
            MonitorDto monitorDto = new MonitorDto();
            monitorDto.setMonitor(monitorOptional.get());
            List<Param> params = paramDao.findParamsByMonitorId(id);
            monitorDto.setParams(params);
            return monitorDto;
        } else {
            return null;
        }
    }
}
