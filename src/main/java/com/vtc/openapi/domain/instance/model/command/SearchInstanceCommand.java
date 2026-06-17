package com.vtc.openapi.domain.instance.model.command;

import java.util.List;
import lombok.Data;

/**
 * 实例搜索命令。
 */
@Data
public class SearchInstanceCommand {
    private String taskId;
    private String extTaskId;
    private List<Integer> vulInfoStatList;
    private List<Integer> vulLevelList;
    private String vulNetAddr;
    private String assetName;
    private String vulName;
    private String orgVulId;
    private String vulId;
    private Boolean isAccess;
    private String unitType;
    private Integer page;
    private Integer size;
    private String exportProfile;
}