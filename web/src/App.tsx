import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import { Layout } from './components/Layout'
import { Login } from './pages/Login'
import { Register } from './pages/Register'
import { Orders } from './pages/Orders'
import { OrderDetail } from './pages/OrderDetail'
import { NewOrder } from './pages/NewOrder'
import { Users } from './pages/Users'
import { Customers } from './pages/Customers'
import { AdminActivity } from './pages/AdminActivity'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
            <Route path="/orders" element={<Orders />} />
            <Route path="/orders/new" element={<NewOrder />} />
            <Route path="/orders/:id" element={<OrderDetail />} />
            <Route path="/customers" element={<Customers />} />
            <Route path="/admin-activity" element={<AdminActivity />} />
            <Route path="/users" element={<Users />} />
          </Route>
          <Route path="*" element={<Navigate to="/orders" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
