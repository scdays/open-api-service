package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateRequest;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateResult;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskProgressResult;

public interface SvmpEngineAdapter {

    SvmpTaskCreateResult createTask(SvmpTaskCreateRequest request);

    SvmpTaskProgressResult getTaskProgress(String engineTaskId);

    // TODO(P0+): 实例查询与生命周期写操作

    /** POST /vuln/disposal/list */
    default Object searchInstances(Object request) {
        throw new UnsupportedOperationException("TODO: searchInstances");
    }

    /** GET /vuln/disposal/detail */
    default Object getInstanceDetail(String vulnDisposalId) {
        throw new UnsupportedOperationException("TODO: getInstanceDetail");
    }

    /** POST /vuln/disposal/disposal */
    default Object disposeInstance(Object request) {
        throw new UnsupportedOperationException("TODO: disposeInstance");
    }

    /** POST /vuln/disposal/verify */
    default Object verifyInstance(Object request) {
        throw new UnsupportedOperationException("TODO: verifyInstance");
    }
}