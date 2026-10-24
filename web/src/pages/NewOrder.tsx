import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, extractApiMessage } from '../lib/api'
import type { CreateOrderItemRequest, Order } from '../types'

interface ItemRow extends CreateOrderItemRequest {
  key: number
}

let nextKey = 1
const newRow = (): ItemRow => ({ key: nextKey++, productName: '', quantity: 1, unitPrice: 0 })

function formatBRL(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function NewOrder() {
  const navigate = useNavigate()
  const [items, setItems] = useState<ItemRow[]>([newRow()])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const total = items.reduce((sum, it) => sum + it.quantity * it.unitPrice, 0)

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
    if (items.some((i) => !i.productName.trim() || i.quantity < 1 || i.unitPrice <= 0)) {
      setError('Cada item precisa de nome, quantidade ≥ 1 e preço > 0.')
      return
    }
    setLoading(true)
    try {
      const payload = {
        items: items.map(({ productName, quantity, unitPrice }) => ({ productName, quantity, unitPrice })),
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
        <div className="space-y-3">
          {items.map((it) => (
            <div key={it.key} className="grid grid-cols-12 gap-3 items-end">
              <div className="col-span-6">
                <label className="label">Produto</label>
                <input
                  required
                  className="input"
                  value={it.productName}
                  onChange={(e) => updateItem(it.key, { productName: e.target.value })}
                />
              </div>
              <div className="col-span-2">
                <label className="label">Qtd</label>
                <input
                  type="number"
                  min={1}
                  required
                  className="input"
                  value={it.quantity}
                  onChange={(e) => updateItem(it.key, { quantity: Number(e.target.value) })}
                />
              </div>
              <div className="col-span-3">
                <label className="label">Preço unit. (R$)</label>
                <input
                  type="number"
                  step="0.01"
                  min={0.01}
                  required
                  className="input"
                  value={it.unitPrice}
                  onChange={(e) => updateItem(it.key, { unitPrice: Number(e.target.value) })}
                />
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
          ))}
        </div>

        <div className="flex items-center justify-between border-t border-gray-200 pt-4">
          <button type="button" onClick={addItem} className="btn-secondary">
            + Adicionar item
          </button>
          <p className="text-lg font-semibold">
            Total: <span className="text-brand-600">{formatBRL(total)}</span>
          </p>
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
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Criando…' : 'Criar pedido'}
          </button>
        </div>
      </form>
    </div>
  )
}
