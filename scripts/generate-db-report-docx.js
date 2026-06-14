const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const outPath = path.join(root, 'docs', '教学事务管理系统数据库大作业报告.docx')
const CM = 360000
const IMAGE_W = 15 * CM
const TABLE_W_DXA = 7938

function escapeXml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

function crc32(buf) {
  let c = 0xffffffff
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i]
    for (let k = 0; k < 8; k++) {
      c = (c >>> 1) ^ (0xedb88320 & -(c & 1))
    }
  }
  return (c ^ 0xffffffff) >>> 0
}

function dosDateTime(date = new Date()) {
  const time = (date.getHours() << 11) | (date.getMinutes() << 5) | Math.floor(date.getSeconds() / 2)
  const dosDate = ((date.getFullYear() - 1980) << 9) | ((date.getMonth() + 1) << 5) | date.getDate()
  return { time, date: dosDate }
}

function createZip(files) {
  const localParts = []
  const centralParts = []
  let offset = 0
  const dt = dosDateTime()

  for (const file of files) {
    const name = Buffer.from(file.name.replace(/\\/g, '/'), 'utf8')
    const data = Buffer.isBuffer(file.data) ? file.data : Buffer.from(file.data, 'utf8')
    const crc = crc32(data)

    const local = Buffer.alloc(30)
    local.writeUInt32LE(0x04034b50, 0)
    local.writeUInt16LE(20, 4)
    local.writeUInt16LE(0x0800, 6)
    local.writeUInt16LE(0, 8)
    local.writeUInt16LE(dt.time, 10)
    local.writeUInt16LE(dt.date, 12)
    local.writeUInt32LE(crc, 14)
    local.writeUInt32LE(data.length, 18)
    local.writeUInt32LE(data.length, 22)
    local.writeUInt16LE(name.length, 26)
    local.writeUInt16LE(0, 28)
    localParts.push(local, name, data)

    const central = Buffer.alloc(46)
    central.writeUInt32LE(0x02014b50, 0)
    central.writeUInt16LE(20, 4)
    central.writeUInt16LE(20, 6)
    central.writeUInt16LE(0x0800, 8)
    central.writeUInt16LE(0, 10)
    central.writeUInt16LE(dt.time, 12)
    central.writeUInt16LE(dt.date, 14)
    central.writeUInt32LE(crc, 16)
    central.writeUInt32LE(data.length, 20)
    central.writeUInt32LE(data.length, 24)
    central.writeUInt16LE(name.length, 28)
    central.writeUInt16LE(0, 30)
    central.writeUInt16LE(0, 32)
    central.writeUInt16LE(0, 34)
    central.writeUInt16LE(0, 36)
    central.writeUInt32LE(0, 38)
    central.writeUInt32LE(offset, 42)
    centralParts.push(central, name)

    offset += local.length + name.length + data.length
  }

  const centralSize = centralParts.reduce((sum, part) => sum + part.length, 0)
  const end = Buffer.alloc(22)
  end.writeUInt32LE(0x06054b50, 0)
  end.writeUInt16LE(0, 4)
  end.writeUInt16LE(0, 6)
  end.writeUInt16LE(files.length, 8)
  end.writeUInt16LE(files.length, 10)
  end.writeUInt32LE(centralSize, 12)
  end.writeUInt32LE(offset, 16)
  end.writeUInt16LE(0, 20)

  return Buffer.concat([...localParts, ...centralParts, end])
}

function rPr(font = '宋体', size = 21, bold = false) {
  return `<w:rPr><w:rFonts w:ascii="Times New Roman" w:eastAsia="${font}" w:hAnsi="Times New Roman"/><w:sz w:val="${size}"/><w:szCs w:val="${size}"/>${bold ? '<w:b/><w:bCs/>' : ''}</w:rPr>`
}

function paragraph(text, opts = {}) {
  const { style, align, indent = true, bold = false, font = '宋体', size = 21, spacingAfter = 160 } = opts
  const styleXml = style ? `<w:pStyle w:val="${style}"/>` : ''
  const jcXml = align ? `<w:jc w:val="${align}"/>` : ''
  const indXml = indent
    ? '<w:ind w:firstLineChars="200"/>'
    : '<w:ind w:left="0" w:right="0" w:firstLine="0" w:firstLineChars="0"/>'
  return `<w:p><w:pPr>${styleXml}${jcXml}<w:spacing w:after="${spacingAfter}" w:line="283" w:lineRule="auto"/>${indXml}</w:pPr><w:r>${rPr(font, size, bold)}<w:t xml:space="preserve">${escapeXml(text)}</w:t></w:r></w:p>`
}

function heading(level, text) {
  const cfg = {
    1: { style: 'Heading1', size: 32, align: 'center' },
    2: { style: 'Heading2', size: 28 },
    3: { style: 'Heading3', size: 24 },
    4: { style: 'Heading4', size: 21 }
  }[level]
  return paragraph(text, { ...cfg, font: '黑体', bold: true, indent: false, spacingAfter: 122 })
}

function bullet(text) {
  return paragraph(`• ${text}`, { indent: false, spacingAfter: 80 })
}

function pageBreak() {
  return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'
}

function table(caption, headers, rows) {
  const widths = [2000, 1900, 2300, 1738]
  const effectiveWidths = headers.length === 1
    ? [TABLE_W_DXA]
    : headers.length === 3
      ? [1500, 4000, 2438]
      : widths
  const cell = (text, index, header = false) => {
    return `<w:tc><w:tcPr><w:tcW w:w="${effectiveWidths[index] || 2500}" w:type="dxa"/><w:tcMar><w:top w:w="80" w:type="dxa"/><w:left w:w="100" w:type="dxa"/><w:bottom w:w="80" w:type="dxa"/><w:right w:w="100" w:type="dxa"/></w:tcMar><w:tcBorders><w:top w:val="single" w:sz="4"/><w:left w:val="single" w:sz="4"/><w:bottom w:val="single" w:sz="4"/><w:right w:val="single" w:sz="4"/></w:tcBorders></w:tcPr><w:p><w:pPr><w:jc w:val="center"/><w:spacing w:after="0" w:line="240" w:lineRule="auto"/><w:ind w:left="0" w:firstLine="0" w:firstLineChars="0"/></w:pPr><w:r>${rPr(header ? '黑体' : '宋体', 18, header)}<w:t xml:space="preserve">${escapeXml(text)}</w:t></w:r></w:p></w:tc>`
  }
  const grid = effectiveWidths.map(w => `<w:gridCol w:w="${w}"/>`).join('')
  const headerRow = `<w:tr>${headers.map((h, i) => cell(h, i, true)).join('')}</w:tr>`
  const bodyRows = rows.map(row => `<w:tr>${row.map((v, i) => cell(v, i)).join('')}</w:tr>`).join('')
  return [
    paragraph(caption, { align: 'center', indent: false, spacingAfter: 20 }),
    `<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="${TABLE_W_DXA}" w:type="dxa"/><w:jc w:val="center"/><w:tblLayout w:type="fixed"/><w:tblBorders><w:top w:val="single" w:sz="4"/><w:left w:val="single" w:sz="4"/><w:bottom w:val="single" w:sz="4"/><w:right w:val="single" w:sz="4"/><w:insideH w:val="single" w:sz="4"/><w:insideV w:val="single" w:sz="4"/></w:tblBorders></w:tblPr><w:tblGrid>${grid}</w:tblGrid>${headerRow}${bodyRows}</w:tbl>`,
    paragraph('', { indent: false, spacingAfter: 140 })
  ].join('')
}

function image(relId, caption, cx, cy) {
  return `<w:p><w:pPr><w:jc w:val="center"/></w:pPr><w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="${cx}" cy="${cy}"/><wp:docPr id="${relId.replace('rId', '')}" name="${caption}"/><a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="${relId.replace('rId', '')}" name="${caption}"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="${relId}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${cx}" cy="${cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>${paragraph(caption, { align: 'center', indent: false, spacingAfter: 160 })}`
}

