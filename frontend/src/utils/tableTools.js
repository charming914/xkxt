export function includesKeyword(row, fields, keyword) {
  const text = String(keyword || '').trim().toLowerCase()
  if (!text) {
    return true
  }
  return fields
    .map(field => (typeof field === 'function' ? field(row) : row[field]))
    .join(' ')
    .toLowerCase()
    .includes(text)
}

export function sortRows(rows, sortState, getters = {}) {
  if (!sortState?.prop || !sortState?.order) {
    return [...rows]
  }

  const direction = sortState.order === 'ascending' ? 1 : -1
  return [...rows].sort((left, right) => {
    const leftValue = resolveValue(left, sortState.prop, getters)
    const rightValue = resolveValue(right, sortState.prop, getters)
    return compareValues(leftValue, rightValue) * direction
  })
}

export function updateSortState(sortState, { prop, order }) {
  sortState.prop = prop || ''
  sortState.order = order || ''
}

function resolveValue(row, prop, getters) {
  const getter = getters[prop]
  return typeof getter === 'function' ? getter(row) : row[prop]
}

function compareValues(left, right) {
  if (left == null && right == null) return 0
  if (left == null) return -1
  if (right == null) return 1

  const leftNumber = Number(left)
  const rightNumber = Number(right)
  if (!Number.isNaN(leftNumber) && !Number.isNaN(rightNumber)) {
    return leftNumber - rightNumber
  }

  return String(left).localeCompare(String(right), 'zh-Hans', {
    numeric: true,
    sensitivity: 'base'
  })
}
