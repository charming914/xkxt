const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const outPath = path.join(root, 'docs', '教学事务管理系统教师端数据库报告-触发器更新.docx')
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
  return paragraph(text, { indent: true, spacingAfter: 80 })
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
  return `<w:p><w:pPr><w:jc w:val="center"/><w:ind w:left="0" w:firstLine="0" w:firstLineChars="0"/><w:spacing w:after="80"/></w:pPr><w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="${cx}" cy="${cy}"/><wp:docPr id="${relId.replace('rId', '')}" name="${caption}"/><a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="${relId.replace('rId', '')}" name="${caption}"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="${relId}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${cx}" cy="${cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>${paragraph(caption, { align: 'center', indent: false, spacingAfter: 160 })}`
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
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1260" height="720" viewBox="0 0 1260 720">
  <style>
    text{font-family:"Microsoft YaHei",Arial,sans-serif;fill:#1f2937}
    .title{fill:#2563eb;stroke:#1e40af;stroke-width:2}
    .box{fill:#f8fafc;stroke:#64748b;stroke-width:2;rx:10}
    .admin{fill:#e0f2fe;stroke:#0284c7}.student{fill:#dcfce7;stroke:#16a34a}.teacher{fill:#fef3c7;stroke:#d97706}.approval{fill:#f3e8ff;stroke:#9333ea}.support{fill:#fee2e2;stroke:#dc2626}
    .line{stroke:#64748b;stroke-width:2;fill:none}
    .h{font-weight:700;font-size:22px;fill:white}.t{font-weight:700;font-size:18px}.s{font-size:15px}
  </style>
  <rect class="title" x="475" y="28" width="310" height="70" rx="12"/><text class="h" x="630" y="58" text-anchor="middle">教学事务管理系统</text><text x="630" y="83" fill="#dbeafe" font-size="15" text-anchor="middle">Teaching Affairs Management System</text>
  <path class="line" d="M630 98V138H150M630 138H390M630 138H630M630 138H870M630 138H1110"/>
  ${column(65, 155, '基础数据管理', ['院系/专业/班级', '学生/教师/课程', '教室/学期', '删除保护'], 'admin')}
  ${column(305, 155, '学生选课与学业', ['可选课程查询', '选课/退课/补选', '课表/成绩/考试', '培养方案进度'], 'student')}
  ${column(545, 155, '教师教学事务', ['授课课表', '学生名单', '成绩录入提交', '调课/考试修改'], 'teacher')}
  ${column(785, 155, '审批与考试管理', ['退课/补选审核', '成绩修改/复核', '考试生成与调整', '补考/重修处理'], 'approval')}
  ${column(1025, 155, '系统支撑', ['用户角色权限', '通知公告', '操作日志', '数据导入导出'], 'support')}
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

function svgErTeacher() {
  const box = (name, x, y, w = 150, h = 54) =>
    `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="9" fill="#eef2ff" stroke="#4f46e5" stroke-width="2"/><text x="${x + w / 2}" y="${y + 34}" font-size="17" font-weight="700" text-anchor="middle">${escapeXml(name)}</text>`
  const line = (x1, y1, x2, y2) => `<path d="M${x1} ${y1} L${x2} ${y2}" stroke="#64748b" stroke-width="2" fill="none"/>`
  const label = (text, x, y) => `<rect x="${x - 18}" y="${y - 14}" width="36" height="18" rx="4" fill="white"/><text x="${x}" y="${y}" font-size="14" fill="#334155" text-anchor="middle">${escapeXml(text)}</text>`

  return `<svg xmlns="http://www.w3.org/2000/svg" width="1240" height="620" viewBox="0 0 1240 620">
    <rect width="1240" height="620" fill="white"/>
    <g font-family="Microsoft YaHei">
      ${box('教师', 80, 70)}
      ${box('课程', 300, 70)}
      ${box('学期', 520, 70)}
      ${box('教室', 740, 70)}

      ${box('教学班', 410, 235, 170, 60)}

      ${box('上课时间', 80, 430)}
      ${box('考试安排', 300, 430)}
      ${box('选课记录', 520, 430)}
      ${box('学生', 740, 430)}
      ${box('成绩', 960, 430)}

      ${line(230, 97, 410, 265)}${label('1:N', 300, 178)}
      ${line(375, 124, 440, 235)}${label('1:N', 405, 185)}
      ${line(595, 124, 530, 235)}${label('1:N', 565, 185)}
      ${line(815, 124, 580, 265)}${label('1:N', 715, 205)}

      ${line(410, 265, 230, 457)}${label('1:N', 310, 360)}
      ${line(445, 295, 375, 430)}${label('1:1', 405, 365)}
      ${line(530, 295, 595, 430)}${label('1:N', 570, 365)}
      ${line(670, 457, 740, 457)}${label('N:1', 705, 447)}
      ${line(815, 457, 960, 457)}${label('1:N', 890, 447)}
      ${line(580, 265, 1035, 430)}${label('1:N', 830, 350)}
    </g>
  </svg>`
}

function svgTeacherRequestEr() {
  const box = (name, x, y, w = 160, h = 54) =>
    `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="9" fill="#f8fafc" stroke="#0f766e" stroke-width="2"/><text x="${x + w / 2}" y="${y + 34}" font-size="17" font-weight="700" text-anchor="middle">${escapeXml(name)}</text>`
  const line = (x1, y1, x2, y2) => `<path d="M${x1} ${y1} L${x2} ${y2}" stroke="#64748b" stroke-width="2" fill="none"/>`
  const label = (text, x, y) => `<rect x="${x - 18}" y="${y - 14}" width="36" height="18" rx="4" fill="white"/><text x="${x}" y="${y}" font-size="14" fill="#334155" text-anchor="middle">${escapeXml(text)}</text>`

  return `<svg xmlns="http://www.w3.org/2000/svg" width="1240" height="620" viewBox="0 0 1240 620">
    <rect width="1240" height="620" fill="white"/>
    <g font-family="Microsoft YaHei">
      ${box('教师', 80, 75)}
      ${box('教学班', 330, 75)}
      ${box('成绩', 580, 75)}

      ${box('调课申请', 150, 270)}
      ${box('退课申请', 390, 270)}
      ${box('成绩修改申请', 630, 270)}
      ${box('考试安排', 870, 270)}

      ${box('补考申请', 390, 455)}
      ${box('通知', 630, 455)}
      ${box('考试修改申请', 870, 455)}

      ${line(240, 102, 330, 102)}${label('1:N', 285, 92)}
      ${line(490, 102, 580, 102)}${label('1:N', 535, 92)}
      ${line(490, 129, 950, 270)}${label('1:1', 705, 205)}

      ${line(410, 129, 230, 270)}${label('1:N', 300, 205)}
      ${line(410, 129, 470, 270)}${label('1:N', 455, 205)}
      ${line(660, 129, 710, 270)}${label('1:N', 700, 205)}
      ${line(950, 324, 950, 455)}${label('1:N', 982, 395)}

      ${line(660, 129, 470, 455)}${label('1:N', 535, 360)}
      ${line(710, 324, 710, 455)}${label('1:N', 742, 395)}
      ${line(950, 455, 710, 455)}${label('1:N', 835, 445)}
      ${line(470, 324, 710, 455)}${label('1:N', 585, 382)}
      ${line(230, 324, 710, 455)}${label('1:N', 455, 405)}
    </g>
  </svg>`
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
docParts.push(heading(1, '教学事务管理系统教师端数据库报告'))
docParts.push(paragraph('课程：《数据库原理(1)》', { align: 'center', indent: false }))
docParts.push(paragraph('专题：教师端业务与数据库设计', { align: 'center', indent: false }))
docParts.push(pageBreak())

docParts.push(heading(2, '一、项目背景'))
docParts.push(heading(3, '1.1 教师端业务背景'))
docParts.push(paragraph('教学事务管理系统面向管理员、教师和学生三类角色，其中教师端承担授课管理、学生名单查看、成绩录入、调课申请、考试修改申请、退课审核、补考确认和通知查看等工作。教师端虽然只是系统的一部分，但它直接连接教学班、学生、成绩、考试和申请审核流程，是教学事务从“排课”走向“教学执行”的关键环节。'))
docParts.push(paragraph('传统教师教学事务往往分散在纸质名单、Excel 成绩表、人工通知和线下审批中，容易出现成绩版本不一致、审批过程不可追踪、调课和考试时间冲突难以及时发现等问题。因此教师端报告重点讨论如何围绕教师工作流建立数据库模型，让课表、成绩、申请、通知等数据形成可查询、可校验、可追踪的闭环。'))
docParts.push(heading(3, '1.2 教师端建设目标'))
docParts.push(bullet('教师只能查看和操作本人教学班，保证教师端数据边界清晰。'))
docParts.push(bullet('教师可以按学期查看授课课表、教学班详情和学生名单。'))
docParts.push(bullet('成绩录入支持暂存、提交、批量导入导出，并由数据库触发器统一计算总评和绩点。'))
docParts.push(bullet('调课、考试修改、成绩修改等高风险操作必须通过申请表保存过程，由管理员审核后生效。'))
docParts.push(bullet('退课审核、补考确认和通知中心让教师能够参与跨角色事务闭环。'))

docParts.push(heading(2, '二、总体功能'))
docParts.push(paragraph('教师端属于教学事务管理系统总体功能的一部分。系统总体上仍包含基础数据管理、学生选课与学业服务、教师教学事务、审批与考试管理、系统支撑五类功能。保留总体功能图可以说明教师端与其它模块的关系，如图1所示。'))
docParts.push(image('rId1', '图1 教学事务管理系统总体功能设计图', IMAGE_W, Math.round(IMAGE_W * 720 / 1260)))
docParts.push(paragraph('在总体功能中，教师端主要位于“教师教学事务”模块，同时与“审批与考试管理”“系统支撑”紧密关联。教师端并不直接维护学期、课程、学生等全局数据，而是读取这些基础数据，并围绕本人教学班完成教学执行。'))

docParts.push(heading(2, '三、教师端功能设计'))
docParts.push(heading(3, '3.1 授课课表与教学班详情'))
docParts.push(heading(4, '3.1.1 前端设计'))
docParts.push(bullet('授课管理页面使用周课表网格展示教师课程。横向为星期，纵向为节次，课程卡片展示课程名称、节次、周次、单双周、教室和选课人数。'))
docParts.push(bullet('页面支持按学期切换课表，默认优先显示当前学期。课表状态只描述学期状态：当前学期显示“当前”，历史学期显示“已结束”，未来学期显示“未开始”。'))
docParts.push(bullet('点击课程卡片打开教学班详情抽屉，展示课程编号、课程类型、学分、考核方式、平时考试比例、教室、容量和学生名单。'))
docParts.push(heading(4, '3.1.2 后端设计'))
docParts.push(bullet('教师教学班列表接口默认只返回当前学期，也支持 includeAllSemesters 参数查询全部学期，供课表页切换学期。'))
docParts.push(bullet('后端根据当前登录用户关联的教师工号查询 teaching_class.teacher_id，保证教师只能看到本人教学班。'))
docParts.push(bullet('教学班详情和学生名单接口都会校验教学班归属，防止教师通过接口参数访问其它教师数据。'))
docParts.push(heading(4, '3.1.3 数据库落点'))
docParts.push(bullet('课表数据主要来自 teaching_class、teaching_class_session、course、semester、classroom。'))
docParts.push(bullet('学生名单通过 course_selection 和 student 联查，只展示 selection_status 为已选的学生。'))
docParts.push(bullet('教师端权限边界来自 teaching_class.teacher_id，不额外建立教师权限映射表。'))
docParts.push(heading(4, '3.1.4 设计原因'))
docParts.push(paragraph('教师最常用的操作是查看本人在某学期的课程安排，因此课表视图比普通列表更符合使用习惯。教学班作为核心实体，把课程、教师、学期、教室、学生名单、成绩和考试串在一起，教师端的大部分功能都从教学班出发。'))

docParts.push(heading(3, '3.2 成绩录入与成绩修改申请'))
docParts.push(heading(4, '3.2.1 前端设计'))
docParts.push(bullet('成绩页面采用可编辑表格，教师录入平时成绩和考试成绩。'))
docParts.push(bullet('页面支持单条暂存、单条提交、批量暂存、批量提交、CSV/XLSX 导出和离线导入。'))
docParts.push(bullet('页面展示统计卡片和绩点分布，方便教师在提交前检查平均分、最高分、最低分、及格率和成绩分布。'))
docParts.push(bullet('已提交成绩只读，如果需要修改，教师只能提交成绩修改申请。'))
docParts.push(heading(4, '3.2.2 后端设计'))
docParts.push(bullet('保存成绩时校验教学班是否属于当前教师。'))
docParts.push(bullet('暂存成绩不受成绩提交时间限制，正式提交成绩必须处于 semester.score_submit_start 和 semester.score_submit_end 范围内。'))
docParts.push(bullet('成绩修改申请保存原平时成绩、原考试成绩、原总评成绩、原绩点和申请原因，避免审核时原始数据丢失。'))
docParts.push(heading(4, '3.2.3 数据库落点'))
docParts.push(bullet('成绩写入 score。usual_score 和 exam_score 由教师录入，final_score 和 gpa_point 由数据库触发器计算。'))
docParts.push(bullet('成绩修改申请写入 score_change_request。管理员审核通过后，将对应 score 退回未提交状态，教师再重新修改。'))
docParts.push(heading(4, '3.2.4 设计原因'))
docParts.push(paragraph('成绩属于高风险数据。系统允许教师先暂存，降低一次性录入压力；正式提交受学期时间窗口控制，保证教学管理节奏；提交后不能直接修改，而是通过申请表保留修改原因和审核轨迹。'))

docParts.push(heading(3, '3.3 调课申请'))
docParts.push(heading(4, '3.3.1 前端设计'))
docParts.push(bullet('教师选择教学班后，页面自动带出原星期、原节次、原周次和原教室。'))
docParts.push(bullet('申请表限制周次不可修改，节次选择根据原节次跨度进行限制。目标教室根据申请后的星期和节次动态加载。'))
docParts.push(bullet('申请记录列表展示申请状态、申请内容、审核意见和撤销入口。待审核申请可以撤销。'))
docParts.push(heading(4, '3.3.2 后端设计'))
docParts.push(bullet('提交申请时校验教学班归属、周次不变、节次跨度一致。'))
docParts.push(bullet('后端检查目标教室在同学期、同星期、同节次、同单双周范围内是否被占用。'))
docParts.push(bullet('后端还检查教师本人在目标时间是否已有其它课程，避免同一教师课程冲突。'))
docParts.push(heading(4, '3.3.3 数据库落点'))
docParts.push(bullet('调课申请写入 schedule_adjust_request，保存原星期、原节次、原周次、原教室，以及申请后的星期、节次、周次和教室。'))
docParts.push(bullet('审核通过后才更新 teaching_class_session 和 teaching_class 的教室信息。'))
docParts.push(heading(4, '3.3.4 设计原因'))
docParts.push(paragraph('调课会影响学生课表、教师时间和教室占用，不能由教师直接修改主表。申请表将“教师提出变更”和“管理员确认生效”分离，既保留过程，也降低排课冲突风险。'))

docParts.push(heading(3, '3.4 考试安排与考试修改申请'))
docParts.push(heading(4, '3.4.1 前端设计'))
docParts.push(bullet('考试页面展示当前学期本人课程的考试日期、开始时间、结束时间、教室和考试状态。'))
docParts.push(bullet('教师可以提交考试时间或教室修改申请。选择申请后日期和时间后，教室下拉动态显示可用教室。'))
docParts.push(bullet('考试修改申请列表展示原安排、申请后安排、申请原因、审核状态和审核备注。'))
docParts.push(heading(4, '3.4.2 后端设计'))
docParts.push(bullet('后端校验考试所属教学班是否由当前教师授课。'))
docParts.push(bullet('同一考试存在待审核修改申请时不能重复提交。'))
docParts.push(bullet('提交申请和审核通过时均检查目标教室考试时间冲突和教师本人考试时间冲突。'))
docParts.push(heading(4, '3.4.3 数据库落点'))
docParts.push(bullet('考试安排保存在 exam_arrangement。'))
docParts.push(bullet('考试修改申请保存在 exam_change_request，包含原考试日期、原时间、原教室、申请后日期、申请后时间和申请后教室。'))
docParts.push(heading(4, '3.4.4 设计原因'))
docParts.push(paragraph('考试安排直接影响学生和教室资源，因此教师只能提出修改申请。系统用 exam_arrangement 表表示最终考试安排，用 exam_change_request 表保存尚未生效的变更请求。'))

docParts.push(heading(3, '3.5 退课审核、补考确认与通知'))
docParts.push(heading(4, '3.5.1 前端设计'))
docParts.push(bullet('退课审核页面展示学生、教学班、课程、退课原因、教师审核状态和管理员审核状态。'))
docParts.push(bullet('补考确认页面展示学生补考申请、课程、成绩、申请原因和安排信息。'))
docParts.push(bullet('通知中心展示公告、审批结果和系统消息，支持查看详情和标记已读。'))
docParts.push(heading(4, '3.5.2 后端设计'))
docParts.push(bullet('教师只能审核本人教学班相关退课申请。'))
docParts.push(bullet('退课最终通过后，系统更新 course_selection 状态并清理对应 score，避免退课学生继续保留课程成绩。'))
docParts.push(bullet('通知接口按当前登录用户查询 notification，避免教师查看其它用户消息。'))
docParts.push(heading(4, '3.5.3 数据库落点'))
docParts.push(bullet('退课申请来自 course_drop_request，补考申请来自 makeup_exam_request，通知来自 notification。'))
docParts.push(bullet('公告正文来自 announcement，通知表通过 announcement_id 或 ref_type、ref_id 关联具体业务。'))
docParts.push(heading(4, '3.5.4 设计原因'))
docParts.push(paragraph('退课和补考都需要教师参与，但最终往往还涉及管理员安排和状态确认。独立申请表可以保存教师意见和管理员意见，通知表则把处理结果主动推送给教师。'))

docParts.push(heading(2, '四、数据库设计'))
docParts.push(heading(3, '4.1 概念设计'))
docParts.push(paragraph('概念设计阶段保留系统通用实体，包括院系、专业、行政班、学生、教师、用户、角色、学期、课程、教室、教学班、上课时间、选课、成绩、考试安排、申请、公告、通知和操作日志等。教师端报告重点关注这些实体中与教师工作流直接相关的部分。'))
docParts.push(paragraph('教师端概念模型以教师和教学班为中心。为了避免一张 ER 图连线过多，报告将教师端实体关系拆成两张图：图2 展示授课、选课、成绩和考试等核心教学数据关系；图3 展示调课、退课、成绩修改、考试修改、补考和通知等申请流转关系。'))
docParts.push(image('rId2', '图2 教师端核心教学数据 ER 图', IMAGE_W, Math.round(IMAGE_W * 620 / 1240)))
docParts.push(image('rId3', '图3 教师端申请流转 ER 图', IMAGE_W, Math.round(IMAGE_W * 620 / 1240)))

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
docParts.push(paragraph('教师端物理设计重点关注教师授课数据的读取效率、成绩数据的一致性、申请数据的可追踪性和教师权限边界。数据库采用 MySQL InnoDB 引擎和 utf8mb4 字符集，利用外键、唯一约束、索引、触发器和申请表状态字段支撑教师端业务。'))
docParts.push(paragraph('教师端部署时不直接连接数据库，而是通过后端服务访问数据库。数据库既可以部署在本机 MySQL，也可以部署在云服务器自建 MySQL 或云数据库 RDS。云端数据库便于多端访问、集中备份和迁移，但需要配置安全组白名单、独立数据库账号和定期备份，避免成绩、选课和审批记录丢失。'))
docParts.push(codeBlock('表' + tableNo++ + ' 建库语句示例', [
  'DROP DATABASE IF EXISTS school;',
  'CREATE DATABASE school DEFAULT CHARACTER SET utf8mb4;',
  'USE school;',
  'SET NAMES utf8mb4;'
]))
docParts.push(heading(4, '4.3.1 约束与索引设计'))
docParts.push(paragraph('教师端最关键的约束是教学班归属、成绩唯一性和考试唯一性。teaching_class.teacher_id 用于限制教师只能访问本人教学班；score 对 student_id 和 teaching_class_id 建立唯一约束，保证一个学生在一个教学班只有一条成绩；exam_arrangement 对 teaching_class_id 建立唯一约束，保证一个教学班最多一条考试安排。'))
docParts.push(paragraph('教师端常用查询需要按 teacher_id、teaching_class_id、request_status 等字段过滤，因此教学班、成绩和申请表需要围绕这些字段设计索引。这样可以提升课表加载、成绩列表、调课申请列表、考试修改申请列表的查询效率。'))
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
docParts.push(paragraph('教师端直接使用的触发器集中在成绩表 score 上。当前系统共有两个成绩触发器：trg_score_before_insert 和 trg_score_before_update。二者分别在新增成绩和更新成绩前执行，职责一致：校验平时成绩、考试成绩是否在 0 到 100 分之间；当两项成绩都存在时，读取课程成绩比例并计算总评成绩和绩点。'))
docParts.push(paragraph('触发器适合处理与单表写入强相关、必须保持一致的派生字段。本系统中的 final_score 和 gpa_point 不由教师手工填写，而是在 usual_score 和 exam_score 写入时自动推导。这样可以避免教师导入成绩时误填总评成绩，也能保证学生端、教师端和管理员端看到的总评规则一致。'))
docParts.push(table(`表${tableNo++} 成绩表触发器完整清单`, ['触发器名称', '触发时机', '主要作用'], [
  ['trg_score_before_insert', 'BEFORE INSERT ON score', '新增成绩前校验成绩范围，计算总评成绩和绩点，补充录入时间'],
  ['trg_score_before_update', 'BEFORE UPDATE ON score', '更新成绩前校验成绩范围，重新计算总评成绩和绩点']
]))
docParts.push(codeBlock('表' + tableNo++ + ' 触发器语句：trg_score_before_insert', [
  "DELIMITER $$",
  'CREATE TRIGGER trg_score_before_insert',
  'BEFORE INSERT ON score',
  'FOR EACH ROW',
  'BEGIN',
  '    DECLARE v_usual_ratio TINYINT DEFAULT 40;',
  '    DECLARE v_exam_ratio  TINYINT DEFAULT 60;',
  '',
  '    IF NEW.usual_score IS NOT NULL AND (NEW.usual_score < 0 OR NEW.usual_score > 100) THEN',
  "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '平时成绩必须在0到100之间';",
  '    END IF;',
  '',
  '    IF NEW.exam_score IS NOT NULL AND (NEW.exam_score < 0 OR NEW.exam_score > 100) THEN',
  "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考试成绩必须在0到100之间';",
  '    END IF;',
  '',
  '    IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN',
  '        SELECT c.usual_ratio, c.exam_ratio',
  '        INTO v_usual_ratio, v_exam_ratio',
  '        FROM teaching_class tc',
  '        JOIN course c ON tc.course_id = c.course_id',
  '        WHERE tc.teaching_class_id = NEW.teaching_class_id;',
  '',
  '        SET NEW.final_score = ROUND(NEW.usual_score * v_usual_ratio / 100 + NEW.exam_score * v_exam_ratio / 100, 2);',
  '        SET NEW.gpa_point = CASE',
  '            WHEN NEW.final_score >= 90 THEN 4.0',
  '            WHEN NEW.final_score >= 85 THEN 3.7',
  '            WHEN NEW.final_score >= 82 THEN 3.3',
  '            WHEN NEW.final_score >= 78 THEN 3.0',
  '            WHEN NEW.final_score >= 75 THEN 2.7',
  '            WHEN NEW.final_score >= 72 THEN 2.3',
  '            WHEN NEW.final_score >= 68 THEN 2.0',
  '            WHEN NEW.final_score >= 66 THEN 1.7',
  '            WHEN NEW.final_score >= 64 THEN 1.5',
  '            WHEN NEW.final_score >= 60 THEN 1.0',
  '            ELSE 0.0',
  '        END;',
  '    END IF;',
  '',
  '    IF NEW.entered_at IS NULL THEN',
  '        SET NEW.entered_at = NOW();',
  '    END IF;',
  'END$$'
]))
docParts.push(codeBlock('表' + tableNo++ + ' 触发器语句：trg_score_before_update', [
  'CREATE TRIGGER trg_score_before_update',
  'BEFORE UPDATE ON score',
  'FOR EACH ROW',
  'BEGIN',
  '    DECLARE v_usual_ratio TINYINT DEFAULT 40;',
  '    DECLARE v_exam_ratio  TINYINT DEFAULT 60;',
  '',
  '    IF NEW.usual_score IS NOT NULL AND (NEW.usual_score < 0 OR NEW.usual_score > 100) THEN',
  "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '平时成绩必须在0到100之间';",
  '    END IF;',
  '',
  '    IF NEW.exam_score IS NOT NULL AND (NEW.exam_score < 0 OR NEW.exam_score > 100) THEN',
  "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考试成绩必须在0到100之间';",
  '    END IF;',
  '',
  '    IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN',
  '        SELECT c.usual_ratio, c.exam_ratio',
  '        INTO v_usual_ratio, v_exam_ratio',
  '        FROM teaching_class tc',
  '        JOIN course c ON tc.course_id = c.course_id',
  '        WHERE tc.teaching_class_id = NEW.teaching_class_id;',
  '',
  '        SET NEW.final_score = ROUND(NEW.usual_score * v_usual_ratio / 100 + NEW.exam_score * v_exam_ratio / 100, 2);',
  '        SET NEW.gpa_point = CASE',
  '            WHEN NEW.final_score >= 90 THEN 4.0',
  '            WHEN NEW.final_score >= 85 THEN 3.7',
  '            WHEN NEW.final_score >= 82 THEN 3.3',
  '            WHEN NEW.final_score >= 78 THEN 3.0',
  '            WHEN NEW.final_score >= 75 THEN 2.7',
  '            WHEN NEW.final_score >= 72 THEN 2.3',
  '            WHEN NEW.final_score >= 68 THEN 2.0',
  '            WHEN NEW.final_score >= 66 THEN 1.7',
  '            WHEN NEW.final_score >= 64 THEN 1.5',
  '            WHEN NEW.final_score >= 60 THEN 1.0',
  '            ELSE 0.0',
  '        END;',
  '    END IF;',
  'END$$',
  '',
  'DELIMITER ;'
]))
docParts.push(heading(4, '4.3.3 教师端查询设计'))
docParts.push(paragraph('教师课表查询需要联查教学班、上课时间、课程、学期、教师和教室。后端查询时以 teacher_id 为过滤条件，再按学期分组展示。学生名单查询在教学班归属校验后，联查 course_selection 和 student，只返回已选学生。'))
docParts.push(codeBlock('表' + tableNo++ + ' 教师课表查询 SQL 示例', [
  'SELECT tc.teaching_class_id, c.course_name, sem.semester_name,',
  '       sess.week_day, sess.section_desc, sess.week_parity,',
  '       tc.week_range, cl.building_name, cl.room_name',
  'FROM teaching_class tc',
  'JOIN teaching_class_session sess ON sess.teaching_class_id = tc.teaching_class_id',
  'JOIN course c ON tc.course_id = c.course_id',
  'JOIN semester sem ON tc.semester_id = sem.semester_id',
  'LEFT JOIN classroom cl ON tc.classroom_id = cl.classroom_id',
  'WHERE tc.teacher_id = ?;'
]))
docParts.push(heading(4, '4.3.4 教师端申请表设计'))
docParts.push(paragraph('教师端调课、成绩修改和考试修改都采用“申请表先记录，审核后更新主表”的设计。schedule_adjust_request 保存调课前后时间与教室；score_change_request 保存原成绩快照和申请原因；exam_change_request 保存原考试安排和申请后安排。'))
docParts.push(paragraph('这种物理设计可以避免教师端未审核数据污染主业务表，也让管理员审核时能够看到完整上下文。申请表中的 request_status、review_comment、reviewed_by、reviewed_at 等字段统一表达审核状态。'))

docParts.push(heading(2, '五、效果演示'))
docParts.push(paragraph('教师端实际运行后，教师可以围绕本人教学班完成授课查看、成绩处理和申请发起。效果演示只列出教师端相关页面和业务结果，如表所示。'))
docParts.push(table(`表${tableNo++} 教师端功能效果演示摘要`, ['页面或功能', '教师操作', '演示效果'], [
  ['授课课表', '选择学期、查看课程卡片、打开教学班详情', '按周课表展示本人课程，详情抽屉展示课程信息和学生名单'],
  ['成绩录入', '录入平时成绩和考试成绩，暂存或提交', '数据库触发器计算总评和绩点，提交后成绩只读'],
  ['成绩修改申请', '对已提交成绩填写修改原因', '申请表保存原成绩快照，等待管理员审核'],
  ['调课申请', '选择目标星期、节次和教室', '系统检查教室占用和教师时间冲突，待审核申请可撤销'],
  ['考试修改申请', '申请调整考试时间或教室', '系统动态筛选可用教室，并记录原考试安排和申请后安排'],
  ['退课审核与补考确认', '审核学生退课，确认补考信息', '教师意见写入申请表，后续由管理员继续处理'],
  ['通知中心', '查看公告和审批结果', '通知按用户投递，支持已读状态维护']
]))

docParts.push(heading(2, '六、总结和评价'))
docParts.push(heading(3, '6.1 项目总结'))
docParts.push(paragraph('教师端报告从全系统数据库设计中抽取教师工作流相关内容，保留总体功能、概念设计和完整逻辑表结构，同时将功能设计、教师端 ER 图、物理设计和效果演示聚焦到教师端。教师端的核心是 teaching_class.teacher_id 所形成的数据边界，以及围绕教学班展开的成绩、调课、考试、退课、补考和通知流程。'))
docParts.push(heading(3, '6.2 体会'))
docParts.push(paragraph('第一，教师端虽然不是系统中数据维护最多的角色，但它对数据一致性要求很高。成绩、考试和调课都直接影响学生，因此教师端不能绕过数据库约束和审核流程。'))
docParts.push(paragraph('第二，教学班是教师端最重要的实体。课表、学生名单、成绩、考试和申请都围绕教学班展开，教师端权限也通过教学班归属实现。'))
docParts.push(paragraph('第三，成绩计算适合下沉到数据库触发器。这样教师端导入、手工录入、后端保存都使用同一套总评和绩点规则。'))
docParts.push(paragraph('第四，调课、考试修改和成绩修改不能直接改主表。申请表保留原始数据、申请数据和审核状态，使教师端操作可追踪、可撤销、可审核。'))
docParts.push(paragraph('第五，教师端报告抽取时需要区分通用数据结构和教师端专属业务。完整逻辑表结构可以保留作为系统背景，但功能设计和物理设计必须聚焦教师端。'))
docParts.push(heading(3, '6.3 不足与改进方向'))
docParts.push(paragraph('后续教师端可以继续补充更细粒度的教师统计报表，例如按教学班统计成绩分布、退课数量、调课次数和考试修改次数；也可以加入更完善的消息提醒机制，在成绩提交截止前主动提醒教师处理未提交成绩。'))

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
  { name: 'word/media/er-core.svg', data: svgErTeacher() },
  { name: 'word/media/er-approval.svg', data: svgTeacherRequestEr() },
  { name: 'word/media/er-support.svg', data: svgErSupport() }
]

fs.writeFileSync(outPath, createZip(files))
console.log(outPath)
