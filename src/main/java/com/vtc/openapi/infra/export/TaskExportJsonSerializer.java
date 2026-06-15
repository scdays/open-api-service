package com.vtc.openapi.infra.export;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskExportJsonSerializer {

    public byte[] serialize(Map<String, Object> taskExportRoot) {
        String json = JSON.toJSONString(taskExportRoot, SerializerFeature.WriteMapNullValue);
        return json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
