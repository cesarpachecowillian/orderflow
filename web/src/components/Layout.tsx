import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `block rounded-md px-3 py-2 text-sm font-medium ${
    isActive ? 'bg-brand-50 text-brand-700' : 'text-gray-700 hover:bg-gray-100'
  }`

export function Layout() {
  const { userName, userEmail, role, logout } = useAuth()
  const navigate = useNavigate()
  const isAdmin = role === 'ADMIN'

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="h-screen flex overflow-hidden">
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col flex-shrink-0">
        <div className="px-6 py-5 border-b border-gray-200">
          <Link to="/orders" className="text-xl font-bold text-brand-600">OrderFlow</Link>
          <p className="text-xs text-gray-500 mt-1">Gestão de pedidos</p>
        </div>
        <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
          <NavLink to="/orders" className={navLinkClass} end>
            {isAdmin ? 'Pedidos' : 'Meus pedidos'}
          </NavLink>
          <NavLink to="/orders/new" className={navLinkClass}>
            Novo pedido
          </NavLink>
          {isAdmin && (
            <>
              <div className="px-3 pt-4 pb-1 text-xs font-semibold uppercase tracking-wide text-gray-400">
                Administração
              </div>
              <NavLink to="/customers" className={navLinkClass}>
                Pedidos por Cliente
              </NavLink>
              <NavLink to="/admin-activity" className={navLinkClass}>
                Atividade dos Admins
              </NavLink>
              <NavLink to="/users" className={navLinkClass}>
                Usuários
              </NavLink>
            </>
          )}
        </nav>
        <div className="border-t border-gray-200 p-4">
          <p className="text-sm font-medium text-gray-900 truncate">{userName}</p>
          <p className="text-xs text-gray-500 truncate mb-3">{userEmail}</p>
          <button onClick={handleLogout} className="btn-secondary w-full">
            Sair
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-y-auto">
        <div className="max-w-5xl mx-auto px-6 py-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
