package com.school.log;

import com.school.common.api.ApiResponse;
import com.school.common.api.PageResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> logs(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "operationType", required = false) String operationType,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        List<Map<String, Object>> records = operationLogService.logs(keyword, operationType, page, size);
        long total = operationLogService.count(keyword, operationType);
        return ApiResponse.success(new PageResponse<>(total, records));
    }

    @GetMapping("/types")
    public ApiResponse<List<String>> operationTypes() {
        return ApiResponse.success(operationLogService.operationTypes());
    }
}
