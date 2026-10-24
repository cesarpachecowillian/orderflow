import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, extractApiMessage } from '../lib/api'
import type { Order, Product } from '../types'

interface ItemRow {
  key: number
  productId: number | null
  quantity: number
}

let nextKey = 1
const newRow = (): ItemRow => ({ key: nextKey++, productId: null, quantity: 1 })

function formatBRL(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function NewOrder() {
  const navigate = useNavigate()
  const [products, setProducts] = useState<Product[]>([])
  const [loadingProducts, setLoadingProducts] = useState(true)
  const [items, setItems] = useState<ItemRow[]>([newRow()])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let active = true
    api
      .get<Product[]>('/products')
      .then(({ data }) => {
        if (active) setProducts(data)
      })
      .catch((err) => {
        if (active) setError(extractApiMessage(err, 'Falha ao carregar produtos'))
      })
      .finally(() => {
        if (active) setLoadingProducts(false)
      })
    return () => {
      active = false
    }
  }, [])

  const priceOf = (productId: number | null): number =>
    products.find((p) => p.id === productId)?.price ?? 0

  const total = items.reduce((sum, it) => sum + it.quantity * priceOf(it.productId), 0)

  const updateItem = (key: number, patch: Partial<ItemRow>) => {
    setItems((curr) => curr.map((it) => (it.key === key ? { ...it, ...patch } : it)))
  }

  const removeItem = (key: number) => {
    setItems((curr) => (curr.length > 1 ? curr.filter((it) => it.key !== key) : curr))
  }

  const addItem = () => setItems((curr) => [...curr, newRow()])

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (items.some((i) => i.productId === null || !Number.isInteger(i.quantity) || i.quantity < 1)) {
      setError('Cada item precisa de um produto e quantidade ≥ 1.')
      return
    }
    setLoading(true)
    try {
      const payload = {
        items: items.map(({ productId, quantity }) => ({ productId: productId as number, quantity })),
      }
      const { data } = await api.post<Order>('/orders', payload)
      navigate(`/orders/${data.id}`)
    } catch (err) {
      setError(extractApiMessage(err, 'Falha ao criar pedido'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Novo pedido</h1>

      <form onSubmit={onSubmit} className="card p-6 space-y-4">
        {loadingProducts && <p className="text-sm text-gray-500">Carregando produtos…</p>}

        <div className="space-y-3">
          {items.map((it) => {
            const price = priceOf(it.productId)
            return (
              <div key={it.key} className="grid grid-cols-12 gap-3 items-end">
                <div className="col-span-5">
                  <label className="label">Produto</label>
                  <select
                    required
                    className="input"
                    disabled={loadingProducts}
                    value={it.productId ?? ''}
                    onChange={(e) =>
                      updateItem(it.key, { productId: e.target.value ? Number(e.target.value) : null })
                    }
                  >
                    <option value="">Selecione um produto…</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-span-2">
                  <label className="label">Qtd</label>
                  <input
                    type="number"
                    min={1}
                    step={1}
                    required
                    className="input"
                    value={it.quantity}
                    onChange={(e) => updateItem(it.key, { quantity: Number(e.target.value) })}
                  />
                </div>
                <div className="col-span-2">
                  <label className="label">Preço unit.</label>
                  <p className="py-2 text-sm text-gray-700">{it.productId !== null ? formatBRL(price) : '—'}</p>
                </div>
                <div className="col-span-2">
                  <label className="label">Subtotal</label>
                  <p className="py-2 text-sm font-medium text-gray-900">
                    {it.productId !== null ? formatBRL(it.quantity * price) : '—'}
                  </p>
                </div>
                <div className="col-span-1">
                  <button
                    type="button"
                    onClick={() => removeItem(it.key)}
                    disabled={items.length === 1}
                    className="btn-secondary w-full"
                    title="Remover item"
                  >
                    ✕
                  </button>
                </div>
              </div>
            )
          })}
        </div>

        <div className="flex items-center justify-between border-t border-gray-200 pt-4">
          <button type="button" onClick={addItem} className="btn-secondary">
            + Adicionar item
          </button>
          <div className="text-right">
            <p className="text-lg font-semibold">
              Total estimado: <span className="text-brand-600">{formatBRL(total)}</span>
            </p>
            <p className="text-xs text-gray-500">O valor final é calculado pelo servidor.</p>
          </div>
        </div>

        {error && (
          <div className="rounded-md bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="flex gap-3 justify-end pt-2">
          <button type="button" onClick={() => navigate('/orders')} className="btn-secondary">
            Cancelar
          </button>
          <button type="submit" disabled={loading || loadingProducts} className="btn-primary">
            {loading ? 'Criando…' : 'Criar pedido'}
          </button>
        </div>
      </form>
    </div>
  )
}
