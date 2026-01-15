package org.example.campus_performance_ticketing.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.campus_performance_ticketing.logic.dto.PendingApplicationDto;
import org.example.campus_performance_ticketing.model.Application;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;

@Component
public class JsonHelper {

    // 工具：解析extraData为展示字段，可以按需扩展
    public void parseDisplayDtoFields(Application app, PendingApplicationDto dto, ObjectMapper objectMapper) {
        try {
            JsonNode node = objectMapper.readTree(app.getExtraData());
            if ("CREATE_ORG".equals(app.getApplicationType())) {
                dto.setDisplayTitle(node.path("orgName").asText());
                dto.setDisplayDescription(node.path("orgDescription").asText());
            } else if ("JOIN_ORG".equals(app.getApplicationType())) {
                dto.setDisplayTitle(node.path("joinOrgName").asText());
                dto.setDisplayDescription(node.path("joinMessage").asText());
            }
            // 你可以按不同type处理不同展示
        } catch (Exception ignore) {}
    }

    // extraData工具
    public String addReasonToJson(String oldJson, String key, String reason) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode oldNode = StringUtils.hasText(oldJson) ? mapper.readTree(oldJson) : mapper.createObjectNode();
            ((com.fasterxml.jackson.databind.node.ObjectNode) oldNode).put(key, reason);
            return mapper.writeValueAsString(oldNode);
        } catch (Exception e) {
            return "{\"" + key + "\":\"" + reason.replace("\"","\\\"") + "\"}";
        }
    }

}
