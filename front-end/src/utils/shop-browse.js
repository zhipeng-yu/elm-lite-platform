export function selectShops(shops, { query = '', openOnly = false, freeDelivery = false, sort = '' } = {}) {
  const term = query.trim().toLocaleLowerCase()
  return shops.filter(shop =>
    `${shop.shopName} ${shop.description || ''}`.toLocaleLowerCase().includes(term)
    && (!openOnly || shop.businessStatus === 1)
    && (!freeDelivery || shop.deliveryPriceCent === 0)
  ).sort((a, b) => {
    const openFirst = Number(b.businessStatus === 1) - Number(a.businessStatus === 1)
    if (openFirst) return openFirst
    if (sort === 'delivery') return a.deliveryPriceCent - b.deliveryPriceCent
    if (sort === 'minimum') return a.startPriceCent - b.startPriceCent
    return b.id - a.id
  })
}
