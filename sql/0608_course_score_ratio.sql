-- 0608 课程平时/考试成绩占比脚本
-- 功能：
-- 1. 为 course 表增加平时成绩占比、考试成绩占比字段（默认 平时:考试 = 4:6，即 40:60）
-- 2. 重写成绩触发器，按课程配置的占比计算总评成绩与绩点（替代原硬编码 0.4/0.6）
-- 3. 按各课程占比重新计算已有成绩记录的总评和绩点

USE school;

-- 1. 课程表新增占比字段（百分比，平时+考试=100）
ALTER TABLE course
    ADD COLUMN usual_ratio TINYINT NOT NULL DEFAULT 40 COMMENT '平时成绩占比(%)' AFTER assessment_method,
    ADD COLUMN exam_ratio  TINYINT NOT NULL DEFAULT 60 COMMENT '考试成绩占比(%)' AFTER usual_ratio;

-- 2. 重写触发器：按课程占比计算总评
DELIMITER $$

DROP TRIGGER IF EXISTS trg_score_before_insert$$

CREATE TRIGGER trg_score_before_insert
BEFORE INSERT ON score
FOR EACH ROW
BEGIN
    DECLARE v_usual_ratio TINYINT DEFAULT 40;
    DECLARE v_exam_ratio  TINYINT DEFAULT 60;

    IF NEW.usual_score IS NOT NULL AND (NEW.usual_score < 0 OR NEW.usual_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '平时成绩必须在0到100之间';
    END IF;

    IF NEW.exam_score IS NOT NULL AND (NEW.exam_score < 0 OR NEW.exam_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考试成绩必须在0到100之间';
    END IF;

    IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN
        SELECT c.usual_ratio, c.exam_ratio
        INTO v_usual_ratio, v_exam_ratio
        FROM teaching_class tc
        JOIN course c ON tc.course_id = c.course_id
        WHERE tc.teaching_class_id = NEW.teaching_class_id;

        SET NEW.final_score = ROUND(NEW.usual_score * v_usual_ratio / 100 + NEW.exam_score * v_exam_ratio / 100, 2);
        SET NEW.gpa_point = CASE
            WHEN NEW.final_score >= 90 THEN 4.0
            WHEN NEW.final_score >= 85 THEN 3.7
            WHEN NEW.final_score >= 82 THEN 3.3
            WHEN NEW.final_score >= 78 THEN 3.0
            WHEN NEW.final_score >= 75 THEN 2.7
            WHEN NEW.final_score >= 72 THEN 2.3
            WHEN NEW.final_score >= 68 THEN 2.0
            WHEN NEW.final_score >= 66 THEN 1.7
            WHEN NEW.final_score >= 64 THEN 1.5
            WHEN NEW.final_score >= 60 THEN 1.0
            ELSE 0.0
        END;
    END IF;

    IF NEW.entered_at IS NULL THEN
        SET NEW.entered_at = NOW();
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_score_before_update$$

CREATE TRIGGER trg_score_before_update
BEFORE UPDATE ON score
FOR EACH ROW
BEGIN
    DECLARE v_usual_ratio TINYINT DEFAULT 40;
    DECLARE v_exam_ratio  TINYINT DEFAULT 60;

    IF NEW.usual_score IS NOT NULL AND (NEW.usual_score < 0 OR NEW.usual_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '平时成绩必须在0到100之间';
    END IF;

    IF NEW.exam_score IS NOT NULL AND (NEW.exam_score < 0 OR NEW.exam_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考试成绩必须在0到100之间';
    END IF;

    IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN
        SELECT c.usual_ratio, c.exam_ratio
        INTO v_usual_ratio, v_exam_ratio
        FROM teaching_class tc
        JOIN course c ON tc.course_id = c.course_id
        WHERE tc.teaching_class_id = NEW.teaching_class_id;

        SET NEW.final_score = ROUND(NEW.usual_score * v_usual_ratio / 100 + NEW.exam_score * v_exam_ratio / 100, 2);
        SET NEW.gpa_point = CASE
            WHEN NEW.final_score >= 90 THEN 4.0
            WHEN NEW.final_score >= 85 THEN 3.7
            WHEN NEW.final_score >= 82 THEN 3.3
            WHEN NEW.final_score >= 78 THEN 3.0
            WHEN NEW.final_score >= 75 THEN 2.7
            WHEN NEW.final_score >= 72 THEN 2.3
            WHEN NEW.final_score >= 68 THEN 2.0
            WHEN NEW.final_score >= 66 THEN 1.7
            WHEN NEW.final_score >= 64 THEN 1.5
            WHEN NEW.final_score >= 60 THEN 1.0
            ELSE 0.0
        END;
    END IF;
END$$

DELIMITER ;

-- 3. 按课程占比重算已有成绩
UPDATE score sc
JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
JOIN course c ON tc.course_id = c.course_id
SET
    sc.final_score = ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2),
    sc.gpa_point = CASE
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 90 THEN 4.0
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 85 THEN 3.7
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 82 THEN 3.3
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 78 THEN 3.0
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 75 THEN 2.7
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 72 THEN 2.3
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 68 THEN 2.0
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 66 THEN 1.7
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 64 THEN 1.5
        WHEN ROUND(sc.usual_score * c.usual_ratio / 100 + sc.exam_score * c.exam_ratio / 100, 2) >= 60 THEN 1.0
        ELSE 0.0
    END
WHERE sc.usual_score IS NOT NULL
  AND sc.exam_score IS NOT NULL;
