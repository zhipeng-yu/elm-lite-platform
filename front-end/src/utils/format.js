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

// orderStatus 与 api-contract.md 第 8 节一致：0 待处理、1 已确认、2 制作中、3 配送中、4 已完成、5 已取消
export const ORDER_STATUS_TEXT = {
  0: '待处理',
  1: '已确认',
  2: '制作中',
  3: '配送中',
  4: '已完成',
  5: '已取消'
}

export const ORDER_STATUS_TAG = {
  0: 'warning',
  1: 'primary',
  2: 'primary',
  3: 'primary',
  4: 'success',
  5: 'info'
}
