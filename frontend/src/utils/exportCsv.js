function escapeCsvCell(value) {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

function normalizeColumns(columns) {
  return columns.map(column => {
    if (typeof column === 'string') {
      return { label: column, prop: column }
    }
    return column
  })
}

function resolveValue(row, column) {
  if (typeof column.value === 'function') {
    return column.value(row)
  }
  return row[column.prop]
}

export function buildCsvContent(columns, rows) {
  const normalizedColumns = normalizeColumns(columns)
  const header = normalizedColumns.map(column => column.label)
  const body = rows.map(row => normalizedColumns.map(column => resolveValue(row, column)))
  return [header, ...body].map(row => row.map(escapeCsvCell).join(',')).join('\n')
}

export function downloadCsv(filename, columns, rows) {
  const content = buildCsvContent(columns, rows)
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
