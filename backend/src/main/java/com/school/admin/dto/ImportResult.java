package com.school.admin.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 批量导入结果：成功条数、失败条数与逐行错误信息。
 */
@Data
public class ImportResult {

    private int total;
    private int success;
    private int failed;
    private final List<String> errors = new ArrayList<>();

    public void addError(int rowNumber, String message) {
        errors.add("第" + rowNumber + "行：" + message);
        failed++;
    }

    public void markSuccess() {
        success++;
    }
}
