-- 0605 成绩绩点规则与学生成绩查询升级脚本
-- 功能：
-- 1. 将成绩触发器中的绩点规则改为五级（十一等）制
-- 2. 重新计算已有成绩记录的总评和绩点
-- 3. 扩展 v_student_score 视图，提供课程号、学分、等级、教师等成绩查询字段

USE school;

DELIMITER $$

DROP TRIGGER IF EXISTS trg_score_before_insert$$

CREATE TRIGGER trg_score_before_insert
BEFORE INSERT ON score
FOR EACH ROW
BEGIN
    IF NEW.usual_score IS NOT NULL AND (NEW.usual_score < 0 OR NEW.usual_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '平时成绩必须在0到100之间';
    END IF;

    IF NEW.exam_score IS NOT NULL AND (NEW.exam_score < 0 OR NEW.exam_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考试成绩必须在0到100之间';
    END IF;

    IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN
        SET NEW.final_score = ROUND(NEW.usual_score * 0.4 + NEW.exam_score * 0.6, 2);
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
    IF NEW.usual_score IS NOT NULL AND (NEW.usual_score < 0 OR NEW.usual_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '平时成绩必须在0到100之间';
    END IF;

    IF NEW.exam_score IS NOT NULL AND (NEW.exam_score < 0 OR NEW.exam_score > 100) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考试成绩必须在0到100之间';
    END IF;

    IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN
        SET NEW.final_score = ROUND(NEW.usual_score * 0.4 + NEW.exam_score * 0.6, 2);
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

UPDATE score
SET
    final_score = ROUND(usual_score * 0.4 + exam_score * 0.6, 2),
    gpa_point = CASE
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 90 THEN 4.0
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 85 THEN 3.7
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 82 THEN 3.3
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 78 THEN 3.0
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 75 THEN 2.7
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 72 THEN 2.3
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 68 THEN 2.0
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 66 THEN 1.7
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 64 THEN 1.5
        WHEN ROUND(usual_score * 0.4 + exam_score * 0.6, 2) >= 60 THEN 1.0
        ELSE 0.0
    END
WHERE usual_score IS NOT NULL
  AND exam_score IS NOT NULL;

CREATE OR REPLACE VIEW v_student_score AS
SELECT
    sc.score_id,
    sc.student_id,
    s.student_name,
    sem.semester_id,
    sem.semester_name,
    tc.teaching_class_id,
    c.course_id,
    c.course_name,
    c.course_type,
    c.credit,
    t.teacher_name,
    sc.usual_score,
    sc.exam_score,
    sc.final_score,
    sc.gpa_point,
    CASE
        WHEN sc.final_score IS NULL THEN NULL
        WHEN sc.final_score >= 90 THEN 'A'
        WHEN sc.final_score >= 85 THEN 'A-'
        WHEN sc.final_score >= 82 THEN 'B+'
        WHEN sc.final_score >= 78 THEN 'B'
        WHEN sc.final_score >= 75 THEN 'B-'
        WHEN sc.final_score >= 72 THEN 'C+'
        WHEN sc.final_score >= 68 THEN 'C'
        WHEN sc.final_score >= 66 THEN 'C-'
        WHEN sc.final_score >= 64 THEN 'D'
        WHEN sc.final_score >= 60 THEN 'D-'
        ELSE 'F'
    END AS grade_level,
    sc.is_submitted,
    sc.entered_at
FROM score sc
JOIN student s ON sc.student_id = s.student_id
JOIN teaching_class tc ON sc.teaching_class_id = tc.teaching_class_id
JOIN semester sem ON tc.semester_id = sem.semester_id
JOIN course c ON tc.course_id = c.course_id
JOIN teacher t ON tc.teacher_id = t.teacher_id;
