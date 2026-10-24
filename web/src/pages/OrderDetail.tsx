import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, extractApiMessage } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ArrowLeft } from '../components/Icons'
import type { Order, OrderStatus } from '../types'

type PendingAction = 'cancel' | 'confirm' | null

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

export function OrderDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { role } = useAuth()
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [pendingAction, setPendingAction] = useState<PendingAction>(null)
  const [actionRunning, setActionRunning] = useState(false)
  const [cancelReason, setCancelReason] = useState('')

  useEffect(() => {
    if (!id) return
    setLoading(true)
    api.get<Order>(`/orders/${id}`)
      .then((r) => setOrder(r.data))
      .catch((err) => setError(extractApiMessage(err, 'Pedido não encontrado')))
      .finally(() => setLoading(false))
  }, [id])

  const closeDialog = () => {
    setPendingAction(null)
    setCancelReason('')
  }

  const runAction = async () => {
    if (!order || !pendingAction) return
    setActionRunning(true)
    setError(null)
    try {
      if (pendingAction === 'cancel') {
        const body = cancelReason.trim() ? { reason: cancelReason.trim() } : {}
        const { data } = await api.patch<Order>(`/orders/${order.id}/cancel`, body)
        setOrder(data)
      } else {
        const { data } = await api.patch<Order>(`/orders/${order.id}/confirm`)
        setOrder(data)
      }
      closeDialog()
    } catch (err) {
      setError(extractApiMessage(err, pendingAction === 'cancel' ? 'Falha ao cancelar' : 'Falha ao confirmar'))
      closeDialog()
    } finally {
      setActionRunning(false)
    }
  }

  if (loading) return <div className="text-gray-500">Carregando…</div>
  if (error && !order) {
    return (
      <div>
        <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 mb-4">{error}</div>
        <button onClick={() => navigate('/orders')} className="btn-secondary">Voltar</button>
      </div>
    )
  }
  if (!order) return null

  return (
    <div>
      <Link to="/orders" className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-900 transition-colors">
        <ArrowLeft />
        Voltar
      </Link>
      <div className="flex items-center justify-between mt-2 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Pedido #{order.id}</h1>
          <p className="text-sm text-gray-500">Criado em {new Date(order.createdAt).toLocaleString('pt-BR')}</p>
        </div>
        <StatusBadge status={order.status} />
      </div>

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 mb-4">
          {error}
        </div>
      )}

      {role === 'ADMIN' && (
        <div className="card p-4 mb-4 bg-brand-50 border-brand-200">
          <p className="text-xs uppercase tracking-wide text-brand-700 font-medium mb-1">Cliente</p>
          <p className="text-sm font-medium text-gray-900">{order.customerName}</p>
          <p className="text-sm text-gray-600">{order.customerEmail ?? '(e-mail não disponível)'}</p>
        </div>
      )}

      {order.status === 'CONFIRMED' && order.confirmedAt && (
        <div className="card p-4 mb-4 bg-green-50 border-green-200">
          <p className="text-xs uppercase tracking-wide text-green-700 font-medium mb-2">Confirmação</p>
          <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-1 text-sm">
            <dt className="text-gray-500">Quando</dt>
            <dd className="text-gray-900">{new Date(order.confirmedAt).toLocaleString('pt-BR')}</dd>
            <dt className="text-gray-500">Por</dt>
            <dd className="text-gray-900">
              {order.confirmedByName ?? '—'}
              {order.confirmedByEmail && <span className="text-gray-500"> ({order.confirmedByEmail})</span>}
            </dd>
            <dt className="text-gray-500">Como</dt>
            <dd className="text-gray-900">
              {order.confirmationMethod === 'ADMIN_MANUAL' ? 'Confirmação manual por administrador' : (order.confirmationMethod ?? '—')}
            </dd>
          </dl>
        </div>
      )}

      {order.status === 'CANCELLED' && order.cancelledAt && (
        <div className="card p-4 mb-4 bg-red-50 border-red-200">
          <p className="text-xs uppercase tracking-wide text-red-700 font-medium mb-2">Cancelamento</p>
          <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-1 text-sm">
            <dt className="text-gray-500">Quando</dt>
            <dd className="text-gray-900">{new Date(order.cancelledAt).toLocaleString('pt-BR')}</dd>
            <dt className="text-gray-500">Por</dt>
            <dd className="text-gray-900">
              {order.cancelledByName ?? '—'}
              {order.cancelledByEmail && <span className="text-gray-500"> ({order.cancelledByEmail})</span>}
            </dd>
            <dt className="text-gray-500">Como</dt>
            <dd className="text-gray-900">
              {order.cancellationMethod === 'ADMIN_MANUAL'
                ? 'Cancelamento manual por administrador'
                : order.cancellationMethod === 'USER_REQUEST'
                ? 'Solicitação do cliente'
                : (order.cancellationMethod ?? '—')}
            </dd>
            <dt className="text-gray-500">Motivo</dt>
            <dd className="text-gray-900">{order.cancellationReason ?? '—'}</dd>
          </dl>
        </div>
      )}

      <div className="card overflow-hidden mb-4">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase">
            <tr>
              <th className="px-4 py-3">Produto</th>
              <th className="px-4 py-3 text-right">Qtd</th>
              <th className="px-4 py-3 text-right">Preço unit.</th>
              <th className="px-4 py-3 text-right">Subtotal</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {order.items.map((it) => (
              <tr key={it.id}>
                <td className="px-4 py-3">{it.productName}</td>
                <td className="px-4 py-3 text-right">{it.quantity}</td>
                <td className="px-4 py-3 text-right">{formatBRL(it.unitPrice)}</td>
                <td className="px-4 py-3 text-right font-medium">{formatBRL(it.subtotal)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot className="bg-gray-50">
            <tr>
              <td colSpan={3} className="px-4 py-3 text-right font-medium">Total</td>
              <td className="px-4 py-3 text-right font-bold">{formatBRL(order.totalAmount)}</td>
            </tr>
          </tfoot>
        </table>
      </div>

      {order.status === 'PENDING' && (
        <div className="flex gap-3">
          {role === 'ADMIN' && (
            <button onClick={() => setPendingAction('confirm')} className="btn-primary">
              Confirmar pedido
            </button>
          )}
          <button onClick={() => setPendingAction('cancel')} className="btn-danger">
            Cancelar pedido
          </button>
        </div>
      )}

      <ConfirmDialog
        open={pendingAction !== null}
        loading={actionRunning}
        title={pendingAction === 'confirm' ? `Confirmar pedido #${order.id}?` : `Cancelar pedido #${order.id}?`}
        description={
          pendingAction === 'confirm'
            ? <>O pedido vai para o status <strong>CONFIRMED</strong> e um e-mail é disparado para o cliente. Esta ação não pode ser desfeita.</>
            : <>O pedido vai para o status <strong>CANCELLED</strong>. Esta ação não pode ser desfeita.</>
        }
        confirmLabel={pendingAction === 'confirm' ? 'Confirmar' : 'Cancelar pedido'}
        cancelLabel="Voltar"
        variant={pendingAction === 'confirm' ? 'primary' : 'danger'}
        onConfirm={runAction}
        onCancel={closeDialog}
      >
        {pendingAction === 'cancel' && (
          <div>
            <label className="label">
              Motivo {role === 'ADMIN' ? '(opcional — será visível para o cliente)' : '(opcional)'}
            </label>
            <textarea
              className="input min-h-[80px]"
              maxLength={500}
              placeholder={role === 'ADMIN' ? 'Ex.: Estoque indisponível para o produto X' : 'Ex.: Mudei de ideia'}
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              disabled={actionRunning}
            />
            <p className="text-xs text-gray-500 mt-1">
              Se vazio, será usada uma mensagem padrão.
            </p>
          </div>
        )}
      </ConfirmDialog>
    </div>
  )
}
