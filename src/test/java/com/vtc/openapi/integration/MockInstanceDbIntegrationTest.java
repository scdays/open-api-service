package com.vtc.openapi.integration;

import com.vtc.openapi.ApplicationStart;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IInstanceIngestDomainService;
import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.domain.task.model.command.CreateOpenTaskCommand;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.result.OpenTaskProgressResult;
import com.vtc.openapi.domain.task.model.support.TaskTypeSupport;
import com.vtc.openapi.domain.task.model.vo.ScanTaskTargets;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.service.business.IOpenTaskDomainService;
import com.vtc.openapi.infra.adapter.IVulnInstanceGateway;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskProgressResult;
import com.vtc.openapi.infra.adapter.SvmpEngineAdapter;
import com.vtc.openapi.infra.dao.OpenTaskMapper;
import com.vtc.openapi.infra.dao.OpenVulnInstanceMapper;
import com.vtc.openapi.infra.dao.PartnerTaskMapMapper;
import com.vtc.openapi.infra.dao.po.OpenTaskPO;
import com.vtc.openapi.infra.dao.po.OpenVulnInstancePO;
import com.vtc.openapi.infra.dao.po.PartnerTaskMapPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

/**
 * OP-MOCK-P1-DB integration tests against real MySQL (see application.yml).
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApplicationStart.class)
@ActiveProfiles({"mock", "integration-test"})
public class MockInstanceDbIntegrationTest {

    private static final String PARTNER_ID = "IT-MOCK-P1-DB";
    private static final int PORT_BUNDLE_EXPECTED = 5;

    @Autowired
    private IInstanceIngestDomainService instanceIngestDomainService;

    @Autowired
    private IOpenVulnInstanceRepository vulnInstanceRepository;

    @Autowired
    private IOpenTaskRepository openTaskRepository;

    @Autowired
    private IOpenTaskDomainService openTaskDomainService;

    @Autowired
    private IVulnInstanceGateway vulnInstanceGateway;

    @Autowired
    private SvmpEngineAdapter svmpEngineAdapter;

    @Autowired
    private OpenVulnInstanceMapper openVulnInstanceMapper;

    @Autowired
    private OpenTaskMapper openTaskMapper;

    @Autowired
    private PartnerTaskMapMapper partnerTaskMapMapper;

    private String taskId;
    private String extTaskId;
    private String engineTaskId;

    @Before
    public void setUp() {
        cleanupPartnerData();
        PartnerContext.set(PARTNER_ID, "it-mock-p1-db-" + System.currentTimeMillis());
    }

    @After
    public void tearDown() {
        cleanupPartnerData();
        PartnerContext.clear();
    }

    @Test
    public void ingestFromFixture_persistsInstancesToMysql() {
        OpenTaskDO task = buildFinishedTask(1003, 1);
        openTaskRepository.save(task);

        instanceIngestDomainService.tryIngestOnTaskFinished(task);

        long count = vulnInstanceRepository.countByPartnerAndTaskId(PARTNER_ID, task.getTaskId());
        Assert.assertEquals(PORT_BUNDLE_EXPECTED, count);

        OpenTaskDO reloaded = openTaskRepository.findByTaskId(task.getTaskId());
        Assert.assertTrue(Boolean.TRUE.equals(reloaded.getInstancesIngested()));

        SearchInstanceCommand search = new SearchInstanceCommand();
        search.setTaskId(task.getTaskId());
        search.setPage(1);
        search.setSize(100);
        InstancePageResult page = vulnInstanceRepository.searchFromDb(PARTNER_ID, search);
        Assert.assertEquals(PORT_BUNDLE_EXPECTED, page.getTotal().longValue());
        Assert.assertFalse(page.getItems().isEmpty());
        Assert.assertTrue(page.getItems().get(0).getVulInfoId().startsWith(task.getTaskId() + "-"));
    }

    @Test
    public void ingest_isIdempotent_noDuplicateRows() {
        OpenTaskDO task = buildFinishedTask(1003, 1);
        openTaskRepository.save(task);

        instanceIngestDomainService.tryIngestOnTaskFinished(task);
        instanceIngestDomainService.tryIngestOnTaskFinished(task);

        long count = vulnInstanceRepository.countByPartnerAndTaskId(PARTNER_ID, task.getTaskId());
        Assert.assertEquals(PORT_BUNDLE_EXPECTED, count);
    }

    @Test
    public void gateway_updateInstance_persistsVulInfoStat() {
        OpenTaskDO task = buildFinishedTask(1003, 1);
        openTaskRepository.save(task);
        instanceIngestDomainService.tryIngestOnTaskFinished(task);

        SearchInstanceCommand search = new SearchInstanceCommand();
        search.setTaskId(task.getTaskId());
        search.setPage(1);
        search.setSize(1);
        InstancePageResult page = vulnInstanceGateway.searchInstances(search);
        InstanceItemResult item = page.getItems().get(0);

        vulnInstanceGateway.updateInstance(item.getId(), 2, null, null);

        InstanceItemResult after = vulnInstanceGateway.findByVulInfoId(item.getVulInfoId());
        Assert.assertEquals(Integer.valueOf(2), after.getVulInfoStat());

        OpenVulnInstancePO row = openVulnInstanceMapper.selectOne(
                new LambdaQueryWrapper<OpenVulnInstancePO>()
                        .eq(OpenVulnInstancePO::getPartnerId, PARTNER_ID)
                        .eq(OpenVulnInstancePO::getVulInfoId, item.getVulInfoId()));
        Assert.assertEquals(Integer.valueOf(2), row.getVulInfoStat());
    }

    @Test
    public void createTask_getTaskProgress_triggersIngest_endToEnd() {
        extTaskId = "ext-it-" + UUID.randomUUID().toString().substring(0, 8);
        InvocationContext ctx = new InvocationContext(
                PARTNER_ID, "req-it-e2e", "createTask", "POST", "/api/open/v1/tasks/vul", "127.0.0.1");

        CreateOpenTaskCommand command = new CreateOpenTaskCommand();
        command.setExtTaskId(extTaskId);
        command.setTaskName("it-mock-port-scan");
        command.setType(1);
        command.setScanTemplateId(1003);
        command.setReportTemplateId(2001);
        ScanTaskTargets targets = new ScanTaskTargets();
        targets.setHosts("10.0.0.1");
        command.setTargets(targets);

        openTaskDomainService.create(ctx, command);

        OpenTaskDO created = openTaskRepository.findTaskMap(PARTNER_ID, extTaskId) != null
                ? openTaskRepository.findByTaskId(
                openTaskRepository.findTaskMap(PARTNER_ID, extTaskId).getPlatformTaskId())
                : null;
        Assert.assertNotNull(created);
        taskId = created.getTaskId();
        engineTaskId = created.getEngineTaskId();

        OpenTaskProgressResult progress = openTaskDomainService.get(ctx, taskId);
        Assert.assertEquals("FINISHED", progress.getStatus());

        long count = vulnInstanceRepository.countByPartnerAndTaskId(PARTNER_ID, taskId);
        Assert.assertEquals(PORT_BUNDLE_EXPECTED, count);

        SvmpTaskProgressResult engineProgress = svmpEngineAdapter.getTaskProgress(engineTaskId);
        Assert.assertEquals("FINISHED", engineProgress.getStatus());
    }

    private OpenTaskDO buildFinishedTask(int scanTemplateId, int vulnType) {
        taskId = "TASK-IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        extTaskId = "ext-it-" + UUID.randomUUID().toString().substring(0, 8);
        engineTaskId = "MOCK-ENG-IT-" + System.currentTimeMillis();
        Date now = new Date();

        OpenTaskDO task = new OpenTaskDO();
        task.setTaskId(taskId);
        task.setPartnerId(PARTNER_ID);
        task.setExtTaskId(extTaskId);
        task.setEngineTaskId(engineTaskId);
        task.setTaskName("it-mock-scan");
        task.setTargetType(TaskTypeSupport.resolveTargetType(vulnType));
        task.setVulnType(vulnType);
        task.setTargetsJson("{\"hosts\":\"10.0.0.1\"}");
        task.setStatus("FINISHED");
        task.setProgress(100);
        task.setScanTemplateId(scanTemplateId);
        task.setOptionsJson("{\"reportTemplateId\":2001}");
        task.setStartedAt(now);
        task.setFinishedAt(now);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setInstancesIngested(false);
        return task;
    }

    private void cleanupPartnerData() {
        openVulnInstanceMapper.delete(new LambdaQueryWrapper<OpenVulnInstancePO>()
                .eq(OpenVulnInstancePO::getPartnerId, PARTNER_ID));
        partnerTaskMapMapper.delete(new LambdaQueryWrapper<PartnerTaskMapPO>()
                .eq(PartnerTaskMapPO::getPartnerId, PARTNER_ID));
        openTaskMapper.delete(new LambdaQueryWrapper<OpenTaskPO>()
                .eq(OpenTaskPO::getPartnerId, PARTNER_ID));
    }
}
