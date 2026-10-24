import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, extractApiMessage } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { ChevronRight } from '../components/Icons'
import type { UserOrdersSummary } from '../types'

const PAGE_SIZE = 10

function formatBRL(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function Customers() {
  const { role } = useAuth()
  const navigate = useNavigate()

  const [all, setAll] = useState<UserOrdersSummary[] | null>(null)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (role !== 'ADMIN') return
    setLoading(true)
    setError(null)
    api.get<UserOrdersSummary[]>('/orders/summary/by-user')
      .then((r) => setAll(r.data))
      .catch((err) => setError(extractApiMessage(err, 'Falha ao carregar clientes')))
      .finally(() => setLoading(false))
  }, [role])

  const filtered = useMemo(() => {
    if (!all) return []
    const q = search.trim().toLowerCase()
    if (!q) return all
    return all.filter(
      (u) =>
        u.customerName.toLowerCase().includes(q) ||
        u.customerEmail.toLowerCase().includes(q)
    )
  }, [all, search])

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const safePage = Math.min(page, totalPages - 1)
  const slice = filtered.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE)

  const goToOrders = (u: UserOrdersSummary) => {
    const params = new URLSearchParams({
      customer: u.customerEmail,
      customerName: u.customerName,
    })
    navigate(`/orders?${params.toString()}`)
  }

  if (role !== 'ADMIN') {
    return <div className="text-gray-600">Acesso restrito a administradores.</div>
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Pedidos por Cliente</h1>
        <p className="text-sm text-gray-500">Valores agregados por usuário. Clique em um cliente para ver os pedidos dele.</p>
      </div>

      <div className="mb-4">
        <input
          type="text"
          className="input max-w-md"
          placeholder="Buscar por nome ou e-mail…"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0) }}
        />
      </div>

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 mb-4">
          {error}
        </div>
      )}

      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase">
            <tr>
              <th className="px-4 py-3">Cliente</th>
              <th className="px-4 py-3 text-right">Pedidos</th>
              <th className="px-4 py-3 text-right">Valor total</th>
              <th className="px-4 py-3 text-right">Aprovados</th>
              <th className="px-4 py-3 text-right">Em análise</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading && (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">Carregando…</td></tr>
            )}
            {!loading && slice.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center text-gray-500">
                  {search ? 'Nenhum cliente bate com a busca.' : 'Nenhum cliente cadastrado.'}
                </td>
              </tr>
            )}
            {!loading && slice.map((u) => (
              <tr
                key={u.customerEmail}
                className="group hover:bg-gray-50 cursor-pointer transition-colors"
                onClick={() => goToOrders(u)}
              >
                <td className="px-4 py-3">
                  <div className="text-gray-900 font-medium">{u.customerName}</div>
                  <div className="text-xs text-gray-500">{u.customerEmail}</div>
                </td>
                <td className="px-4 py-3 text-right text-gray-700">{u.totalOrders}</td>
                <td className="px-4 py-3 text-right font-medium">{formatBRL(u.totalAmount)}</td>
                <td className="px-4 py-3 text-right text-green-700">{formatBRL(u.confirmedAmount)}</td>
                <td className="px-4 py-3 text-right text-amber-700">{formatBRL(u.pendingAmount)}</td>
                <td className="px-4 py-3 text-right">
                  <ChevronRight className="w-5 h-5 text-gray-300 group-hover:text-gray-700 transition-colors inline-block" />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 text-sm">
          <span className="text-gray-600">
            Página {safePage + 1} de {totalPages} — {filtered.length} cliente(s)
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage(Math.max(0, safePage - 1))}
              disabled={safePage === 0}
              className="btn-secondary"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage(Math.min(totalPages - 1, safePage + 1))}
              disabled={safePage >= totalPages - 1}
              className="btn-secondary"
            >
              Próxima
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
