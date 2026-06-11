@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem ============================================================
rem  rebuild-db.bat  Rebuild the school database from SQL scripts
rem  WARNING: step 1 (school.sql) DROPS the entire school database.
rem           All existing data will be lost.
rem ============================================================

rem ---- Database connection (edit these to match application.yml) ----
set "DB_HOST=124.70.200.70"
set "DB_PORT=3306"
set "DB_USER=root"
set "DB_NAME=school"

echo ============================================================
echo  Rebuild database: %DB_NAME%  on  %DB_HOST%:%DB_PORT%  (user: %DB_USER%)
echo ------------------------------------------------------------
echo  WARNING: this will DROP and recreate the "%DB_NAME%" database.
echo           ALL existing data will be permanently deleted.
echo ============================================================
echo.

echo [0/8] Checking mysql client...
where mysql >nul 2>nul
if errorlevel 1 (
    echo [Error] mysql client not found. Please install MySQL and add it to PATH.
    pause
    exit /b 1
)

set /p CONFIRM=Type YES to continue, anything else to cancel:
if /i not "%CONFIRM%"=="YES" (
    echo Cancelled. No changes were made.
    pause
    exit /b 0
)

set /p DB_PASS=Enter MySQL password for %DB_USER%:

set "MYSQL=mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS%"

echo.
echo [1/7] school.sql  (tables, triggers, procedures, base data)...
%MYSQL% < sql\school.sql
if errorlevel 1 goto :failed

echo [2/7] 0601_student_features.sql  (course drop / schedule adjust)...
%MYSQL% %DB_NAME% < sql\0601_student_features.sql
if errorlevel 1 goto :failed

echo [3/7] 0607_teacher_features.sql  (score submit window / adjust)...
%MYSQL% %DB_NAME% < sql\0607_teacher_features.sql
if errorlevel 1 goto :failed

echo [4/7] 0607_score_change_request.sql  (score change request table)...
%MYSQL% %DB_NAME% < sql\0607_score_change_request.sql
if errorlevel 1 goto :failed

echo [5/7] 0607_demo_seed_data.sql  (demo students/teachers/courses/enrollments)...
%MYSQL% < sql\0607_demo_seed_data.sql
if errorlevel 1 goto :failed

echo [6/7] 0607_fix_schedule_conflicts.sql  (resolve teaching class conflicts)...
%MYSQL% < sql\0607_fix_schedule_conflicts.sql
if errorlevel 1 goto :failed

echo [7/8] 0611_course_selection_manual_control.sql  (manual course selection control)...
%MYSQL% %DB_NAME% < sql\0611_course_selection_manual_control.sql
if errorlevel 1 goto :failed

echo [8/9] 0611_notification.sql  (in-app notifications / announcements)...
%MYSQL% %DB_NAME% < sql\0611_notification.sql
if errorlevel 1 goto :failed

echo [9/9] Done.
echo.
echo ============================================================
echo  Database "%DB_NAME%" rebuilt successfully.
echo ============================================================
set "DB_PASS="
endlocal
pause
exit /b 0

:failed
echo.
echo [Error] A step failed. Please check the message above.
set "DB_PASS="
endlocal
pause
exit /b 1
