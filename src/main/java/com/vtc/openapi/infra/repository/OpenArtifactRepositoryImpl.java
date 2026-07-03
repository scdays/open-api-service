package com.vtc.openapi.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.artifact.model.ArtifactWebhookDeliveryStatus;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.infra.dao.OpenArtifactMapper;
import com.vtc.openapi.infra.dao.po.OpenArtifactPO;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class OpenArtifactRepositoryImpl implements IOpenArtifactRepository {

    private final OpenArtifactMapper artifactMapper;

    public OpenArtifactRepositoryImpl(OpenArtifactMapper artifactMapper) {
        this.artifactMapper = artifactMapper;
    }

    @Override
    public OpenArtifactDO findByArtifactId(String artifactId) {
        if (!StringUtils.hasText(artifactId)) {
            return null;
        }
        OpenArtifactPO po = artifactMapper.selectOne(new LambdaQueryWrapper<OpenArtifactPO>()
                .eq(OpenArtifactPO::getArtifactId, artifactId));
        return ConvertHelper.convert(po, OpenArtifactDO.class);
    }

    @Override
    public OpenArtifactDO findByPartnerAndArtifactId(String partnerId, String artifactId) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(artifactId)) {
            return null;
        }
        OpenArtifactPO po = artifactMapper.selectOne(new LambdaQueryWrapper<OpenArtifactPO>()
                .eq(OpenArtifactPO::getPartnerId, partnerId)
                .eq(OpenArtifactPO::getArtifactId, artifactId));
        return ConvertHelper.convert(po, OpenArtifactDO.class);
    }

    @Override
    public OpenArtifactDO findBySubTaskAndSource(String partnerId, String subTaskId, String artifactSource) {
        if (!StringUtils.hasText(subTaskId) || !StringUtils.hasText(artifactSource)) {
            return null;
        }
        LambdaQueryWrapper<OpenArtifactPO> wrapper = new LambdaQueryWrapper<OpenArtifactPO>()
                .eq(OpenArtifactPO::getSubTaskId, subTaskId)
                .eq(OpenArtifactPO::getArtifactSource, artifactSource);
        if (StringUtils.hasText(partnerId)) {
            wrapper.eq(OpenArtifactPO::getPartnerId, partnerId);
        }
        OpenArtifactPO po = artifactMapper.selectOne(wrapper);
        return ConvertHelper.convert(po, OpenArtifactDO.class);
    }

    @Override
    public void saveArtifact(OpenArtifactDO artifact) {
        artifactMapper.insert(ConvertHelper.convert(artifact, OpenArtifactPO.class));
    }

    @Override
    public void updateArtifact(OpenArtifactDO artifact) {
        OpenArtifactPO po = ConvertHelper.convert(artifact, OpenArtifactPO.class);
        artifactMapper.updateById(po);
        if (artifact.getId() != null && !StringUtils.hasText(artifact.getErrorMessage())) {
            artifactMapper.update(null, new LambdaUpdateWrapper<OpenArtifactPO>()
                    .eq(OpenArtifactPO::getId, artifact.getId())
                    .set(OpenArtifactPO::getErrorMessage, null));
        }
    }

    @Override
    public PageInfo<OpenArtifactDO> pageByTask(String partnerId, String taskId, String exportStage,
                                               String artifactSource, int page, int size) {
        LambdaQueryWrapper<OpenArtifactPO> wrapper = new LambdaQueryWrapper<OpenArtifactPO>()
                .eq(OpenArtifactPO::getPartnerId, partnerId)
                .eq(OpenArtifactPO::getTaskId, taskId)
                .orderByDesc(OpenArtifactPO::getCreatedAt);
        if (StringUtils.hasText(exportStage)) {
            wrapper.eq(OpenArtifactPO::getExportStage, exportStage);
        }
        if (StringUtils.hasText(artifactSource)) {
            wrapper.eq(OpenArtifactPO::getArtifactSource, artifactSource);
        }
        return toPageInfo(artifactMapper.selectPage(new Page<>(page, size), wrapper), page, size);
    }

    @Override
    public PageInfo<OpenArtifactDO> pageByTaskAndStage(String partnerId, String taskId, String exportStage,
                                                       int page, int size) {
        LambdaQueryWrapper<OpenArtifactPO> wrapper = new LambdaQueryWrapper<OpenArtifactPO>()
                .eq(OpenArtifactPO::getPartnerId, partnerId)
                .eq(OpenArtifactPO::getTaskId, taskId)
                .eq(OpenArtifactPO::getExportStage, exportStage)
                .orderByDesc(OpenArtifactPO::getCreatedAt);
        return toPageInfo(artifactMapper.selectPage(new Page<>(page, size), wrapper), page, size);
    }

    @Override
    public List<OpenArtifactDO> listPendingWebhookDelivery(String partnerId, String taskId, String exportStage,
                                                           String verifyFixJobId, int limit) {
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(taskId) || !StringUtils.hasText(exportStage)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<OpenArtifactPO> wrapper = new LambdaQueryWrapper<OpenArtifactPO>()
                .eq(OpenArtifactPO::getPartnerId, partnerId)
                .eq(OpenArtifactPO::getTaskId, taskId)
                .eq(OpenArtifactPO::getExportStage, exportStage)
                .eq(OpenArtifactPO::getWebhookDeliveryStatus, ArtifactWebhookDeliveryStatus.PENDING)
                .orderByAsc(OpenArtifactPO::getCreatedAt)
                .last("LIMIT " + Math.max(limit, 1));
        if (StringUtils.hasText(verifyFixJobId)) {
            wrapper.eq(OpenArtifactPO::getVerifyFixJobId, verifyFixJobId.trim());
        }
        return artifactMapper.selectList(wrapper).stream()
                .map(po -> ConvertHelper.convert(po, OpenArtifactDO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<OpenArtifactDO> listAllPendingWebhookDelivery(int limit) {
        LambdaQueryWrapper<OpenArtifactPO> wrapper = new LambdaQueryWrapper<OpenArtifactPO>()
                .eq(OpenArtifactPO::getWebhookDeliveryStatus, ArtifactWebhookDeliveryStatus.PENDING)
                .orderByAsc(OpenArtifactPO::getCreatedAt)
                .last("LIMIT " + Math.max(limit, 1));
        return artifactMapper.selectList(wrapper).stream()
                .map(po -> ConvertHelper.convert(po, OpenArtifactDO.class))
                .collect(Collectors.toList());
    }

    private static PageInfo<OpenArtifactDO> toPageInfo(Page<OpenArtifactPO> result, int page, int size) {
        PageInfo<OpenArtifactDO> pageInfo = new PageInfo<>();
        pageInfo.setCurrent(page);
        pageInfo.setSize(size);
        pageInfo.setTotal(result.getTotal());
        pageInfo.setRecords(result.getRecords().stream()
                .map(po -> ConvertHelper.convert(po, OpenArtifactDO.class))
                .collect(Collectors.toList()));
        return pageInfo;
    }
}
