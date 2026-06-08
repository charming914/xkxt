-- 退课逻辑修正：新学期未设置开课日期或课程尚未开课时允许学生直接退课

DROP TRIGGER IF EXISTS trg_course_selection_before_drop_update;
DROP TRIGGER IF EXISTS trg_course_drop_request_before_insert;

DELIMITER $$

CREATE TRIGGER trg_course_selection_before_drop_update
BEFORE UPDATE ON course_selection
FOR EACH ROW
BEGIN
    DECLARE v_course_start_date DATE;
    DECLARE v_approved_count INT DEFAULT 0;

    IF OLD.selection_status = '已选' AND NEW.selection_status = '已退' THEN
        SELECT tc.course_start_date
        INTO v_course_start_date
        FROM teaching_class tc
        WHERE tc.teaching_class_id = OLD.teaching_class_id;

        IF v_course_start_date IS NOT NULL
           AND CURDATE() >= v_course_start_date THEN
            SELECT COUNT(*)
            INTO v_approved_count
            FROM course_drop_request cdr
            WHERE cdr.student_id = OLD.student_id
              AND cdr.teaching_class_id = OLD.teaching_class_id
              AND cdr.request_status = '已通过'
              AND cdr.teacher_status = '已通过'
              AND cdr.admin_status = '已通过'
              AND cdr.created_at <= DATE_ADD(v_course_start_date, INTERVAL 21 DAY);

            IF CURDATE() > DATE_ADD(v_course_start_date, INTERVAL 21 DAY)
               AND v_approved_count = 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已超过开课三周，不能退课';
            END IF;

            IF CURDATE() > DATE_ADD(v_course_start_date, INTERVAL 7 DAY)
               AND v_approved_count = 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '开课超过一周，需教师和管理员均同意后才能退课';
            END IF;
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_course_drop_request_before_insert
BEFORE INSERT ON course_drop_request
FOR EACH ROW
BEGIN
    DECLARE v_course_start_date DATE;
    DECLARE v_selected_count INT DEFAULT 0;
    DECLARE v_pending_count INT DEFAULT 0;

    SELECT tc.course_start_date
    INTO v_course_start_date
    FROM teaching_class tc
    WHERE tc.teaching_class_id = NEW.teaching_class_id;

    IF v_course_start_date IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '新学期未设置开课日期，可直接退课，无需提交申请';
    END IF;

    SELECT COUNT(*)
    INTO v_selected_count
    FROM course_selection cs
    WHERE cs.student_id = NEW.student_id
      AND cs.teaching_class_id = NEW.teaching_class_id
      AND cs.selection_status = '已选';

    IF v_selected_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '未选该课程，不能提交退课申请';
    END IF;

    IF CURDATE() < v_course_start_date THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '课程尚未开课，可直接退课，无需提交申请';
    END IF;

    IF CURDATE() <= DATE_ADD(v_course_start_date, INTERVAL 7 DAY) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '开课一周内可直接退课，无需提交申请';
    END IF;

    IF CURDATE() > DATE_ADD(v_course_start_date, INTERVAL 21 DAY) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已超过开课三周，不能提交退课申请';
    END IF;

    SELECT COUNT(*)
    INTO v_pending_count
    FROM course_drop_request cdr
    WHERE cdr.student_id = NEW.student_id
      AND cdr.teaching_class_id = NEW.teaching_class_id
      AND cdr.request_status = '待审核';

    IF v_pending_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该课程已有待审核退课申请';
    END IF;
END$$

DELIMITER ;
