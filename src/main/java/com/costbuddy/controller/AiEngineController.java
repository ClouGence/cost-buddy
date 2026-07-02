package com.costbuddy.controller;

import com.costbuddy.common.api.ApiResponse;
import com.costbuddy.domain.AiEngineDO;
import com.costbuddy.dto.request.AiEngineRequest;
import com.costbuddy.dto.response.AiEngineCheckResponse;
import com.costbuddy.service.AiEngineService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-engines")
public class AiEngineController {

    private final AiEngineService aiEngineService;

    public AiEngineController(AiEngineService aiEngineService) {
        this.aiEngineService = aiEngineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AiEngineDO> create(@Valid @RequestBody AiEngineRequest request) {
        return ApiResponse.ok(aiEngineService.create(request));
    }

    @GetMapping
    public ApiResponse<List<AiEngineDO>> list() {
        return ApiResponse.ok(aiEngineService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<AiEngineDO> update(@PathVariable Long id, @Valid @RequestBody AiEngineRequest request) {
        return ApiResponse.ok(aiEngineService.update(id, request));
    }

    @PostMapping("/{id}/check")
    public ApiResponse<AiEngineCheckResponse> check(@PathVariable Long id) {
        return ApiResponse.ok(aiEngineService.check(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        aiEngineService.delete(id);
        return ApiResponse.ok(null);
    }
}
