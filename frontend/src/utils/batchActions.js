export async function runBatchAction(rows, action) {
  const results = await Promise.allSettled(rows.map(row => action(row)))
  return {
    successCount: results.filter(item => item.status === 'fulfilled').length,
    failedCount: results.filter(item => item.status === 'rejected').length
  }
}
