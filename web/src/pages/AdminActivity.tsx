import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, extractApiMessage } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { ChevronRight } from '../components/Icons'
import type { AdminActivitySummary } from '../types'

const PAGE_SIZE = 10

function formatBRL(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function AdminActivity() {
  const { role } = useAuth()
  const navigate = useNavigate()
  const [rows, setRows] = useState<AdminActivitySummary[] | null>(null)

  const goToAdminOrders = (r: AdminActivitySummary) => {
    // Default: abre na visão de aprovados. Se o admin não tem nenhum aprovado mas tem reprovados,
    // já abre na visão de reprovados.
    const initialScope = r.confirmedCount > 0 ? 'confirmedBy' : 'cancelledBy'
    const params = new URLSearchParams({
      [initialScope]: r.adminEmail,
      adminName: r.adminName,
    })
    navigate(`/orders?${params.toString()}`)
  }
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  useEffect(() => {
    if (role !== 'ADMIN') return
    setLoading(true)
    setError(null)
    api.get<AdminActivitySummary[]>('/orders/admin-activity')
      .then((r) => setRows(r.data))
      .catch((err) => setError(extractApiMessage(err, 'Falha ao carregar atividade')))
      .finally(() => setLoading(false))
  }, [role])

  const filtered = useMemo(() => {
    if (!rows) return []
    const q = search.trim().toLowerCase()
    if (!q) return rows
    return rows.filter(
      (r) => r.adminName.toLowerCase().includes(q) || r.adminEmail.toLowerCase().includes(q)
    )
  }, [rows, search])

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const safePage = Math.min(page, totalPages - 1)
  const slice = filtered.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE)

  const totalConfirmed = filtered.reduce((s, r) => s + r.confirmedCount, 0)
  const totalCancelled = filtered.reduce((s, r) => s + r.cancelledCount, 0)

  if (role !== 'ADMIN') {
    return <div className="text-gray-600">Acesso restrito a administradores.</div>
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Atividade dos Administradores</h1>
        <p className="text-sm text-gray-500">
          Pedidos confirmados (aprovados) e cancelados manualmente (reprovados) por cada administrador.
          Cancelamentos automáticos do próprio cliente não entram aqui.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-6">
        <div className="card p-4">
          <p className="text-xs uppercase tracking-wide text-gray-500">Confirmações totais</p>
          <p className="text-2xl font-bold mt-1 text-green-700">{totalConfirmed}</p>
          <p className="text-xs text-gray-500 mt-1">somando todos os admins</p>
        </div>
        <div className="card p-4">
          <p className="text-xs uppercase tracking-wide text-gray-500">Cancelamentos manuais totais</p>
          <p className="text-2xl font-bold mt-1 text-red-700">{totalCancelled}</p>
          <p className="text-xs text-gray-500 mt-1">somando todos os admins</p>
        </div>
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
              <th className="px-4 py-3">Administrador</th>
              <th className="px-4 py-3 text-right">Aprovados</th>
              <th className="px-4 py-3 text-right">Valor aprovado</th>
              <th className="px-4 py-3 text-right">Reprovados</th>
              <th className="px-4 py-3 text-right">Valor reprovado</th>
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
                  {search ? 'Nenhum admin bate com a busca.' : 'Nenhuma atividade administrativa registrada ainda.'}
                </td>
              </tr>
            )}
            {!loading && slice.map((r) => {
              const hasActivity = r.confirmedCount + r.cancelledCount > 0
              return (
                <tr
                  key={r.adminEmail}
                  className={hasActivity ? 'group hover:bg-gray-50 cursor-pointer transition-colors' : ''}
                  onClick={() => hasActivity && goToAdminOrders(r)}
                  title={hasActivity ? 'Clique para ver os pedidos deste admin' : ''}
                >
                  <td className="px-4 py-3">
                    <div className="text-gray-900 font-medium">{r.adminName}</div>
                    <div className="text-xs text-gray-500">{r.adminEmail}</div>
                  </td>
                  <td className="px-4 py-3 text-right text-green-700 font-medium">{r.confirmedCount}</td>
                  <td className="px-4 py-3 text-right text-green-700">{formatBRL(r.confirmedAmount)}</td>
                  <td className="px-4 py-3 text-right text-red-700 font-medium">{r.cancelledCount}</td>
                  <td className="px-4 py-3 text-right text-red-700">{formatBRL(r.cancelledAmount)}</td>
                  <td className="px-4 py-3 text-right">
                    {hasActivity && (
                      <ChevronRight className="w-5 h-5 text-gray-300 group-hover:text-gray-700 transition-colors inline-block" />
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 text-sm">
          <span className="text-gray-600">
            Página {safePage + 1} de {totalPages} — {filtered.length} admin(s)
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
