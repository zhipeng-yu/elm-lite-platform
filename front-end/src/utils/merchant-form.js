// PATCH 仅发送编辑过的字段，避免将加载表单时的库存覆盖回数据库。
export function productPayload(form, original = null) {
  const data = {
    productName: form.productName, categoryId: form.categoryId,
    description: form.description || '', imageUrl: form.imageUrl || '',
    priceCent: Math.round(form.priceYuan * 100), stock: form.stock
  }
  if (!original) return data
  return Object.fromEntries(Object.entries(data).filter(([key, value]) =>
    value !== (original[key] ?? (key === 'description' || key === 'imageUrl' ? '' : undefined))))
}
