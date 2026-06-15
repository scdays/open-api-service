package com.vtc.openapi.infra.adapter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.infra.adapter.mock.MockEngineBundle;
import com.vtc.openapi.infra.adapter.mock.MockEngineFixtureLoader;
import com.vtc.openapi.infra.adapter.mock.MockFixtureResolver;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class VulnInstanceGatewayMockImpl implements IVulnInstanceGateway {

    private static final Logger log = LoggerFactory.getLogger(VulnInstanceGatewayMockImpl.class);

    private final MockEngineFixtureLoader fixtureLoader;
    private final MockFixtureResolver fixtureResolver;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;

    public VulnInstanceGatewayMockImpl(MockEngineFixtureLoader fixtureLoader,
                                       MockFixtureResolver fixtureResolver,
                                       IOpenVulnInstanceRepository vulnInstanceRepository) {
        this.fixtureLoader = fixtureLoader;
        this.fixtureResolver = fixtureResolver;
        this.vulnInstanceRepository = vulnInstanceRepository;
        log.info("VulnInstanceGateway: MOCK mode (db-backed instances when ingested)");
    }

    @Override
    public InstancePageResult searchInstances(SearchInstanceCommand command) {
        String partnerId = PartnerContext.requirePartnerId();
        if (hasPersistedInstances(partnerId, command)) {
            return vulnInstanceRepository.searchFromDb(partnerId, command);
        }
        return searchFromFixture(command);
    }

    @Override
    public InstanceItemResult findByVulInfoId(String vulInfoId) {
        if (!StringUtils.hasText(vulInfoId)) {
            return null;
        }
        String partnerId = PartnerContext.getPartnerId();
        if (StringUtils.hasText(partnerId)) {
            InstanceItemResult fromDb = InstanceItemConverter.fromSnapshot(
                    vulnInstanceRepository.findByPartnerAndVulInfoId(partnerId, vulInfoId));
            if (fromDb != null) {
                return fromDb;
            }
        }
        return findInFixture(vulInfoId);
    }

    @Override
    public void updateInstance(Long id, int vulInfoStat, String method, String remedDesc) {
        String partnerId = PartnerContext.requirePartnerId();
        if (id != null && vulnInstanceRepository.findByIdAndPartner(id, partnerId) != null) {
            vulnInstanceRepository.updateState(id, partnerId, vulInfoStat, method, remedDesc);
            log.info("MOCK updateInstance persisted: id={}, vulInfoStat={}", id, vulInfoStat);
            return;
        }
        log.warn("MOCK updateInstance: no persisted instance for id={}", id);
    }

    private boolean hasPersistedInstances(String partnerId, SearchInstanceCommand command) {
        if (!StringUtils.hasText(partnerId)) {
            return false;
        }
        if (StringUtils.hasText(command.getTaskId())
                && vulnInstanceRepository.countByPartnerAndTaskId(partnerId, command.getTaskId()) > 0) {
            return true;
        }
        if (StringUtils.hasText(command.getExtTaskId())
                || StringUtils.hasText(command.getTaskId())) {
            List<?> rows = vulnInstanceRepository.listByPartnerAndTask(
                    partnerId, command.getTaskId(), command.getExtTaskId());
            return !CollectionUtils.isEmpty(rows);
        }
        return false;
    }

    private InstancePageResult searchFromFixture(SearchInstanceCommand command) {
        MockEngineBundle bundle = fixtureResolver.resolve(command.getExtTaskId(), command.getTaskId(), null);
        InstancePageResult result = new InstancePageResult();
        List<InstanceItemResult> allItems = new ArrayList<>();
        if (bundle != null && bundle.getInstances() != null) {
            List<JSONObject> filtered = filterInstances(bundle.getInstances(), command);
            for (JSONObject inst : filtered) {
                allItems.add(InstanceItemConverter.fromJson(inst));
            }
        }
        int page = Math.max(1, command.getPage() != null ? command.getPage() : 1);
        int size = Math.min(1000, Math.max(1, command.getSize() != null ? command.getSize() : 20));
        int from = (page - 1) * size;
        int to = Math.min(from + size, allItems.size());
        result.setItems(from < allItems.size()
                ? new ArrayList<>(allItems.subList(from, to)) : new ArrayList<>());
        result.setTotal((long) allItems.size());
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    private InstanceItemResult findInFixture(String vulInfoId) {
        for (MockEngineBundle bundle : fixtureLoader.listAllBundles()) {
            if (bundle.getInstances() == null) {
                continue;
            }
            for (JSONObject inst : bundle.getInstances()) {
                if (matchesVulInfoId(inst, vulInfoId)) {
                    return InstanceItemConverter.fromJson(inst);
                }
            }
        }
        return null;
    }

    private List<JSONObject> filterInstances(List<JSONObject> source, SearchInstanceCommand command) {
        List<JSONObject> out = new ArrayList<>();
        for (JSONObject inst : source) {
            if (command.getVulInfoStatList() != null && !command.getVulInfoStatList().isEmpty()) {
                if (!command.getVulInfoStatList().contains(inst.getInteger("vulInfoStat"))) {
                    continue;
                }
            }
            if (command.getVulLevelList() != null && !command.getVulLevelList().isEmpty()) {
                Integer level = inst.getInteger("vulLevel");
                boolean levelMatch = false;
                if (level != null) {
                    for (String lv : command.getVulLevelList()) {
                        if (Objects.equals(String.valueOf(level), lv)) {
                            levelMatch = true;
                            break;
                        }
                    }
                }
                if (!levelMatch) {
                    continue;
                }
            }
            out.add(JSON.parseObject(inst.toJSONString()));
        }
        return out;
    }

    private static boolean matchesVulInfoId(JSONObject inst, String vulInfoId) {
        return Objects.equals(vulInfoId, inst.getString("vulInfoID"))
                || Objects.equals(vulInfoId, inst.getString("vulInfoId"));
    }
}
