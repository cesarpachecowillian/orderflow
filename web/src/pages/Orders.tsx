import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api, extractApiMessage } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { ChevronRight } from '../components/Icons'
import type { AdminActivitySummary, Order, OrderStatus, OrderSummary, Page } from '../types'

const PAGE_SIZE = 10

type StatusFilter = 'ALL' | OrderStatus

const STATUS_TABS: { id: StatusFilter; label: string }[] = [
  { id: 'ALL', label: 'Todos' },
  { id: 'CONFIRMED', label: 'Confirmados' },
  { id: 'PENDING', label: 'Em análise' },
  { id: 'CANCELLED', label: 'Cancelados' },
]

function StatusBadge({ status }: { status: OrderStatus }) {
  const cls =
    status === 'PENDING' ? 'badge-pending' :
    status === 'CONFIRMED' ? 'badge-confirmed' :
    'badge-cancelled'
  return <span className={cls}>{status}</span>
}

function formatBRL(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR')
}

function SummaryCard({ label, value, sub, accent }: { label: string; value: string; sub?: string; accent?: 'amber' | 'green' | 'red' | 'brand' }) {
  const accentCls =
    accent === 'green' ? 'text-green-700' :
    accent === 'amber' ? 'text-amber-700' :
    accent === 'red' ? 'text-red-700' :
    accent === 'brand' ? 'text-brand-700' : 'text-gray-900'
  return (
    <div className="card p-4">
      <p className="text-xs uppercase tracking-wide text-gray-500">{label}</p>
      <p className={`text-2xl font-bold mt-1 ${accentCls}`}>{value}</p>
      {sub && <p className="text-xs text-gray-500 mt-1">{sub}</p>}
    </div>
  )
}

