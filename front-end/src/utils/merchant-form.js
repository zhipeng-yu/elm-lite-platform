// PATCH 仅发送编辑过的字段，避免将加载表单时的库存覆盖回数据库。

function normalizeDetailImageUrls(value) {
  if (!Array.isArray(value)) return []

  return value
    .map((item) => typeof item === 'string' ? item.trim() : '')
    .filter(Boolean)
}

function sameStringArray(left, right) {
  if (left.length !== right.length) return false
  return left.every((value, index) => value === right[index])
}

export function productPayload(form, original = null) {
  const data = {
    productName: form.productName,
    categoryId: form.categoryId,
    description: form.description || '',
    imageUrl: form.imageUrl || '',
    detailImageUrls: normalizeDetailImageUrls(form.detailImageUrls),
    priceCent: Math.round(form.priceYuan * 100),
    stock: form.stock
  }

  if (!original) return data

  const originalDetailImageUrls = normalizeDetailImageUrls(original.detailImageUrls)

  return Object.fromEntries(
    Object.entries(data).filter(([key, value]) => {
      if (key === 'detailImageUrls') {
        return !sameStringArray(value, originalDetailImageUrls)
      }

      const fallback =
        key === 'description' || key === 'imageUrl'
          ? ''
          : undefined

      return value !== (original[key] ?? fallback)
    })
  )
}