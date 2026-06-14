const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const outPath = path.join(root, 'docs', '教师端成绩触发器SQL.docx')
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

function rPr(font = '宋体', size = 18, bold = false) {
  return `<w:rPr><w:rFonts w:ascii="Times New Roman" w:eastAsia="${font}" w:hAnsi="Times New Roman"/><w:sz w:val="${size}"/><w:szCs w:val="${size}"/>${bold ? '<w:b/><w:bCs/>' : ''}</w:rPr>`
}

function paragraph(text, opts = {}) {
  const { align, indent = true, bold = false, font = '宋体', size = 21, spacingAfter = 160 } = opts
  const jcXml = align ? `<w:jc w:val="${align}"/>` : ''
  const indXml = indent
    ? '<w:ind w:firstLineChars="200"/>'
    : '<w:ind w:left="0" w:right="0" w:firstLine="0" w:firstLineChars="0"/>'
  return `<w:p><w:pPr>${jcXml}<w:spacing w:after="${spacingAfter}" w:line="283" w:lineRule="auto"/>${indXml}</w:pPr><w:r>${rPr(font, size, bold)}<w:t xml:space="preserve">${escapeXml(text)}</w:t></w:r></w:p>`
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

function triggerBlock(sql, triggerName) {
  const start = sql.indexOf(`CREATE TRIGGER ${triggerName}`)
  if (start === -1) throw new Error(`Missing trigger: ${triggerName}`)
  const end = sql.indexOf('END$$', start)
  if (end === -1) throw new Error(`Missing END$$ after: ${triggerName}`)
  return sql.slice(start, end + 'END$$'.length).trim().split(/\r?\n/)
}

const sql = fs.readFileSync(path.join(root, 'sql', 'school.sql'), 'utf8')
const insertLines = ['DELIMITER $$', '', ...triggerBlock(sql, 'trg_score_before_insert')]
const updateLines = [...triggerBlock(sql, 'trg_score_before_update'), '', 'DELIMITER ;']

const body = [
  codeBlock('表1 触发器语句：trg_score_before_insert', insertLines),
  codeBlock('表2 触发器语句：trg_score_before_update', updateLines)
].join('')

const documentXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>${body}<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="851" w:footer="992" w:gutter="0"/></w:sectPr></w:body></w:document>`
const stylesXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:rFonts w:ascii="Times New Roman" w:eastAsia="宋体" w:hAnsi="Times New Roman"/><w:sz w:val="21"/></w:rPr><w:pPr><w:spacing w:after="160" w:line="283" w:lineRule="auto"/><w:ind w:firstLineChars="200"/></w:pPr></w:style></w:styles>`
const contentTypes = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/></Types>`
const rootRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>`
const docRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>`

const files = [
  { name: '[Content_Types].xml', data: contentTypes },
  { name: '_rels/.rels', data: rootRels },
  { name: 'word/document.xml', data: documentXml },
  { name: 'word/styles.xml', data: stylesXml },
  { name: 'word/_rels/document.xml.rels', data: docRels }
]

fs.writeFileSync(outPath, createZip(files))
console.log(outPath)
