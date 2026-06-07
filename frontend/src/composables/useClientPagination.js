import { computed, ref, watch } from 'vue'

/**
 * 通用客户端分页。
 * 传入一个返回完整数组的响应式 source（ref 或 computed），
 * 返回当前页数据、页码、每页条数、总数等，供 el-table 与 el-pagination 使用。
 *
 * @param {import('vue').Ref<Array>|import('vue').ComputedRef<Array>} source 完整数据源
 * @param {number} defaultPageSize 默认每页条数
 */
export function useClientPagination(source, defaultPageSize = 10) {
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)

  const total = computed(() => source.value?.length || 0)

  const pagedData = computed(() => {
    const list = source.value || []
    const start = (currentPage.value - 1) * pageSize.value
    return list.slice(start, start + pageSize.value)
  })

  // 数据变化或每页条数变化时，若当前页超出范围则回到第 1 页
  watch([total, pageSize], () => {
    const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  })

  function handleSizeChange(size) {
    pageSize.value = size
    currentPage.value = 1
  }

  function handleCurrentChange(page) {
    currentPage.value = page
  }

  function reset() {
    currentPage.value = 1
  }

  return {
    currentPage,
    pageSize,
    total,
    pagedData,
    handleSizeChange,
    handleCurrentChange,
    reset
  }
}
