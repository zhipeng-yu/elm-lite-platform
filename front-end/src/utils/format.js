// 展示格式化工具：接口金额为整数分，页面统一转为元字符串。

export function formatPriceCent(cent) {
  return (Number(cent) / 100).toFixed(2)
}

// businessStatus 与 api-contract.md 冻结契约一致：0 休息、1 营业、2 临时闭店
export const BUSINESS_STATUS_TEXT = {
  0: '休息中',
  1: '营业中',
  2: '临时闭店'
}

export const BUSINESS_STATUS_TAG = {
  0: 'info',
  1: 'success',
  2: 'warning'
}