function codeBlock(caption, lines) {
  const codeCell = (text, top = false, bottom = false) => `<w:tc><w:tcPr><w:tcW w:w="${TABLE_W_DXA}" w:type="dxa"/><w:tcMar><w:top w:w="70" w:type="dxa"/><w:left w:w="120" w:type="dxa"/><w:bottom w:w="70" w:type="dxa"/><w:right w:w="120" w:type="dxa"/></w:tcMar><w:tcBorders><w:top w:val="${top ? 'single' : 'nil'}" w:sz="4"/><w:left w:val="single" w:sz="4"/><w:bottom w:val="${bottom ? 'single' : 'nil'}" w:sz="4"/><w:right w:val="single" w:sz="4"/></w:tcBorders></w:tcPr><w:p><w:pPr><w:jc w:val="left"/><w:spacing w:after="0" w:line="240" w:lineRule="auto"/><w:ind w:left="0" w:firstLine="0" w:firstLineChars="0"/></w:pPr><w:r>${rPr('宋体', 18, false)}<w:t xml:space="preserve">${escapeXml(text)}</w:t></w:r></w:p></w:tc>`
  const bodyRows = lines.map((line, i) => `<w:tr>${codeCell(line, i === 0, i === lines.length - 1)}</w:tr>`).join('')
  return [
    paragraph(caption, { align: 'center', indent: false, spacingAfter: 20 }),
    `<w:tbl><w:tblPr><w:tblW w:w="${TABLE_W_DXA}" w:type="dxa"/><w:jc w:val="center"/><w:tblLayout w:type="fixed"/><w:tblBorders><w:top w:val="single" w:sz="4"/><w:left w:val="single" w:sz="4"/><w:bottom w:val="single" w:sz="4"/><w:right w:val="single" w:sz="4"/><w:insideH w:val="nil"/><w:insideV w:val="nil"/></w:tblBorders></w:tblPr><w:tblGrid><w:gridCol w:w="${TABLE_W_DXA}"/></w:tblGrid>${bodyRows}</w:tbl>`,
    paragraph('', { indent: false, spacingAfter: 140 })
  ].join('')
}

