package com.school.common.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 按课程学分自动排课：每节课=2学时。
 *   偶数学分 -> 学分/2 节全周课；
 *   奇数学分 -> floor(学分/2) 节全周课 + 1 节单/双周隔周课（平均补足半个学时）。
 * 排课时避开同教师/同教室已占用的时段（单双周相交才算冲突）。
 */
public final class SessionScheduleGenerator {

    public static final String FULL = "全周";
    public static final String ODD = "单周";
    public static final String EVEN = "双周";

    private static final int[] WEEK_DAYS = {1, 2, 3, 4, 5};
    private static final String[] SECTIONS = {"1-2节", "3-4节", "5-6节", "7-8节"};

    private SessionScheduleGenerator() {
    }

    /**
     * 根据学分计算需要的全周节数与是否含半节。
     */
    public static int fullSessionCount(double credit) {
        int rounded = (int) Math.round(credit);
        int full = rounded / 2;
        if (full == 0 && rounded % 2 == 0) {
            return 1; // 学分异常时至少排一节
        }
        return full;
    }

    public static boolean hasHalfSession(double credit) {
        return Math.round(credit) % 2 == 1;
    }

    /**
     * 生成上课时间列表。
     *
     * @param credit       课程学分
     * @param seed         用于确定性分配的种子（如教学班编号），保证可复现
     * @param occupied     同学期同教师/同教室已占用的时段
     */
    public static List<SessionSlot> generate(double credit, int seed, List<SessionSlot> occupied) {
        List<SessionSlot> result = new ArrayList<>();
        List<SessionSlot> taken = new ArrayList<>(occupied == null ? List.of() : occupied);

        int full = fullSessionCount(credit);
        boolean half = hasHalfSession(credit);
        int total = full + (half ? 1 : 0);

        for (int made = 0; made < total; made++) {
            String parity = made < full ? FULL : (Math.floorMod(seed, 2) == 1 ? ODD : EVEN);
            SessionSlot slot = pickSlot(seed, made, parity, taken);
            slot.setSortOrder(made + 1);
            result.add(slot);
            taken.add(slot);
        }
        return result;
    }

    private static SessionSlot pickSlot(int seed, int made, String parity, List<SessionSlot> taken) {
        for (int attempt = 0; attempt < 60; attempt++) {
            int weekDay = WEEK_DAYS[Math.floorMod(seed + made + attempt, WEEK_DAYS.length)];
            String section = SECTIONS[Math.floorMod(seed + made * 2 + attempt, SECTIONS.length)];
            if (!conflicts(weekDay, section, parity, taken)) {
                return new SessionSlot(weekDay, section, parity);
            }
        }
        // 兜底：取首个时段，避免死循环（极端拥挤时允许冲突，后续可人工调整）
        return new SessionSlot(WEEK_DAYS[0], SECTIONS[0], parity);
    }

    private static boolean conflicts(int weekDay, String section, String parity, List<SessionSlot> taken) {
        for (SessionSlot s : taken) {
            if (!Integer.valueOf(weekDay).equals(s.getWeekDay())) {
                continue;
            }
            if (!sectionsOverlap(section, s.getSectionDesc())) {
                continue;
            }
            if (paritiesIntersect(parity, s.getWeekParity())) {
                return true;
            }
        }
        return false;
    }

    /** 单双周相交：全周与任意相交，单周仅与单周/全周，双周仅与双周/全周。 */
    public static boolean paritiesIntersect(String a, String b) {
        if (a == null || b == null) {
            return true;
        }
        return FULL.equals(a) || FULL.equals(b) || a.equals(b);
    }

    /** 节次区间是否重叠，如 "1-2节" 与 "1-4节"。 */
    public static boolean sectionsOverlap(String a, String b) {
        int[] ra = parseSection(a);
        int[] rb = parseSection(b);
        return ra[0] <= rb[1] && rb[0] <= ra[1];
    }

    private static int[] parseSection(String section) {
        if (section == null) {
            return new int[]{0, 0};
        }
        String normalized = section.replace("节", "");
        String[] parts = normalized.split("-");
        try {
            int start = Integer.parseInt(parts[0].trim());
            int end = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : start;
            return new int[]{start, end};
        } catch (NumberFormatException ex) {
            return new int[]{0, 0};
        }
    }

    /**
     * 把 mapper 查到的占用时段 Map 列表转换为 SessionSlot。
     */
    public static SessionSlot fromMap(Map<String, Object> row) {
        Object day = row.get("weekDay");
        Object section = row.get("sectionDesc");
        Object parity = row.get("weekParity");
        Integer weekDay = day == null ? null : ((Number) day).intValue();
        return new SessionSlot(
            weekDay,
            section == null ? null : section.toString(),
            parity == null ? FULL : parity.toString()
        );
    }
}
