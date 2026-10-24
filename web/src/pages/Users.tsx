import { useEffect, useMemo, useState } from 'react'
import { api, extractApiMessage } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ShieldCheck, ShieldOff } from '../components/Icons'
import type { Role, UserListItem } from '../types'

const PAGE_SIZE = 10

interface PendingRoleChange {
  user: UserListItem
  newRole: Role
}

function RoleBadge({ user }: { user: UserListItem }) {
  if (user.superAdmin) return <span className="badge bg-purple-100 text-purple-800">SUPER ADMIN</span>
  if (user.role === 'ADMIN') return <span className="badge bg-blue-100 text-blue-800">ADMIN</span>
  return <span className="badge bg-gray-100 text-gray-700">USER</span>
}

export function Users() {
  const { role, userEmail } = useAuth()
  const [users, setUsers] = useState<UserListItem[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState<PendingRoleChange | null>(null)
  const [running, setRunning] = useState(false)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  // Super admin é determinado pelo próprio servidor; aqui usamos um flag derivado:
  // o usuário logado é super-admin se ele aparece na lista com superAdmin=true.
  const me = users?.find((u) => u.email === userEmail)
  const iAmSuperAdmin = me?.superAdmin ?? false

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const { data } = await api.get<UserListItem[]>('/auth/users')
      setUsers(data)
    } catch (err) {
      setError(extractApiMessage(err, 'Falha ao carregar usuários'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (role === 'ADMIN') load()
  }, [role])

  const apply = async () => {
    if (!pending) return
    setRunning(true)
    setError(null)
    try {
      await api.patch(`/auth/users/${pending.user.id}/role`, { role: pending.newRole })
      setPending(null)
      await load()
    } catch (err) {
      setError(extractApiMessage(err, 'Falha ao atualizar role'))
      setPending(null)
    } finally {
      setRunning(false)
    }
  }

  const filtered = useMemo(() => {
    if (!users) return []
    const q = search.trim().toLowerCase()
    if (!q) return users
    return users.filter(
      (u) => u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
    )
  }, [users, search])

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const safePage = Math.min(page, totalPages - 1)
  const slice = filtered.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE)

  if (role !== 'ADMIN') {
    return <div className="text-gray-600">Acesso restrito a administradores.</div>
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Usuários cadastrados</h1>
        <p className="text-sm text-gray-500">
          {iAmSuperAdmin
            ? 'Como administrador original, você pode promover ou remover ADMIN de outros usuários.'
            : 'Apenas o administrador original pode promover ou remover ADMIN.'}
        </p>
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
              <th className="px-4 py-3">Nome</th>
              <th className="px-4 py-3">E-mail</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3">Cadastrado em</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading && (
              <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-500">Carregando…</td></tr>
            )}
            {!loading && slice.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-12 text-center text-gray-500">
                  {search ? 'Nenhum usuário bate com a busca.' : 'Nenhum usuário cadastrado.'}
                </td>
              </tr>
            )}
            {!loading && slice.map((u) => {
              const canActOnThis = iAmSuperAdmin && !u.superAdmin && u.email !== userEmail
              return (
                <tr key={u.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{u.name}</td>
                  <td className="px-4 py-3 text-gray-600">{u.email}</td>
                  <td className="px-4 py-3"><RoleBadge user={u} /></td>
                  <td className="px-4 py-3 text-gray-600">{new Date(u.createdAt).toLocaleDateString('pt-BR')}</td>
                  <td className="px-4 py-3 text-right">
                    {canActOnThis && u.role === 'USER' && (
                      <button
                        className="inline-flex items-center gap-1.5 rounded-full border border-gray-200 bg-white px-3 py-1 text-xs font-medium text-gray-700 hover:bg-gray-50 hover:border-gray-300 hover:text-gray-900 transition-colors"
                        onClick={() => setPending({ user: u, newRole: 'ADMIN' })}
                      >
                        <ShieldCheck />
                        Tornar admin
                      </button>
                    )}
                    {canActOnThis && u.role === 'ADMIN' && (
                      <button
                        className="inline-flex items-center gap-1.5 rounded-full border border-gray-200 bg-white px-3 py-1 text-xs font-medium text-gray-700 hover:bg-red-50 hover:border-red-200 hover:text-red-700 transition-colors"
                        onClick={() => setPending({ user: u, newRole: 'USER' })}
                      >
                        <ShieldOff />
                        Remover admin
                      </button>
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
            Página {safePage + 1} de {totalPages} — {filtered.length} usuário(s)
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

      <ConfirmDialog
        open={pending !== null}
        loading={running}
        title={pending?.newRole === 'ADMIN'
          ? `Tornar ${pending?.user.name} administrador?`
          : `Remover ADMIN de ${pending?.user.name}?`}
        description={pending?.newRole === 'ADMIN'
          ? <>O usuário ganhará acesso administrativo: poderá ver todos os pedidos, confirmar e cancelar manualmente. <strong>Não</strong> poderá promover outros usuários.</>
          : <>O usuário voltará a ter acesso de cliente comum.</>}
        confirmLabel={pending?.newRole === 'ADMIN' ? 'Tornar ADMIN' : 'Remover ADMIN'}
        variant={pending?.newRole === 'ADMIN' ? 'primary' : 'danger'}
        onConfirm={apply}
        onCancel={() => setPending(null)}
      />
    </div>
  )
}