function svgFunction() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="720" viewBox="0 0 1200 720">
  <style>
    text{font-family:"Microsoft YaHei",Arial,sans-serif;fill:#1f2937}
    .title{fill:#2563eb;stroke:#1e40af;stroke-width:2}
    .box{fill:#f8fafc;stroke:#64748b;stroke-width:2;rx:10}
    .admin{fill:#e0f2fe;stroke:#0284c7}.student{fill:#dcfce7;stroke:#16a34a}.teacher{fill:#fef3c7;stroke:#d97706}.approval{fill:#f3e8ff;stroke:#9333ea}.support{fill:#fee2e2;stroke:#dc2626}
    .line{stroke:#64748b;stroke-width:2;fill:none}
    .h{font-weight:700;font-size:22px;fill:white}.t{font-weight:700;font-size:18px}.s{font-size:15px}
  </style>
  <rect class="title" x="445" y="28" width="310" height="70" rx="12"/><text class="h" x="600" y="58" text-anchor="middle">教学事务管理系统</text><text x="600" y="83" fill="#dbeafe" font-size="15" text-anchor="middle">Teaching Affairs Management System</text>
  <path class="line" d="M600 98V138H130M600 138H360M600 138H600M600 138H840M600 138H1070"/>
  ${column(45, 155, '基础数据管理', ['院系/专业/班级', '学生/教师/课程', '教室/学期', '删除保护'], 'admin')}
  ${column(285, 155, '学生选课与学业', ['可选课程查询', '选课/退课/补选', '课表/成绩/考试', '培养方案进度'], 'student')}
  ${column(525, 155, '教师教学事务', ['授课课表', '学生名单', '成绩录入提交', '调课/考试修改'], 'teacher')}
  ${column(765, 155, '审批与考试管理', ['退课/补选审核', '成绩修改/复核', '考试生成与调整', '补考/重修处理'], 'approval')}
  ${column(1005, 155, '系统支撑', ['用户角色权限', '通知公告', '操作日志', '数据导入导出'], 'support')}
  </svg>`
}

function column(x, y, title, items, cls) {
  const lines = items.map((item, i) => `<rect class="box" x="${x + 18}" y="${y + 82 + i * 72}" width="170" height="48" rx="8"/><text class="s" x="${x + 103}" y="${y + 112 + i * 72}" text-anchor="middle">${escapeXml(item)}</text><path class="line" d="M${x + 103} ${y + 58 + i * 72}V${y + 82 + i * 72}"/>`).join('')
  return `<rect class="${cls}" x="${x}" y="${y}" width="206" height="58" rx="10"/><text class="t" x="${x + 103}" y="${y + 36}" text-anchor="middle">${escapeXml(title)}</text>${lines}`
}

function svgErCore() {
  return erSvg('ER 图分层展示（一）：核心教学关系', [
    ['院系', 60, 70], ['专业', 240, 70], ['行政班', 420, 70], ['学生', 600, 70],
    ['课程', 60, 230], ['学期', 240, 230], ['教师', 420, 230], ['教室', 600, 230],
    ['教学班', 330, 390], ['上课时间', 130, 520], ['选课记录', 330, 520], ['成绩', 530, 520], ['考试安排', 730, 520]
  ], [
    [60, 110, 240, 110, '1:N'], [240, 110, 420, 110, '1:N'], [420, 110, 600, 110, '1:N'],
    [60, 270, 330, 420, '1:N'], [240, 270, 360, 390, '1:N'], [420, 270, 390, 390, '1:N'], [600, 270, 440, 390, '1:N'],
    [330, 450, 130, 550, '1:N'], [330, 450, 330, 520, '1:N'], [330, 450, 530, 520, '1:N'], [330, 450, 730, 520, '1:1'],
    [600, 130, 330, 520, 'N:M'], [600, 130, 530, 520, '1:N']
  ])
}

function svgErApproval() {
  return erSvg('ER 图分层展示（二）：申请审批关系', [
    ['学生', 90, 70], ['教师', 360, 70], ['管理员用户', 630, 70],
    ['教学班', 90, 230], ['成绩', 360, 230], ['考试安排', 630, 230],
    ['补选/退课申请', 40, 410], ['调课申请', 240, 410], ['成绩修改/复核', 440, 410], ['考试修改申请', 650, 410],
    ['补考/重修申请', 250, 560], ['通知/公告', 560, 560]
  ], [
    [90, 110, 40, 440, '发起'], [90, 110, 440, 440, '复核'], [90, 110, 250, 590, '补考/重修'],
    [360, 110, 240, 440, '发起'], [360, 110, 440, 440, '申请修改'], [360, 110, 650, 440, '发起'],
    [90, 260, 40, 440, '目标'], [90, 260, 240, 440, '目标'], [360, 260, 440, 440, '目标'], [630, 260, 650, 440, '目标'],
    [630, 110, 40, 440, '审核'], [630, 110, 240, 440, '审核'], [630, 110, 440, 440, '审核'], [630, 110, 650, 440, '审核'],
    [630, 110, 560, 590, '发布/投递']
  ])
}

function svgErSupport() {
  return erSvg('ER 图分层展示（三）：用户权限与通知支撑', [
    ['系统用户', 120, 110], ['用户角色关联', 360, 110], ['角色', 600, 110],
    ['公告', 180, 310], ['通知', 480, 310], ['操作日志', 720, 310],
    ['学生/教师引用', 330, 500]
  ], [
    [120, 140, 360, 140, '1:N'], [360, 140, 600, 140, 'N:1'], [180, 340, 480, 340, '1:N'],
    [120, 140, 480, 340, '接收'], [120, 140, 720, 340, '记录'], [120, 140, 330, 500, 'ref_id']
  ], 900, 620)
}

function erSvg(title, boxes, lines, width = 900, height = 680) {
  const boxXml = boxes.map(([name, x, y]) => `<rect x="${x}" y="${y}" width="140" height="54" rx="9" fill="#eef2ff" stroke="#4f46e5" stroke-width="2"/><text x="${x + 70}" y="${y + 34}" font-size="17" font-weight="700" text-anchor="middle">${escapeXml(name)}</text>`).join('')
  const lineXml = lines.map(([x1, y1, x2, y2, label]) => `<path d="M${x1 + 70} ${y1} L${x2 + 70} ${y2}" stroke="#64748b" stroke-width="1.8" fill="none"/><text x="${(x1 + x2) / 2 + 70}" y="${(y1 + y2) / 2 - 6}" font-size="13" fill="#334155" text-anchor="middle">${escapeXml(label)}</text>`).join('')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}"><rect width="${width}" height="${height}" fill="white"/><g font-family="Microsoft YaHei">${lineXml}${boxXml}</g></svg>`
}

const tableRows = {
  department: [['department_id', 'VARCHAR(10)', 'PK', '院系编号'], ['department_name', 'VARCHAR(50)', 'UNIQUE', '院系名称'], ['office_address', 'VARCHAR(100)', 'NULL', '办公地点'], ['contact_phone', 'VARCHAR(20)', 'NULL', '联系电话']],
  student: [['student_id', 'VARCHAR(12)', 'PK', '学号'], ['student_name', 'VARCHAR(20)', 'NOT NULL', '姓名'], ['gender', 'CHAR(1)', 'NOT NULL', '性别'], ['birth_date', 'DATE', 'NULL', '出生日期'], ['hometown', 'VARCHAR(50)', 'NULL', '籍贯'], ['phone', 'VARCHAR(20)', 'NULL', '手机号'], ['email', 'VARCHAR(50)', 'NULL', '邮箱'], ['enrollment_year', 'CHAR(4)', 'NOT NULL', '入学年份'], ['department_id', 'VARCHAR(10)', 'FK', '院系编号'], ['major_id', 'VARCHAR(10)', 'FK', '专业编号'], ['class_id', 'VARCHAR(10)', 'FK', '班级编号'], ['status', 'VARCHAR(20)', "DEFAULT '正常'", '学籍状态']],
  teacher: [['teacher_id', 'VARCHAR(12)', 'PK', '工号'], ['teacher_name', 'VARCHAR(20)', 'NOT NULL', '姓名'], ['gender', 'CHAR(1)', 'NOT NULL', '性别'], ['birth_date', 'DATE', 'NULL', '出生日期'], ['title_name', 'VARCHAR(20)', 'NOT NULL', '职称'], ['education', 'VARCHAR(20)', 'NULL', '学历'], ['phone', 'VARCHAR(20)', 'NULL', '手机号'], ['email', 'VARCHAR(50)', 'NULL', '邮箱'], ['department_id', 'VARCHAR(10)', 'FK', '所属院系'], ['status', 'VARCHAR(20)', "DEFAULT '在职'", '教师状态']],
  semester: [['semester_id', 'INT', 'PK, AI', '学期编号'], ['semester_name', 'VARCHAR(30)', 'UNIQUE', '学期名称'], ['school_year', 'VARCHAR(20)', 'NOT NULL', '学年'], ['term_code', 'VARCHAR(10)', 'NOT NULL', '学期代码'], ['start_date', 'DATE', 'NOT NULL', '开始日期'], ['end_date', 'DATE', 'NOT NULL', '结束日期'], ['course_select_start', 'DATETIME', 'NOT NULL', '选课开始'], ['course_select_end', 'DATETIME', 'NOT NULL', '选课结束'], ['course_selection_manual_mode', 'VARCHAR(10)', "DEFAULT 'AUTO'", '选课控制'], ['score_submit_start', 'DATETIME', 'NULL', '成绩提交开始'], ['score_submit_end', 'DATETIME', 'NULL', '成绩提交结束'], ['makeup_request_start', 'DATETIME', 'NULL', '补考申请开始'], ['makeup_request_end', 'DATETIME', 'NULL', '补考申请结束'], ['is_current', 'TINYINT', 'DEFAULT 0', '当前学期']],
  course: [['course_id', 'VARCHAR(12)', 'PK', '课程编号'], ['course_name', 'VARCHAR(50)', 'UNIQUE', '课程名称'], ['course_type', 'VARCHAR(20)', 'NOT NULL', '课程类型'], ['credit', 'DECIMAL(4,1)', 'NOT NULL', '学分'], ['total_hours', 'INT', 'NOT NULL', '总学时'], ['assessment_method', 'VARCHAR(20)', "DEFAULT '考试'", '考核方式'], ['usual_ratio', 'TINYINT', 'DEFAULT 40', '平时占比'], ['exam_ratio', 'TINYINT', 'DEFAULT 60', '考试占比'], ['department_id', 'VARCHAR(10)', 'FK', '开课院系'], ['is_required', 'TINYINT', 'DEFAULT 1', '是否必修'], ['description', 'VARCHAR(200)', 'NULL', '课程说明']],
  teaching_class: [['teaching_class_id', 'INT', 'PK, AI', '教学班编号'], ['semester_id', 'INT', 'FK', '学期编号'], ['course_id', 'VARCHAR(12)', 'FK', '课程编号'], ['teacher_id', 'VARCHAR(12)', 'FK', '教师工号'], ['classroom_id', 'VARCHAR(10)', 'FK, NULL', '教室编号'], ['course_start_date', 'DATE', 'NULL', '开课日期'], ['week_range', 'VARCHAR(30)', 'NOT NULL', '周次范围'], ['is_retake_class', 'TINYINT', 'DEFAULT 0', '是否重修班'], ['max_students', 'INT', 'DEFAULT 60', '容量'], ['selected_count', 'INT', 'DEFAULT 0', '已选人数'], ['status', 'VARCHAR(20)', "DEFAULT '未开始'", '教学班状态']],
  session: [['session_id', 'INT', 'PK, AI', '上课时间编号'], ['teaching_class_id', 'INT', 'FK', '教学班编号'], ['week_day', 'TINYINT', 'NOT NULL', '星期几'], ['section_desc', 'VARCHAR(20)', 'NOT NULL', '节次'], ['week_parity', 'VARCHAR(10)', "DEFAULT '全周'", '全周/单周/双周'], ['sort_order', 'INT', 'DEFAULT 1', '排序']],
  selection: [['selection_id', 'BIGINT', 'PK, AI', '选课记录编号'], ['student_id', 'VARCHAR(12)', 'FK', '学号'], ['teaching_class_id', 'INT', 'FK', '教学班编号'], ['selection_time', 'DATETIME', 'CURRENT_TIMESTAMP', '选课时间'], ['selection_status', 'VARCHAR(20)', "DEFAULT '已选'", '选课状态']],
  score: [['score_id', 'BIGINT', 'PK, AI', '成绩编号'], ['student_id', 'VARCHAR(12)', 'FK', '学号'], ['teaching_class_id', 'INT', 'FK', '教学班编号'], ['usual_score', 'DECIMAL(5,2)', 'NULL', '平时成绩'], ['exam_score', 'DECIMAL(5,2)', 'NULL', '考试成绩'], ['final_score', 'DECIMAL(5,2)', 'NULL', '总评成绩'], ['gpa_point', 'DECIMAL(3,1)', 'NULL', '绩点'], ['is_submitted', 'TINYINT', 'DEFAULT 0', '是否提交'], ['entered_at', 'DATETIME', 'NULL', '录入时间']],
  exam: [['exam_id', 'BIGINT', 'PK, AI', '考试安排编号'], ['teaching_class_id', 'INT', 'FK, UNIQUE', '教学班编号'], ['exam_date', 'DATE', 'NOT NULL', '考试日期'], ['start_time', 'TIME', 'NOT NULL', '开始时间'], ['end_time', 'TIME', 'NOT NULL', '结束时间'], ['classroom_id', 'VARCHAR(10)', 'FK, NULL', '考试教室'], ['exam_status', 'VARCHAR(20)', "DEFAULT '已排定'", '考试状态'], ['created_at', 'DATETIME', 'CURRENT_TIMESTAMP', '创建时间'], ['updated_at', 'DATETIME', 'NULL', '更新时间']]
}

const docParts = []
docParts.push(heading(1, '教学事务管理系统数据库大作业报告'))
docParts.push(paragraph('课程：《数据库原理(1)》', { align: 'center', indent: false }))
docParts.push(paragraph('项目名称：教学事务管理系统', { align: 'center', indent: false }))
docParts.push(pageBreak())

docParts.push(heading(2, '一、项目背景'))
docParts.push(heading(3, '1.1 项目背景与问题分析'))
docParts.push(paragraph('高校教学事务覆盖学生、教师和管理员三类用户，业务对象多、流程长、状态变化频繁。传统人工管理方式容易出现数据重复录入、课表冲突难以及时发现、成绩提交缺少统一约束、审批结果不可追踪等问题。随着课程规模和管理要求提高，需要一个以数据库为核心的教学事务管理系统，把课程、教学班、选课、成绩、考试和审批流程统一管理。'))
docParts.push(paragraph('本系统围绕教学班建立核心数据模型，将学期、课程、教师、教室、学生和成绩等对象连接起来，并通过触发器、存储过程、视图、外键和唯一约束保证数据一致性。系统在业务上支持管理员维护基础数据，学生完成选课和学业查询，教师处理授课、成绩和申请，管理员进行审核和公告发布。'))
docParts.push(paragraph('从完整链路看，系统并不是只完成页面录入，而是把“业务需求、功能设计、数据库建模、后端接口、前端交互、部署运行、数据维护”串成闭环。需求侧先识别教学事务中的人、事、物和状态；数据库侧把这些对象拆分成表和关系；后端侧围绕角色和业务规则封装接口；前端侧提供课表、表格、表单、审批和通知界面；部署侧则把 MySQL 数据库作为可以长期保存、备份和迁移的核心数据资产。'))
docParts.push(heading(3, '1.2 建设目标'))
docParts.push(bullet('建立统一的教学基础数据库，减少重复维护和信息不一致。'))
docParts.push(bullet('支持选课、课表、成绩、考试、退课、调课、补选、补考等教学事务闭环。'))
docParts.push(bullet('通过数据库约束和后端校验控制关键业务规则，例如容量、时间冲突、成绩提交窗口和申请状态。'))
docParts.push(bullet('提供管理员、教师、学生三个角色的清晰数据边界和操作权限。'))

docParts.push(heading(2, '二、总体功能'))
docParts.push(paragraph('系统总体功能分为基础数据管理、学生选课与学业服务、教师教学事务、审批与考试管理、系统支撑五个部分。整体结构参考功能树方式组织，如图1所示。'))
docParts.push(image('rId1', '图1 教学事务管理系统总体功能设计图', IMAGE_W, Math.round(IMAGE_W * 720 / 1200)))
docParts.push(paragraph('基础数据管理维护系统运行所需的静态资料；学生选课与学业服务面向学生学习过程；教师教学事务面向教师授课和成绩处理；审批与考试管理负责跨角色流程闭环；系统支撑提供用户角色、通知公告、操作日志和数据导入导出能力。'))

docParts.push(heading(2, '三、功能设计'))
docParts.push(heading(3, '3.1 管理员端功能设计'))
docParts.push(paragraph('管理员端负责系统基础数据和全局业务规则维护，是系统中权限最高、覆盖面最广的角色。管理员模块不是单纯的数据录入页面，而是承担教学规则配置、教学资源调度、审批流转控制和系统运行监控等职责。'))
docParts.push(heading(4, '3.1.1 基础数据管理'))
docParts.push(bullet('前端设计：院系、专业、班级、学生、教师、课程、教室等页面采用表格加筛选栏的形式。表格负责展示完整数据，筛选栏负责按编号、名称、院系、专业、状态等条件快速定位记录。新增和编辑使用弹窗表单，避免页面跳转打断管理流程。'))
docParts.push(bullet('后端设计：后端为每类基础数据提供查询、新增、修改、删除和删除前检查接口。删除接口不会直接删除，而是先检查是否存在关联教学班、选课、成绩、考试或申请数据。'))
docParts.push(bullet('数据库设计：基础数据分别落到 department、major、class_info、student、teacher、course、classroom 等表。学生表通过 department_id、major_id、class_id 关联院系、专业和班级；课程和教师都关联院系，保证组织关系清晰。'))
docParts.push(bullet('设计原因：基础数据是后续教学业务的前置条件。通过独立维护基础数据，可以避免教学班、选课、成绩等业务表重复保存院系、专业、教师名称等信息，降低冗余。'))
docParts.push(heading(4, '3.1.2 学期与教学班管理'))
docParts.push(bullet('前端设计：学期页面只要求管理员选择学期开始日期，页面实时预览学期名称、教学周、考试周、选课时间。教学班页面提供课程、教师、教室、学期、容量等字段，支持筛选和导出教室课表。'))
docParts.push(bullet('后端设计：创建学期时由服务层推导学年、学期代码、结束日期、考试周和默认选课时间。创建或修改教学班时检查教师、教室、节次和单双周是否冲突。'))
docParts.push(bullet('数据库设计：学期信息写入 semester；教学班主信息写入 teaching_class；一周内的多次课写入 teaching_class_session。这样可以支持一门课每周多次上课，也可以支持单周和双周课程。'))
docParts.push(bullet('设计原因：学期和教学班是教学运行的核心上下文。系统把时间规则集中在学期，把授课实体集中在教学班，让选课、成绩、考试和调课都能基于同一套数据判断。'))
docParts.push(heading(4, '3.1.3 审批管理'))
docParts.push(bullet('前端设计：审批页面按申请类型和审核状态分类展示，包括补选、退课、调课、成绩修改、成绩复核、考试修改、补考和重修。管理员在弹窗中查看原始数据、申请数据和申请原因，再填写审核意见。'))
docParts.push(bullet('后端设计：审核通过时不只是更新申请状态，还要根据申请类型更新业务主表。例如补选通过后写入选课记录，调课通过后更新教学班上课时间，考试修改通过后更新考试安排，成绩修改通过后把成绩退回未提交状态。'))
docParts.push(bullet('数据库设计：各类申请独立建表，保存申请对象、申请人、原始数据、目标数据、审核状态、审核意见、审核人和审核时间。审批结果必要时同步 course_selection、teaching_class、score、exam_arrangement 等主业务表。'))
docParts.push(bullet('设计原因：申请审批具有过程性，不能只保存最终结果。独立申请表可以保留完整轨迹，也能支持待审核、已通过、已驳回、已撤销等状态流转。'))
docParts.push(heading(4, '3.1.4 公告、日志与数据维护'))
docParts.push(bullet('前端设计：公告发布页面支持填写标题、正文和投递范围；操作日志页面支持按操作人、操作类型和时间查询；数据交换页面支持批量导入学生、教师和培养方案等数据。'))
docParts.push(bullet('后端设计：公告发布后，后端根据投递范围生成对应用户的通知记录；关键管理操作通过日志切面或服务层写入 operation_log；批量导入时逐行校验格式并返回成功和失败数量。'))
docParts.push(bullet('数据库设计：公告正文保存在 announcement，用户消息保存在 notification，管理员关键操作写入 operation_log。'))
docParts.push(bullet('设计原因：公告和通知解决消息触达问题，操作日志解决问题追溯问题，批量导入解决实际教学数据量大、手动录入效率低的问题。'))
docParts.push(heading(3, '3.2 教师端功能设计'))
docParts.push(paragraph('教师端围绕本人教学班展开，重点是解决教师日常教学中的“看课表、看学生、录成绩、提申请、看通知”几个高频问题。教师端的权限边界来自 teaching_class.teacher_id，所有关键数据都必须校验是否属于当前教师。'))
docParts.push(heading(4, '3.2.1 授课课表与教学班详情'))
docParts.push(bullet('前端设计：授课管理页面采用周课表网格，横向为星期，纵向为节次。课程卡片展示课程名称、节次、周次、单双周、教室和选课人数。点击课程卡片后打开详情抽屉，展示课程信息、成绩比例和学生名单。'))
docParts.push(bullet('后端设计：教学班列表接口默认只返回当前学期，也支持教师课表页传入参数查询全部学期。详情接口会再次校验教学班是否属于当前教师。'))
docParts.push(bullet('数据库设计：课表数据来自 teaching_class、teaching_class_session、course、semester、classroom。学生名单通过 course_selection 和 student 联查，只展示 selection_status 为已选的学生。'))
docParts.push(bullet('设计原因：教师最关心的是“什么时候、在哪里、给哪些学生上课”。用课表网格展示比普通表格更符合教师使用习惯，多学期切换则方便查看历史和未来安排。'))
docParts.push(heading(4, '3.2.2 成绩录入与成绩修改'))
docParts.push(bullet('前端设计：成绩页面采用可编辑表格，教师录入平时成绩和考试成绩。页面提供暂存、提交、批量暂存、批量提交、导出、导入、统计卡片和绩点分布。已提交成绩只读，并提供成绩修改申请入口。'))
docParts.push(bullet('后端设计：保存成绩时校验教学班归属；正式提交时额外校验当前时间是否处于 semester.score_submit_start 和 score_submit_end 之间。批量导入复用批量保存接口，避免新增复杂上传流程。'))
docParts.push(bullet('数据库设计：成绩写入 score，触发器自动计算 final_score 和 gpa_point。成绩修改申请写入 score_change_request，管理员审核通过后将成绩退回未提交状态。'))
docParts.push(bullet('设计原因：成绩是高风险数据，系统允许暂存提高录入灵活性，但正式提交必须受时间窗口约束。提交后通过申请流程修改，保证成绩变更有审核记录。'))
docParts.push(heading(4, '3.2.3 调课与考试修改申请'))
docParts.push(bullet('前端设计：调课页面选择教学班后自动带出原星期、节次、周次和教室，教师只填写申请后的时间、教室和原因。考试修改页面展示当前考试安排，并按申请后日期和时间动态加载可用教室。'))
docParts.push(bullet('后端设计：调课申请校验周次不变、节次跨度一致、目标教室空闲和教师本人时间不冲突。考试修改申请校验同一考试不能重复提交待审核申请，目标教室和教师考试时间不能冲突。'))
docParts.push(bullet('数据库设计：调课申请写入 schedule_adjust_request，考试修改申请写入 exam_change_request。申请表保存原安排和申请后安排，审核通过后才更新 teaching_class_session 或 exam_arrangement。'))
docParts.push(bullet('设计原因：调课和考试修改会影响学生、教师和教室资源，不能由教师直接修改主表。通过申请表隔离未审核变更，可以减少误操作和冲突。'))
docParts.push(heading(4, '3.2.4 退课审核、补考确认与通知'))
docParts.push(bullet('前端设计：退课审核页面展示学生、课程、退课原因和当前状态；补考确认页面展示补考申请和安排信息；通知中心展示公告、审批结果和系统提醒。'))
docParts.push(bullet('后端设计：教师只能审核本人教学班相关退课申请。补考确认根据教师所授课程筛选数据。通知接口按当前登录用户查询 notification，并支持标记已读。'))
docParts.push(bullet('数据库设计：退课申请来自 course_drop_request，补考申请来自 makeup_exam_request，通知来自 notification。退课最终通过后更新 course_selection 状态并清理对应 score。'))
docParts.push(bullet('设计原因：退课和补考都需要教师参与确认，通知中心则把审批结果集中展示，避免教师反复进入不同业务页面查看状态。'))
docParts.push(heading(3, '3.3 学生端功能设计'))
docParts.push(paragraph('学生端负责学习过程中的自助操作和查询。学生只能访问本人 student_id 关联的数据，主要功能包括选课、课表、成绩、考试、申请、培养方案进度和个人资料维护。'))
docParts.push(heading(4, '3.3.1 选课、补选与退课'))
docParts.push(bullet('前端设计：可选课程页面展示课程名称、教师、上课时间、教室、容量、已选人数和是否已选。学生可以直接选课，也可以在无法直接选课时提交补选申请。已选课程支持提交退课申请。'))
docParts.push(bullet('后端设计：直接选课调用 sp_select_course 存储过程，统一完成选课时间、容量、重复选课和时间冲突检查。补选和退课不直接改选课结果，而是先写入申请表。'))
docParts.push(bullet('数据库设计：直接选课写入 course_selection，补选申请写入 course_selection_request，退课申请写入 course_drop_request。退课通过后不删除选课记录，而是把 selection_status 改为已退。'))
docParts.push(bullet('设计原因：选课对容量和时间冲突要求严格，适合封装为数据库过程。补选和退课涉及审核，因此必须通过申请表保留过程。'))
docParts.push(heading(4, '3.3.2 课表、成绩与考试查询'))
docParts.push(bullet('前端设计：课表页面用周视图展示已选课程；成绩页面按学期展示课程成绩、总评、绩点和等级；考试页面展示考试日期、时间和地点。'))
docParts.push(bullet('后端设计：课表和成绩接口以 student_id 为条件查询，不允许传入其它学生编号越权访问。成绩复核申请接口要求学生只能对本人已提交成绩发起申请。'))
docParts.push(bullet('数据库设计：课表优先读取 v_student_timetable，成绩读取 v_student_score，考试安排通过 course_selection、teaching_class 和 exam_arrangement 联查。'))
docParts.push(bullet('设计原因：课表、成绩和考试查询都涉及多表连接，用视图封装可以降低后端重复 SQL，也使查询结果字段更稳定。'))
docParts.push(heading(4, '3.3.3 培养方案、补考与重修'))
docParts.push(bullet('前端设计：培养方案页面按课程类型展示要求学分、已修学分和缺口。补考和重修页面展示可申请课程、申请原因、审核状态和安排结果。'))
docParts.push(bullet('后端设计：培养方案接口根据学生入学年份和专业查询培养方案要求，再结合已提交成绩统计已修学分。补考和重修接口根据成绩状态判断是否允许申请。'))
docParts.push(bullet('数据库设计：培养方案要求保存在 cultivation_plan_requirement，学生课程计划保存在 student_course_plan，补考申请保存在 makeup_exam_request，重修申请保存在 course_retake_request。'))
docParts.push(bullet('设计原因：学生不只需要完成单次选课，还需要长期了解毕业要求完成情况。培养方案和课程计划提供长期学业视角，补考和重修则覆盖课程未通过后的补救流程。'))
docParts.push(heading(4, '3.3.4 学生个人信息与通知'))
docParts.push(bullet('前端设计：学生可以查看学籍资料并维护手机号、邮箱等联系方式；通知中心展示公告和审批结果。'))
docParts.push(bullet('后端设计：个人资料接口只允许修改联系信息，不允许学生自行修改院系、专业、班级、入学年份等学籍字段。'))
docParts.push(bullet('数据库设计：学生基础资料保存在 student，登录账号保存在 sys_user，通知保存在 notification。'))
docParts.push(bullet('设计原因：联系方式属于学生可维护信息，而学籍归属属于管理信息，必须由管理员维护。这样可以兼顾信息准确性和学生自助维护需求。'))
docParts.push(heading(3, '3.4 系统支撑功能设计'))
docParts.push(paragraph('系统支撑功能贯穿所有角色，主要包括登录认证、角色权限、通知公告、操作日志、数据导入导出、云端数据库部署和安全备份。'))
docParts.push(heading(4, '3.4.1 用户登录与角色权限'))
docParts.push(bullet('前端设计：登录成功后根据用户角色加载不同菜单。管理员、教师和学生看到的导航不同，避免普通用户进入不属于自己的业务页面。'))
docParts.push(bullet('后端设计：登录接口校验 sys_user，角色信息来自 sys_role 和 sys_user_role。教师和学生接口进一步通过 ref_id 绑定教师工号或学生学号。'))
docParts.push(bullet('数据库设计：sys_user 保存账号，sys_role 保存角色，sys_user_role 保存用户和角色的多对多关系。'))
docParts.push(bullet('设计原因：系统存在三类角色，权限边界差异很大。用用户表加角色表可以支持后续扩展更多角色。'))
docParts.push(heading(4, '3.4.2 通知公告与操作日志'))
docParts.push(bullet('前端设计：通知中心展示未读和已读消息，公告内容通过弹窗或详情页查看。操作日志页面支持管理员按条件查询。'))
docParts.push(bullet('后端设计：公告发布后按投递范围生成通知记录。审批通过或驳回后，也可以生成关联业务通知。关键操作写入日志，记录操作人、类型、内容和时间。'))
docParts.push(bullet('数据库设计：announcement 保存公告，notification 保存用户消息，operation_log 保存操作轨迹。'))
docParts.push(bullet('设计原因：通知保证消息能到达用户，日志保证管理动作可追踪，二者共同提升系统可维护性。'))
docParts.push(heading(4, '3.4.3 数据导入导出与删除保护'))
docParts.push(bullet('前端设计：管理员可以批量导入学生、教师和培养方案数据；教师成绩页面支持导出 CSV/XLSX 后离线填写再导入。删除操作使用二次确认和删除前检查。'))
docParts.push(bullet('后端设计：导入接口逐行校验数据合法性，返回成功、失败和错误原因。删除前检查接口统计关联记录，存在关联时阻止删除。'))
docParts.push(bullet('数据库设计：批量导入最终仍写入标准业务表，不额外建立临时业务表。删除保护依赖外键关系和服务层关联检查共同完成。'))
docParts.push(bullet('设计原因：教学系统数据量较大，必须支持批量维护；同时基础数据一旦被误删，会影响选课、成绩和审批，因此必须提供删除保护。'))
docParts.push(heading(4, '3.4.4 云端部署与数据安全'))
docParts.push(bullet('前端设计：前端通过统一 API 地址访问后端，不直接连接数据库。这样前端部署位置可以独立变化，例如本机、校园服务器或云服务器。'))
docParts.push(bullet('后端设计：后端通过配置文件维护数据库连接地址、用户名和密码。部署到云端时，只需要切换数据库连接配置即可连接云数据库。'))
docParts.push(bullet('数据库设计：MySQL 可以部署在云服务器自建实例，也可以使用云数据库 RDS。云端数据库需要配置安全组、白名单、定时备份和账号权限，避免数据库直接暴露在公网。'))
docParts.push(bullet('设计原因：数据库是系统最核心的资产，包含学生成绩、选课记录和审批记录。云端部署便于集中管理和备份，但必须同时考虑访问控制和数据安全。'))

docParts.push(heading(2, '四、数据库设计'))
docParts.push(heading(3, '4.1 概念设计'))
docParts.push(paragraph('概念设计阶段抽象出院系、专业、行政班、学生、教师、用户、角色、学期、课程、教室、教学班、上课时间、选课、成绩、考试安排、申请、公告、通知和操作日志等实体。教学班是系统的核心实体，连接课程、教师、学期、教室、学生选课、成绩和考试安排。'))
docParts.push(paragraph('为了避免单张 ER 图过大，报告采用分层方式展示。第一张图展示核心教学关系，第二张图展示申请审批关系，第三张图展示用户权限和通知支撑关系。'))
docParts.push(image('rId2', '图2 核心教学关系 ER 图', IMAGE_W, Math.round(IMAGE_W * 680 / 900)))
docParts.push(image('rId3', '图3 申请审批关系 ER 图', IMAGE_W, Math.round(IMAGE_W * 680 / 900)))
docParts.push(image('rId4', '图4 用户权限与通知支撑 ER 图', IMAGE_W, Math.round(IMAGE_W * 620 / 900)))

docParts.push(heading(3, '4.2 逻辑设计'))
docParts.push(paragraph('逻辑设计采用关系模型。学生、教师、课程、院系、专业、行政班、教室使用业务编号作为主键；学期、教学班、选课、成绩、考试、申请、通知和日志使用自增主键。系统设置外键维护引用完整性，并在选课、成绩、考试安排等表上设置唯一约束，避免重复记录。'))
docParts.push(paragraph('逻辑设计的核心思路是把稳定的基础资料、可反复发生的业务记录、需要审核流转的申请记录分开建模。基础资料表主要保存相对稳定的对象，例如学生、教师、课程、教室；业务记录表保存教学运行过程中持续产生的数据，例如教学班、选课、成绩和考试；申请审批表保存流程状态、原始值、申请值和审核信息。这样设计可以降低表之间的耦合，也便于后续分别扩展基础维护、教学运行和审批流程。'))
docParts.push(paragraph('教学班采用主表加明细表的方式设计。teaching_class 保存一个教学班的课程、教师、学期、教室、容量和周次，teaching_class_session 保存星期、节次和单双周。这样比把多个星期和节次直接塞进教学班主表更规范，也能支持一门课一周多次上课。成绩表和选课表都围绕教学班建立，保证课表、成绩、考试和申请都能回到同一个教学实体上。'))
docParts.push(table('表1 主要数据表清单', ['类别', '表名', '说明'], [
  ['基础数据', 'department、major、class_info、student、teacher', '院系、专业、班级、学生、教师资料'],
  ['权限支撑', 'sys_user、sys_role、sys_user_role', '登录账号和角色关系'],
  ['教学资源', 'semester、course、classroom、teaching_class、teaching_class_session', '学期、课程、教室、教学班和上课时间'],
  ['学习过程', 'course_selection、student_course_plan、score、exam_arrangement', '选课、计划、成绩和考试'],
  ['申请审批', 'course_selection_request、course_drop_request、schedule_adjust_request、score_change_request、score_review_request、exam_change_request、makeup_exam_request、course_retake_request', '各类跨角色申请'],
  ['通知日志', 'announcement、notification、operation_log', '公告、通知和操作日志']
]))

let tableNo = 2
for (const [name, rows] of Object.entries(tableRows)) {
  docParts.push(table(`表${tableNo++} ${name} 表结构`, ['字段名', '类型长度', '约束', '说明'], rows))
}
docParts.push(table(`表${tableNo++} 申请审批表结构摘要`, ['表名', '主键', '主要字段', '作用'], [
  ['course_selection_request', 'request_id BIGINT', 'student_id、teaching_class_id、request_reason、request_status、reviewed_by', '补选申请'],
  ['course_drop_request', 'drop_request_id BIGINT', 'student_id、teaching_class_id、teacher_status、admin_status', '退课申请'],
  ['schedule_adjust_request', 'adjust_request_id BIGINT', 'teaching_class_id、teacher_id、original_section_desc、requested_section_desc', '调课申请'],
  ['score_change_request', 'score_change_request_id BIGINT', 'score_id、teacher_id、original_usual_score、original_exam_score', '教师成绩修改申请'],
  ['score_review_request', 'request_id BIGINT', 'student_id、score_id、request_reason、request_status', '学生成绩复核申请'],
  ['exam_change_request', 'exam_change_request_id BIGINT', 'exam_id、teacher_id、requested_exam_date、requested_start_time', '考试修改申请'],
  ['makeup_exam_request', 'request_id BIGINT', 'student_id、score_id、teacher_status、admin_status、classroom_id', '补考申请'],
  ['course_retake_request', 'request_id BIGINT', 'student_id、score_id、request_status', '重修申请']
]))
docParts.push(table(`表${tableNo++} 支撑表结构摘要`, ['表名', '字段设计', '作用'], [
  ['announcement', 'announcement_id BIGINT、title VARCHAR(100)、content VARCHAR(1000)、target_scope VARCHAR(20)、published_by INT', '公告发布'],
  ['notification', 'notification_id BIGINT、user_id INT、category VARCHAR(20)、title VARCHAR(100)、is_read TINYINT、ref_type VARCHAR(30)', '站内通知'],
  ['operation_log', 'log_id BIGINT、user_id INT、operation_type VARCHAR(30)、operation_content VARCHAR(200)、operation_time DATETIME', '操作日志'],
  ['student_course_plan', 'plan_id BIGINT、student_id VARCHAR(20)、teaching_class_id INT、created_at DATETIME', '学生课程计划'],
  ['cultivation_plan_requirement', 'requirement_id BIGINT、enrollment_year INT、major_id VARCHAR(20)、course_type VARCHAR(50)、required_credit DECIMAL(5,1)', '培养方案要求']
]))

docParts.push(heading(3, '4.3 物理设计'))
docParts.push(paragraph('数据库采用 MySQL InnoDB 引擎和 utf8mb4 字符集。InnoDB 支持事务和外键，适合选课、成绩、审批等需要一致性控制的业务。业务编号字段使用 VARCHAR，便于保留学校现有编码规则；流程型数据使用 BIGINT 或 INT 自增主键，便于扩展和关联。'))
docParts.push(paragraph('物理设计阶段需要把逻辑模型落实到具体数据类型、约束、索引、触发器和存储过程。系统中姓名、编号、状态等字段使用 VARCHAR 或 CHAR；日期和时间分别使用 DATE、TIME、DATETIME；成绩和学分使用 DECIMAL，避免浮点误差；是否当前学期、是否提交、是否必修等布尔型状态使用 TINYINT。'))
docParts.push(paragraph('部署层面，数据库既可以部署在本机 MySQL，也可以迁移到云端关系型数据库实例，例如云服务器自建 MySQL 或云数据库 RDS。云端数据库的价值在于数据集中保存、便于备份、便于多终端访问，也方便前后端部署在不同机器时仍能共享同一份教学数据。实际部署时需要限制数据库公网访问，优先使用内网连接或安全组白名单，并定期备份 school 数据库，保证学生成绩、选课记录和审批记录不会因单机故障丢失。'))
docParts.push(codeBlock('表' + tableNo++ + ' 建库语句示例', [
  'DROP DATABASE IF EXISTS school;',
  'CREATE DATABASE school DEFAULT CHARACTER SET utf8mb4;',
  'USE school;',
  'SET NAMES utf8mb4;'
]))
docParts.push(heading(4, '4.3.1 约束与索引设计'))
docParts.push(paragraph('系统通过主键、外键、唯一约束和普通索引保证数据完整性。选课表对学生和教学班建立唯一约束，成绩表对学生和教学班建立唯一约束，考试安排表对教学班建立唯一约束。申请表按学生、教师、状态等字段建立索引，提升待审核列表和历史查询效率。'))
docParts.push(paragraph('外键约束用于表达实体之间的从属关系，例如学生必须归属于已有院系、专业和行政班，教学班必须关联已有学期、课程和教师。唯一约束用于表达业务上的“一次性关系”，例如同一学生不能重复选择同一教学班，一个学生在同一教学班只能有一条成绩，一个教学班只能有一条考试安排。'))
docParts.push(codeBlock('表' + tableNo++ + ' 建表语句示例：教学班表', [
  'CREATE TABLE teaching_class (',
  '  teaching_class_id INT NOT NULL AUTO_INCREMENT COMMENT "教学班编号",',
  '  semester_id INT NOT NULL COMMENT "学期编号",',
  '  course_id VARCHAR(12) NOT NULL COMMENT "课程编号",',
  '  teacher_id VARCHAR(12) NOT NULL COMMENT "任课教师工号",',
  '  classroom_id VARCHAR(10) NULL COMMENT "教室编号",',
  '  week_range VARCHAR(30) NOT NULL COMMENT "周次范围",',
  '  max_students INT NOT NULL DEFAULT 60 COMMENT "容量上限",',
  '  selected_count INT NOT NULL DEFAULT 0 COMMENT "已选人数",',
  '  status VARCHAR(20) NOT NULL DEFAULT "未开始" COMMENT "教学班状态",',
  '  PRIMARY KEY (teaching_class_id),',
  '  CONSTRAINT fk_teaching_class_semester FOREIGN KEY (semester_id) REFERENCES semester(semester_id),',
  '  CONSTRAINT fk_teaching_class_course FOREIGN KEY (course_id) REFERENCES course(course_id),',
  '  CONSTRAINT fk_teaching_class_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id)',
  ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT="教学班表";'
]))
docParts.push(codeBlock('表' + tableNo++ + ' 唯一约束语句示例', [
  'CREATE TABLE course_selection (',
  '  selection_id BIGINT NOT NULL AUTO_INCREMENT COMMENT "选课记录编号",',
  '  student_id VARCHAR(12) NOT NULL COMMENT "学号",',
  '  teaching_class_id INT NOT NULL COMMENT "教学班编号",',
  '  selection_status VARCHAR(20) NOT NULL DEFAULT "已选" COMMENT "选课状态",',
  '  PRIMARY KEY (selection_id),',
  '  UNIQUE KEY uk_student_teaching_class (student_id, teaching_class_id)',
  ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT="选课表";'
]))
docParts.push(heading(4, '4.3.2 触发器设计'))
docParts.push(paragraph('成绩表设置插入和更新前触发器。触发器先校验平时成绩和考试成绩是否在 0 到 100 之间，当两项成绩都存在时，读取课程表中的平时成绩占比和考试成绩占比，计算总评成绩和绩点。这样可以避免前端、后端和数据库三处计算规则不一致。'))
docParts.push(paragraph('触发器适合处理与单表写入强相关、必须保持一致的派生字段。本系统中的 final_score 和 gpa_point 不由用户手工填写，而是在 usual_score 和 exam_score 写入时自动推导。这样可以避免教师导入成绩时误填总评成绩，也能保证学生端、教师端和管理员端看到的总评规则一致。'))
docParts.push(codeBlock('表' + tableNo++ + ' 触发器语句示例：成绩计算', [
  'CREATE TRIGGER trg_score_before_insert',
  'BEFORE INSERT ON score',
  'FOR EACH ROW',
  'BEGIN',
  '  IF NEW.usual_score IS NOT NULL AND (NEW.usual_score < 0 OR NEW.usual_score > 100) THEN',
  '    SIGNAL SQLSTATE "45000" SET MESSAGE_TEXT = "平时成绩必须在0到100之间";',
  '  END IF;',
  '  IF NEW.exam_score IS NOT NULL AND (NEW.exam_score < 0 OR NEW.exam_score > 100) THEN',
  '    SIGNAL SQLSTATE "45000" SET MESSAGE_TEXT = "考试成绩必须在0到100之间";',
  '  END IF;',
  '  IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN',
  '    SET NEW.final_score = ROUND(NEW.usual_score * 0.4 + NEW.exam_score * 0.6, 2);',
  '    SET NEW.gpa_point = CASE WHEN NEW.final_score >= 90 THEN 4.0 WHEN NEW.final_score >= 60 THEN 1.0 ELSE 0.0 END;',
  '  END IF;',
  'END;'
]))
docParts.push(heading(4, '4.3.3 存储过程设计'))
docParts.push(paragraph('选课存储过程 sp_select_course 负责检查选课时间窗口、手动开启或关闭状态、重复选课、教学班容量和课程时间冲突。若学生曾退选同一教学班，过程会恢复原记录并清理旧成绩；否则新增选课记录并同步更新教学班已选人数。'))
docParts.push(paragraph('选课流程放到存储过程中的原因是该流程同时涉及读取教学班容量、读取学期选课时间、检查已有选课、判断课程时间冲突、写入选课记录、更新教学班人数和写入操作日志。把这些步骤放在一个数据库过程内，可以降低中间状态不一致的风险。'))
docParts.push(codeBlock('表' + tableNo++ + ' 存储过程语句示例：选课流程片段', [
  'CREATE PROCEDURE sp_select_course (IN p_student_id VARCHAR(12), IN p_teaching_class_id INT)',
  'BEGIN',
  '  DECLARE v_max_students INT;',
  '  DECLARE v_selected_count INT;',
  '  SELECT max_students, selected_count INTO v_max_students, v_selected_count',
  '  FROM teaching_class WHERE teaching_class_id = p_teaching_class_id;',
  '  IF v_selected_count >= v_max_students THEN',
  '    SIGNAL SQLSTATE "45000" SET MESSAGE_TEXT = "该教学班人数已满";',
  '  END IF;',
  '  INSERT INTO course_selection (student_id, teaching_class_id, selection_status)',
  '  VALUES (p_student_id, p_teaching_class_id, "已选");',
  '  UPDATE teaching_class SET selected_count = selected_count + 1',
  '  WHERE teaching_class_id = p_teaching_class_id;',
  'END;'
]))
docParts.push(heading(4, '4.3.4 视图设计'))
docParts.push(paragraph('系统创建 v_student_timetable 和 v_student_score 两个视图。前者用于学生课表查询，后者用于学生成绩查询和管理员成绩明细查询。视图封装多表连接，减少接口层重复 SQL。'))
docParts.push(codeBlock('表' + tableNo++ + ' 视图语句示例：学生课表', [
  'CREATE OR REPLACE VIEW v_student_timetable AS',
  'SELECT cs.student_id, sem.semester_name, c.course_name, t.teacher_name,',
  '       sess.week_day, sess.section_desc, sess.week_parity,',
  '       tc.week_range, cl.building_name, cl.room_name',
  'FROM course_selection cs',
  'JOIN teaching_class tc ON cs.teaching_class_id = tc.teaching_class_id',
  'JOIN teaching_class_session sess ON sess.teaching_class_id = tc.teaching_class_id',
  'JOIN semester sem ON tc.semester_id = sem.semester_id',
  'JOIN course c ON tc.course_id = c.course_id',
  'JOIN teacher t ON tc.teacher_id = t.teacher_id',
  'LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id',
  'WHERE cs.selection_status = "已选";'
]))
docParts.push(heading(4, '4.3.5 主要功能的详细实现'))
docParts.push(paragraph('基础数据管理负责维护院系、专业、班级、学生、教师、课程、教室和学期。删除操作前会检查关联数据，避免误删已被选课、成绩、考试或申请引用的数据。'))
docParts.push(paragraph('基础数据管理之所以单独设计，是因为它是后续业务的前置条件。没有院系、专业和班级，就无法维护学生；没有课程、教师、学期和教室，就无法创建教学班；没有教学班，就无法产生选课、成绩和考试。因此基础数据表是整个系统的第一层数据。'))
docParts.push(paragraph('教学班管理将教学班主信息和上课时间拆分存储。teaching_class 记录课程、教师、教室、学期、容量和周次，teaching_class_session 记录星期、节次和单双周。系统按同学期、同教师、同教室、节次区间和单双周相交规则检查冲突。'))
docParts.push(paragraph('教学班这样设计的原因是高校排课并不总是一门课一个时间段。四学分课程可能每周两次课，三学分课程可能出现单双周课。如果只在教学班表中放一个 week_day 和 section_desc，就无法表达这种情况。拆出 teaching_class_session 后，排课结构更加灵活，冲突检测也可以按每个时间块精确判断。'))
docParts.push(paragraph('学生选课基于选课存储过程完成。选课成功写入 course_selection，退课不删除记录而是将状态改为“已退”，便于保留历史和支持重新加入。'))
docParts.push(paragraph('选课功能的完整链路是：前端展示可选教学班，后端根据当前学期和选课窗口过滤课程，数据库存储过程检查容量和冲突，选课成功后更新 course_selection 和 teaching_class.selected_count，学生课表视图再读取新的选课结果。这个链路保证了“能选什么、能否选、选完显示在哪里”三件事一致。'))
docParts.push(paragraph('教师成绩录入使用 score 表保存平时成绩、考试成绩、总评成绩、绩点和提交状态。正式提交成绩必须处于学期成绩提交窗口内；已提交成绩如需修改，必须走成绩修改申请。'))
docParts.push(paragraph('成绩功能的完整链路是：教师在页面录入平时和考试成绩，后端校验教学班归属和提交时间，数据库触发器计算总评和绩点，提交后 is_submitted 变为 1，学生端成绩视图只展示已提交成绩。若后续需要修改，则通过 score_change_request 保留申请原因和审核轨迹。'))
docParts.push(paragraph('考试安排由 exam_arrangement 表保存。教师提交考试修改申请时，系统会检查目标教室在申请后时间段是否占用，并检查同一教师是否存在考试时间冲突。管理员审核通过后更新考试安排。'))
docParts.push(paragraph('考试功能的完整链路是：管理员根据当前学期教学班生成考试安排，考试安排关联教学班和教室；教师查看本人课程考试，若时间或地点不合适则提交考试修改申请；管理员审核时再次检查教室和教师时间冲突；审核通过后更新 exam_arrangement，并通过通知表向相关用户反馈。'))
docParts.push(paragraph('申请审批通过独立申请表保存原始数据、目标数据、申请原因、审核状态、审核意见、审核人和审核时间。该设计保证审批过程可追踪，也避免直接修改业务主表导致历史丢失。'))
docParts.push(paragraph('审批功能的设计原则是“不直接改结果，先记录申请”。例如调课申请先保存原星期、原节次、申请后星期和申请后节次，只有管理员审核通过后才更新教学班排课。这样即使申请被驳回，系统也能保留教师曾经申请过什么、管理员为什么驳回。'))
docParts.push(paragraph('通知公告通过 announcement 和 notification 实现。公告保存正文和投递范围，通知保存接收用户、通知类别、已读状态和关联业务编号。操作日志记录关键管理动作。'))
docParts.push(paragraph('通知和日志是全链路闭环的最后一步。审批结果如果只改变申请表状态，用户必须反复进入业务页面查看；引入 notification 后，系统可以主动把结果推送到用户通知中心。operation_log 则记录管理员关键动作，便于问题排查和责任追踪。'))

docParts.push(heading(2, '五、效果演示'))
docParts.push(paragraph('系统实际运行后，管理员可以维护基础数据、设置学期、创建教学班、处理审批并发布公告；学生可以查询课程、完成选课、查看课表成绩并提交申请；教师可以查看授课课表、录入成绩、处理退课和提交调课或考试修改申请。'))
docParts.push(table(`表${tableNo++} 功能效果演示摘要`, ['角色', '页面或功能', '演示效果'], [
  ['管理员', '学期与教学班管理', '创建学期后自动推导教学周、考试周和选课时间；教学班支持排课和冲突检测'],
  ['学生', '选课与课表', '学生在选课时间内选择课程，系统检查容量和时间冲突，选课后课表实时展示'],
  ['教师', '授课课表与成绩录入', '教师按学期查看课表，录入成绩后系统自动计算总评和绩点'],
  ['审批', '退课、调课、成绩和考试申请', '申请状态从待审核流转到已通过、已驳回或已撤销，并可通过通知查看结果'],
  ['系统', '公告与操作日志', '公告按范围投递到用户通知，管理员关键操作写入日志']
]))

docParts.push(heading(2, '六、总结和评价'))
docParts.push(heading(3, '6.1 项目总结'))
docParts.push(paragraph('本系统围绕高校教学事务建立了较完整的数据模型和业务闭环。数据库层面既包括基础资料和教学资源，也包括选课、成绩、考试、申请、通知和日志等过程数据。系统利用主外键、唯一约束、触发器、存储过程和视图提升数据一致性和业务可靠性。'))
docParts.push(heading(3, '6.2 体会'))
docParts.push(paragraph('第一，概念模型必须先抓住核心业务对象。教学事务系统中，教学班是连接课程、教师、学期、教室、学生和成绩的核心。如果教学班建模不清晰，后续课表、成绩和考试都会出现重复字段和数据不一致。'))
docParts.push(paragraph('第二，逻辑设计要兼顾规范化和查询便利。将上课时间拆分到 teaching_class_session 后，一门课可以拥有多个上课时间，也可以表达单双周排课，避免教学班主表变得臃肿。'))
docParts.push(paragraph('第三，数据库应承担关键规则的一部分。成绩计算放到触发器中，选课流程放到存储过程中，可以减少不同层重复实现规则造成的误差。'))
docParts.push(paragraph('第四，流程型业务需要保留状态和历史。退课不直接删除选课记录，成绩提交后不能直接修改，而是通过申请表和状态字段保留审核轨迹。'))
docParts.push(paragraph('第五，页面展示必须与业务语义一致。例如教师课表中的“当前、已结束、未开始”应描述学期状态，而不是选课窗口状态。数据库字段、后端业务和前端展示之间需要明确区分概念。'))
docParts.push(heading(3, '6.3 不足与改进方向'))
docParts.push(paragraph('当前系统仍有进一步优化空间。后续可以增加更细粒度的数据权限、更多自动化排课策略、审批流配置、统计报表和数据库性能监控。同时，报告中的 ER 图采用分层展示，适合阅读；实际开发文档中还可以补充完整数据字典和索引清单。'))

const body = docParts.join('')
const documentXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"><w:body>${body}<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="851" w:footer="992" w:gutter="0"/></w:sectPr></w:body></w:document>`

const stylesXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:rFonts w:ascii="Times New Roman" w:eastAsia="宋体" w:hAnsi="Times New Roman"/><w:sz w:val="21"/></w:rPr><w:pPr><w:spacing w:after="160" w:line="283" w:lineRule="auto"/><w:ind w:firstLineChars="200"/></w:pPr></w:style><w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:pPr><w:ind w:left="0" w:firstLine="0" w:firstLineChars="0"/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体"/><w:b/><w:sz w:val="32"/></w:rPr></w:style><w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:pPr><w:ind w:left="0" w:firstLine="0" w:firstLineChars="0"/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体"/><w:b/><w:sz w:val="28"/></w:rPr></w:style><w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:pPr><w:ind w:left="0" w:firstLine="0" w:firstLineChars="0"/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体"/><w:b/><w:sz w:val="24"/></w:rPr></w:style><w:style w:type="paragraph" w:styleId="Heading4"><w:name w:val="heading 4"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:pPr><w:ind w:left="0" w:firstLine="0" w:firstLineChars="0"/></w:pPr><w:rPr><w:rFonts w:eastAsia="黑体"/><w:b/><w:sz w:val="21"/></w:rPr></w:style></w:styles>`

const contentTypes = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="svg" ContentType="image/svg+xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/></Types>`
const rootRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>`
const docRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/function.svg"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/er-core.svg"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/er-approval.svg"/><Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/er-support.svg"/><Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>`

const files = [
  { name: '[Content_Types].xml', data: contentTypes },
  { name: '_rels/.rels', data: rootRels },
  { name: 'word/document.xml', data: documentXml },
  { name: 'word/styles.xml', data: stylesXml },
  { name: 'word/_rels/document.xml.rels', data: docRels },
  { name: 'word/media/function.svg', data: svgFunction() },
  { name: 'word/media/er-core.svg', data: svgErCore() },
  { name: 'word/media/er-approval.svg', data: svgErApproval() },
  { name: 'word/media/er-support.svg', data: svgErSupport() }
]

fs.writeFileSync(outPath, createZip(files))
console.log(outPath)
