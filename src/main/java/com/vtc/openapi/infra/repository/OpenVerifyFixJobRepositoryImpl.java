package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.infra.dao.OpenVerifyFixJobItemMapper;
import com.vtc.openapi.infra.dao.OpenVerifyFixJobMapper;
import com.vtc.openapi.infra.dao.po.OpenVerifyFixJobItemPO;
import com.vtc.openapi.infra.dao.po.OpenVerifyFixJobPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
public class OpenVerifyFixJobRepositoryImpl implements IOpenVerifyFixJobRepository {

    private final OpenVerifyFixJobMapper jobMapper;
    private final OpenVerifyFixJobItemMapper itemMapper;

    public OpenVerifyFixJobRepositoryImpl(OpenVerifyFixJobMapper jobMapper,
                                          OpenVerifyFixJobItemMapper itemMapper) {
        this.jobMapper = jobMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public void saveJob(OpenVerifyFixJobDO job) {
        OpenVerifyFixJobPO po = ConvertHelper.convert(job, OpenVerifyFixJobPO.class);
        jobMapper.insert(po);
        job.setId(po.getId());
    }

    @Override
    public void updateJob(OpenVerifyFixJobDO job) {
        OpenVerifyFixJobPO po = ConvertHelper.convert(job, OpenVerifyFixJobPO.class);
        jobMapper.updateById(po);
    }

    @Override
    public OpenVerifyFixJobDO findByJobId(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            return null;
        }
        OpenVerifyFixJobPO po = jobMapper.selectOne(new LambdaQueryWrapper<OpenVerifyFixJobPO>()
                .eq(OpenVerifyFixJobPO::getJobId, jobId.trim()));
        return ConvertHelper.convert(po, OpenVerifyFixJobDO.class);
    }

    @Override
    public List<OpenVerifyFixJobDO> listByPartner(String partnerId, String status, int limit) {
        if (!StringUtils.hasText(partnerId)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<OpenVerifyFixJobPO> wrapper = new LambdaQueryWrapper<OpenVerifyFixJobPO>()
                .eq(OpenVerifyFixJobPO::getPartnerId, partnerId)
                .orderByDesc(OpenVerifyFixJobPO::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(OpenVerifyFixJobPO::getStatus, status.trim());
        }
        wrapper.last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        return ConvertHelper.convertList(jobMapper.selectList(wrapper), OpenVerifyFixJobDO.class);
    }

    @Override
    public List<OpenVerifyFixJobDO> listRecent(String status, int limit) {
        LambdaQueryWrapper<OpenVerifyFixJobPO> wrapper = new LambdaQueryWrapper<OpenVerifyFixJobPO>()
                .orderByDesc(OpenVerifyFixJobPO::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(OpenVerifyFixJobPO::getStatus, status.trim());
        }
        wrapper.last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        return ConvertHelper.convertList(jobMapper.selectList(wrapper), OpenVerifyFixJobDO.class);
    }

    @Override
    public void saveItems(List<OpenVerifyFixJobItemDO> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        for (OpenVerifyFixJobItemDO item : items) {
            OpenVerifyFixJobItemPO po = ConvertHelper.convert(item, OpenVerifyFixJobItemPO.class);
            itemMapper.insert(po);
            item.setId(po.getId());
        }
    }

    @Override
    public void updateItem(OpenVerifyFixJobItemDO item) {
        OpenVerifyFixJobItemPO po = ConvertHelper.convert(item, OpenVerifyFixJobItemPO.class);
        itemMapper.updateById(po);
    }

    @Override
    public List<OpenVerifyFixJobItemDO> listItemsByJobId(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            return new ArrayList<>();
        }
        List<OpenVerifyFixJobItemPO> rows = itemMapper.selectList(new LambdaQueryWrapper<OpenVerifyFixJobItemPO>()
                .eq(OpenVerifyFixJobItemPO::getJobId, jobId.trim())
                .orderByAsc(OpenVerifyFixJobItemPO::getId));
        return ConvertHelper.convertList(rows, OpenVerifyFixJobItemDO.class);
    }

