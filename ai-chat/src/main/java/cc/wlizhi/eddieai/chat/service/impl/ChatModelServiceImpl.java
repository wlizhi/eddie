package cc.wlizhi.eddieai.chat.service.impl;

import cc.wlizhi.eddieai.chat.entity.response.ChatModelItemVO;
import cc.wlizhi.eddieai.chat.entity.response.ChatModelSelectorVO;
import cc.wlizhi.eddieai.chat.service.ChatModelService;
import cc.wlizhi.eddieai.common.dao.ChatModelProviderDao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatModelServiceImpl implements ChatModelService {

    @Resource
    private ChatModelProviderDao chatModelProviderDao;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ChatModelSelectorVO> listChatModels() {
        List<ChatModelProviderDao.EnabledProviderModel> entities =
                chatModelProviderDao.findAllEnabledWithModels();

        List<ChatModelSelectorVO> result = new ArrayList<>();

        for (ChatModelProviderDao.EnabledProviderModel entity : entities) {
            String modelsJson = entity.getModels();
            if (modelsJson == null || modelsJson.isEmpty() || "[]".equals(modelsJson)) {
                continue;
            }

            List<ChatModelItemVO> modelItems = parseModels(modelsJson, entity.getId(), entity.getCode());
            if (modelItems.isEmpty()) {
                continue;
            }

            ChatModelSelectorVO vo = new ChatModelSelectorVO();
            vo.setProviderId(entity.getId());
            vo.setProviderCode(entity.getCode());
            vo.setProviderName(entity.getName());
            vo.setModels(modelItems);
            result.add(vo);
        }

        return result;
    }

    private List<ChatModelItemVO> parseModels(String modelsJson, Long providerId, String providerCode) {
        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(
                    modelsJson, new TypeReference<List<Map<String, Object>>>() {
                    });

            return rawList.stream()
                    .map(raw -> {
                        ChatModelItemVO item = new ChatModelItemVO();
                        Object id = raw.get("id");
                        Object ownedBy = raw.get("owned_by");
                        item.setModelId(id != null ? id.toString() : null);
                        item.setDisplayName(id != null ? id.toString() : null);
                        item.setProviderId(providerId);
                        item.setProviderCode(ownedBy != null ? ownedBy.toString() : providerCode);
                        return item;
                    })
                    .filter(item -> item.getModelId() != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("解析模型列表 JSON 失败: " + e.getMessage(), e);
        }
    }
}
