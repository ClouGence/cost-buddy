package com.costbuddy.service;

import com.costbuddy.ai.AiChatClient;
import com.costbuddy.common.exception.BusinessException;
import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.AiEngineDO;
import com.costbuddy.dto.request.AiEngineRequest;
import com.costbuddy.dto.response.AiEngineCheckResponse;
import com.costbuddy.mapper.AiEngineMapper;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiEngineService {

    private static final String RESOURCE_NAME = "ai_engine";

    private final AiEngineMapper aiEngineMapper;
    private final AiChatClient   aiChatClient;

    public AiEngineService(AiEngineMapper aiEngineMapper, AiChatClient aiChatClient) {
        this.aiEngineMapper = aiEngineMapper;
        this.aiChatClient = aiChatClient;
    }

    @Transactional
    public AiEngineDO create(AiEngineRequest request) {
        if (isBlank(request.getApiKey())) {
            throw new BusinessException("AI_ENGINE_API_KEY_REQUIRED", "AI engine API key is required");
        }
        AiEngineDO aiEngine = new AiEngineDO();
        BeanUtils.copyProperties(request, aiEngine);
        aiEngineMapper.insert(aiEngine);
        return get(aiEngine.getId());
    }

    public AiEngineDO get(Long id) {
        AiEngineDO aiEngine = aiEngineMapper.selectById(id);
        if (aiEngine == null) {
            throw new NotFoundException(RESOURCE_NAME, id);
        }
        return aiEngine;
    }

    public List<AiEngineDO> list() {
        return aiEngineMapper.selectAll();
    }

    @Transactional
    public AiEngineDO update(Long id, AiEngineRequest request) {
        AiEngineDO existing = get(id);
        AiEngineDO aiEngine = new AiEngineDO();
        BeanUtils.copyProperties(request, aiEngine);
        aiEngine.setId(id);
        if (isBlank(request.getApiKey())) {
            aiEngine.setApiKey(existing.getApiKey());
        }
        aiEngineMapper.update(aiEngine);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        aiEngineMapper.deleteById(id);
    }

    public AiEngineCheckResponse check(Long id) {
        AiEngineDO aiEngine = get(id);
        try {
            String reply = aiChatClient.chat(aiEngine, "You are a health check endpoint. Reply with OK only.", "OK");
            if (reply == null || reply.isBlank()) {
                return new AiEngineCheckResponse(false, "AI engine response is empty");
            }
            return new AiEngineCheckResponse(true, "AI engine is available");
        } catch (RuntimeException exception) {
            return new AiEngineCheckResponse(false, exception.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
