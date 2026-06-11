import { h } from 'vue'
import { ElMessageBox } from 'element-plus'

export async function runProtectedDelete({
  entityName,
  targetLabel,
  checkAction,
  deleteAction,
  finalWarning,
  blockedHint = '处理完以上关联数据后，再重新执行删除。'
}) {
  try {
    const response = await checkAction()
    const check = response.data || {}
    if (!check.canDelete) {
      await showDeleteBlockedDialog({ entityName, targetLabel, check, blockedHint })
      return false
    }

    await ElMessageBox.confirm(
      `系统检查未发现关联数据。确认继续删除${entityName}「${targetLabel}」吗？`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '继续',
        cancelButtonText: '取消'
      }
    )
    await ElMessageBox.confirm(
      `二次确认：${finalWarning || `删除后将移除${entityName}数据，无法恢复。`}确认删除「${targetLabel}」？`,
      '最终确认',
      {
        type: 'error',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    await deleteAction()
    return true
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return false
    }
    throw error
  }
}

function showDeleteBlockedDialog({ entityName, targetLabel, check, blockedHint }) {
  const items = check.items || []
  const message = h('div', { style: 'line-height: 1.7;' }, [
    h('p', { style: 'margin: 0 0 8px;' }, `${entityName}「${targetLabel}」仍有关联数据，暂不能删除。`),
    h('ul', { style: 'margin: 0; padding-left: 18px; max-height: 260px; overflow-y: auto;' }, items.map(item => {
      return h('li', { style: 'margin-bottom: 6px;' }, [
        h('strong', null, `${item.label}：${item.count} 条`),
        h('span', { style: 'display: block; color: #64748b;' }, item.suggestion || '请先处理关联数据')
      ])
    })),
    h('p', { style: 'margin: 10px 0 0; color: #b45309;' }, blockedHint)
  ])
  return ElMessageBox.alert(message, `无法删除${entityName}`, {
    type: 'warning',
    confirmButtonText: '知道了'
  })
}
