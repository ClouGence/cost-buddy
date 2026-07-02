package com.costbuddy.controller;

import com.costbuddy.common.api.ApiResponse;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.request.CloudAccountRequest;
import com.costbuddy.service.CloudAccountService;
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
@RequestMapping("/api/cloud-accounts")
public class CloudAccountController {

    private final CloudAccountService cloudAccountService;

    public CloudAccountController(CloudAccountService cloudAccountService) {
        this.cloudAccountService = cloudAccountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CloudAccountDO> create(@Valid @RequestBody CloudAccountRequest request) {
        return ApiResponse.ok(cloudAccountService.create(request));
    }

    @GetMapping
    public ApiResponse<List<CloudAccountDO>> list() {
        return ApiResponse.ok(cloudAccountService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<CloudAccountDO> update(@PathVariable Long id, @Valid @RequestBody CloudAccountRequest request) {
        return ApiResponse.ok(cloudAccountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        cloudAccountService.delete(id);
        return ApiResponse.ok(null);
    }
}