    @Override
    public OpenVerifyFixJobItemDO findLatestPendingItemByPartnerAndVulInfoId(String partnerId, String vulInfoId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(vulInfoId)) {
            return null;
        }
        List<OpenVerifyFixJobItemPO> rows = itemMapper.selectList(new LambdaQueryWrapper<OpenVerifyFixJobItemPO>()
                .eq(OpenVerifyFixJobItemPO::getPartnerId, partnerId.trim())
                .eq(OpenVerifyFixJobItemPO::getVulInfoId, vulInfoId.trim())
                .orderByDesc(OpenVerifyFixJobItemPO::getId)
                .last("LIMIT 8"));
        for (OpenVerifyFixJobItemPO row : rows) {
            OpenVerifyFixJobPO job = jobMapper.selectOne(new LambdaQueryWrapper<OpenVerifyFixJobPO>()
                    .eq(OpenVerifyFixJobPO::getJobId, row.getJobId()));
            if (job == null) {
                continue;
            }
            if ("PENDING".equals(job.getStatus()) && "PENDING".equals(row.getItemStatus())) {
                return ConvertHelper.convert(row, OpenVerifyFixJobItemDO.class);
            }
        }
        return null;
    }

    @Override
    public List<OpenVerifyFixJobDO> listActiveVtcJobs(int limit) {
        int cap = limit > 0 ? limit : 50;
        LambdaQueryWrapper<OpenVerifyFixJobPO> wrapper = new LambdaQueryWrapper<OpenVerifyFixJobPO>()
                .isNotNull(OpenVerifyFixJobPO::getCenterPlanId)
                .in(OpenVerifyFixJobPO::getStatus, "PENDING", "RUNNING")
                .orderByAsc(OpenVerifyFixJobPO::getUpdatedAt)
                .last("LIMIT " + cap);
        return ConvertHelper.convertList(jobMapper.selectList(wrapper), OpenVerifyFixJobDO.class);
    }

    @Override
    public List<OpenVerifyFixJobDO> listDispatchFailedJobs(int limit) {
        int cap = limit > 0 ? limit : 50;
        LambdaQueryWrapper<OpenVerifyFixJobPO> wrapper = new LambdaQueryWrapper<OpenVerifyFixJobPO>()
                .eq(OpenVerifyFixJobPO::getStatus, "DISPATCH_FAILED")
                .isNull(OpenVerifyFixJobPO::getCenterPlanId)
                .orderByAsc(OpenVerifyFixJobPO::getUpdatedAt)
                .last("LIMIT " + cap);
        return ConvertHelper.convertList(jobMapper.selectList(wrapper), OpenVerifyFixJobDO.class);
    }

    @Override
    public OpenVerifyFixJobDO findByCenterSubId(String centerSubId) {
        if (!StringUtils.hasText(centerSubId)) {
            return null;
        }
        OpenVerifyFixJobPO po = jobMapper.selectOne(new LambdaQueryWrapper<OpenVerifyFixJobPO>()
                .eq(OpenVerifyFixJobPO::getCenterSubId, centerSubId.trim())
                .orderByDesc(OpenVerifyFixJobPO::getId)
                .last("LIMIT 1"));
        return ConvertHelper.convert(po, OpenVerifyFixJobDO.class);
    }

    @Override
    public void updateCaseId(String jobId, String caseId) {
        if (!StringUtils.hasText(jobId)) {
            return;
        }
        jobMapper.update(null, new LambdaUpdateWrapper<OpenVerifyFixJobPO>()
                .eq(OpenVerifyFixJobPO::getJobId, jobId.trim())
                .set(OpenVerifyFixJobPO::getCaseId, StringUtils.hasText(caseId) ? caseId.trim() : null));
    }

    @Override
    public List<OpenVerifyFixJobDO> listWithoutCaseId(int limit) {
        int cap = limit > 0 ? Math.min(limit, 500) : 200;
        LambdaQueryWrapper<OpenVerifyFixJobPO> wrapper = new LambdaQueryWrapper<OpenVerifyFixJobPO>()
                .and(w -> w.isNull(OpenVerifyFixJobPO::getCaseId).or().eq(OpenVerifyFixJobPO::getCaseId, ""))
                .orderByDesc(OpenVerifyFixJobPO::getCreatedAt)
                .last("LIMIT " + cap);
        return ConvertHelper.convertList(jobMapper.selectList(wrapper), OpenVerifyFixJobDO.class);
    }
}
