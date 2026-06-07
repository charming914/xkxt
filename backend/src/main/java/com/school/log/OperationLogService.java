package com.school.log;

import com.school.log.mapper.OperationLogMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 记录一条操作日志。日志记录失败不应影响主业务，调用方需自行兜底。
     */
    public void record(String username, String operationType, String operationContent) {
        Integer userId = null;
        if (username != null && !username.isBlank()) {
            userId = operationLogMapper.selectUserIdByUsername(username);
        }
        operationLogMapper.insertLog(userId, operationType, operationContent);
    }

    public List<Map<String, Object>> logs(String keyword, String operationType, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;
        return operationLogMapper.selectLogs(keyword, operationType, offset, safeSize);
    }

    public long count(String keyword, String operationType) {
        return operationLogMapper.countLogs(keyword, operationType);
    }

    public List<String> operationTypes() {
        return operationLogMapper.selectOperationTypes();
    }
}
