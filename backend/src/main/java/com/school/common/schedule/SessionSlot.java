package com.school.common.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一周内的一次上课时间（一个连续节次块）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionSlot {

    /** 星期几（1-7）。 */
    private Integer weekDay;

    /** 节次描述，如 "1-2节"。 */
    private String sectionDesc;

    /** 周次类型：全周 / 单周 / 双周。 */
    private String weekParity;

    /** 同教学班内排序。 */
    private Integer sortOrder;

    public SessionSlot(Integer weekDay, String sectionDesc, String weekParity) {
        this(weekDay, sectionDesc, weekParity, 1);
    }
}