export function Orders() {
  const { role } = useAuth()
  const isAdmin = role === 'ADMIN'
  const navigate = useNavigate()

  const [searchParams, setSearchParams] = useSearchParams()
  const status = (searchParams.get('status') as StatusFilter | null) ?? 'ALL'
  const customerEmail = searchParams.get('customer') ?? ''
  const customerName = searchParams.get('customerName') ?? ''
  const confirmedBy = searchParams.get('confirmedBy') ?? ''
  const cancelledBy = searchParams.get('cancelledBy') ?? ''
  const adminName = searchParams.get('adminName') ?? ''
  const page = Number(searchParams.get('page') ?? '0')

  const [data, setData] = useState<Page<Order> | null>(null)
  const [summary, setSummary] = useState<OrderSummary | null>(null)
  const [adminSummary, setAdminSummary] = useState<AdminActivitySummary | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const queryParams = useMemo(() => {
    const p: Record<string, string | number> = {
      page,
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    }
    if (status !== 'ALL') p.status = status
    if (customerEmail && isAdmin) p.customerEmail = customerEmail
    if (confirmedBy && isAdmin) p.confirmedBy = confirmedBy
    if (cancelledBy && isAdmin) p.cancelledBy = cancelledBy
    return p
  }, [page, status, customerEmail, confirmedBy, cancelledBy, isAdmin])

  useEffect(() => {
    setLoading(true)
    setError(null)
    const adminFilterEmail = (confirmedBy || cancelledBy) && isAdmin ? (confirmedBy || cancelledBy) : null
    const summaryParams = customerEmail && isAdmin ? { customerEmail } : undefined

    const listP = api.get<Page<Order>>('/orders', { params: queryParams })
    const summaryP = api.get<OrderSummary>('/orders/summary', { params: summaryParams })
    const adminActivityP = adminFilterEmail
      ? api.get<AdminActivitySummary[]>('/orders/admin-activity')
      : Promise.resolve(null)

    Promise.all([listP, summaryP, adminActivityP])
      .then(([list, sum, adm]) => {
        setData(list.data)
        setSummary(sum.data)
        if (adm && adminFilterEmail) {
          setAdminSummary(adm.data.find((r) => r.adminEmail === adminFilterEmail) ?? null)
        } else {
          setAdminSummary(null)
        }
      })
      .catch((err) => setError(extractApiMessage(err, 'Falha ao carregar pedidos')))
      .finally(() => setLoading(false))
  }, [queryParams, customerEmail, confirmedBy, cancelledBy, isAdmin])

  const updateParams = (patch: Record<string, string | null>) => {
    const next = new URLSearchParams(searchParams)
    Object.entries(patch).forEach(([k, v]) => {
      if (v === null || v === '') next.delete(k)
      else next.set(k, v)
    })
    if (!('page' in patch)) next.delete('page')
    setSearchParams(next)
  }

  const setStatus = (s: StatusFilter) => updateParams({ status: s === 'ALL' ? null : s })
  const setPage = (p: number) => {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(p))
    setSearchParams(next)
  }
  const clearAdminFilter = () =>
    updateParams({ customer: null, customerName: null, confirmedBy: null, cancelledBy: null, adminName: null, status: null })

  // Quando filtrando por admin: toggle entre "Aprovados" (confirmedBy) e "Reprovados" (cancelledBy),
  // mantendo o e-mail do admin e o nome.
  const adminEmailInUse = confirmedBy || cancelledBy
  const isFilteringByAdmin = isAdmin && !!adminEmailInUse
  const adminScope: 'approved' | 'rejected' | null =
    confirmedBy ? 'approved' : (cancelledBy ? 'rejected' : null)
  const setAdminScope = (scope: 'approved' | 'rejected') => {
    const email = adminEmailInUse
    if (!email) return
    updateParams({
      confirmedBy: scope === 'approved' ? email : null,
      cancelledBy: scope === 'rejected' ? email : null,
      adminName,
      status: null,
    })
  }

  const adminFilterBanner = isAdmin && (() => {
    if (customerEmail) return { label: 'cliente', who: customerName || customerEmail, email: customerEmail }
    if (adminEmailInUse) return {
      label: scopeLabel(adminScope),
      who: adminName || adminEmailInUse,
      email: adminEmailInUse,
    }
    return null
  })()

  function scopeLabel(s: 'approved' | 'rejected' | null) {
    if (s === 'approved') return 'aprovados pelo admin'
    if (s === 'rejected') return 'reprovados pelo admin'
    return ''
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {isAdmin ? 'Pedidos' : 'Meus pedidos'}
          </h1>
          {isAdmin && <p className="text-sm text-gray-500">Visão administrativa</p>}
        </div>
        <Link to="/orders/new" className="btn-primary">+ Novo pedido</Link>
      </div>

      {adminFilterBanner && (
        <div className="card p-3 mb-4 bg-brand-50 border-brand-200 flex items-center justify-between">
          <div className="text-sm">
            <span className="text-gray-500">Filtrando por {adminFilterBanner.label}:</span>{' '}
            <span className="font-medium text-gray-900">{adminFilterBanner.who}</span>{' '}
            <span className="text-gray-500">({adminFilterBanner.email})</span>
          </div>
          <button onClick={clearAdminFilter} className="text-sm text-brand-700 hover:underline">
            Limpar filtro
          </button>
        </div>
      )}

      {isFilteringByAdmin && adminSummary ? (
        <>
          <p className="text-xs text-gray-500 mb-2">
            Resumo abaixo refere-se apenas às ações deste administrador.
          </p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-6">
            <SummaryCard
              label="Valor total acionado"
              value={formatBRL(adminSummary.confirmedAmount + adminSummary.cancelledAmount)}
              sub={`${adminSummary.confirmedCount + adminSummary.cancelledCount} pedidos acionados`}
              accent="brand"
            />
            <SummaryCard
              label="Valor dos pedidos aprovados"
              value={formatBRL(adminSummary.confirmedAmount)}
              sub={`${adminSummary.confirmedCount} aprovados`}
              accent="green"
            />
            <SummaryCard
              label="Valor dos pedidos reprovados"
              value={formatBRL(adminSummary.cancelledAmount)}
              sub={`${adminSummary.cancelledCount} reprovados`}
              accent="red"
            />
          </div>
        </>
      ) : summary && (
        <>
          {customerEmail && isAdmin && (
            <p className="text-xs text-gray-500 mb-2">
              Resumo abaixo refere-se apenas a este cliente.
            </p>
          )}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-6">
            <SummaryCard
              label="Valor total dos pedidos"
              value={formatBRL(summary.totalAmount)}
              sub={`${summary.totalOrders} pedidos no total`}
              accent="brand"
            />
            <SummaryCard
              label="Valor dos pedidos aprovados"
              value={formatBRL(summary.confirmedAmount)}
              sub={`${summary.confirmedCount} aprovados`}
              accent="green"
            />
            <SummaryCard
              label="Valor dos pedidos em análise"
              value={formatBRL(summary.pendingAmount)}
              sub={`${summary.pendingCount} pendentes · ${summary.cancelledCount} cancelados`}
              accent="amber"
            />
          </div>
        </>
      )}

      {isFilteringByAdmin ? (
        <div className="flex flex-wrap gap-1 mb-3 border-b border-gray-200">
          <button
            onClick={() => setAdminScope('approved')}
            className={`px-4 py-2 text-sm font-medium rounded-t-md -mb-px border-b-2 transition-colors ${
              adminScope === 'approved'
                ? 'border-green-600 text-green-700'
                : 'border-transparent text-gray-600 hover:text-gray-900 hover:border-gray-300'
            }`}
          >
            Aprovados pelo admin
          </button>
          <button
            onClick={() => setAdminScope('rejected')}
            className={`px-4 py-2 text-sm font-medium rounded-t-md -mb-px border-b-2 transition-colors ${
              adminScope === 'rejected'
                ? 'border-red-600 text-red-700'
                : 'border-transparent text-gray-600 hover:text-gray-900 hover:border-gray-300'
            }`}
          >
            Reprovados pelo admin
          </button>
        </div>
      ) : (
        <div className="flex flex-wrap gap-1 mb-3 border-b border-gray-200">
          {STATUS_TABS.map((tab) => {
            const active = status === tab.id
            return (
              <button
                key={tab.id}
                onClick={() => setStatus(tab.id)}
                className={`px-4 py-2 text-sm font-medium rounded-t-md -mb-px border-b-2 transition-colors ${
                  active
                    ? 'border-brand-600 text-brand-700'
                    : 'border-transparent text-gray-600 hover:text-gray-900 hover:border-gray-300'
                }`}
              >
                {tab.label}
              </button>
            )
          })}
        </div>
      )}

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 mb-4">
          {error}
        </div>
      )}

      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase">
            <tr>
              <th className="px-4 py-3">#</th>
              <th className="px-4 py-3">Data</th>
              {isAdmin && <th className="px-4 py-3">Cliente</th>}
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Itens</th>
              <th className="px-4 py-3 text-right">Total</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading && (
              <tr><td colSpan={isAdmin ? 7 : 6} className="px-4 py-8 text-center text-gray-500">Carregando…</td></tr>
            )}
            {!loading && data?.content.length === 0 && (
              <tr>
                <td colSpan={isAdmin ? 7 : 6} className="px-4 py-12 text-center text-gray-500">
                  Nenhum pedido encontrado para o filtro atual.
                </td>
              </tr>
            )}
            {!loading && data?.content.map((o) => (
              <tr
                key={o.id}
                className="group hover:bg-gray-50 cursor-pointer transition-colors"
                onClick={() => navigate(`/orders/${o.id}`)}
              >
                <td className="px-4 py-3 font-medium">#{o.id}</td>
                <td className="px-4 py-3 text-gray-600">{formatDate(o.createdAt)}</td>
                {isAdmin && (
                  <td className="px-4 py-3">
                    <div className="text-gray-900">{o.customerName}</div>
                    <div className="text-xs text-gray-500">{o.customerEmail ?? '—'}</div>
                  </td>
                )}
                <td className="px-4 py-3"><StatusBadge status={o.status} /></td>
                <td className="px-4 py-3 text-gray-600">{o.items.length}</td>
                <td className="px-4 py-3 text-right font-medium">{formatBRL(o.totalAmount)}</td>
                <td className="px-4 py-3 text-right">
                  <ChevronRight className="w-5 h-5 text-gray-300 group-hover:text-gray-700 transition-colors inline-block" />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 text-sm">
          <span className="text-gray-600">
            Página {data.number + 1} de {data.totalPages} — {data.totalElements} pedidos
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={data.first}
              className="btn-secondary"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage(page + 1)}
              disabled={data.last}
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
