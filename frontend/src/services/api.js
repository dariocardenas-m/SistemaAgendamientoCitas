import axios from 'axios'

const api = axios.create({
  baseURL: '/api'
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('pa_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const metodo = err.config?.method?.toLowerCase()
    const url = err.config?.url || ''
    const esReserva = metodo === 'post' && /\/citas\/?$/.test(url)
    const esEdicion = (metodo === 'put' || metodo === 'patch') && /\/citas\/\d+/.test(url)
    if (err.response?.status === 409 && (esReserva || esEdicion)) {
      err.esConflictoReserva = true
    }
    if (err.response?.status === 401) {
      localStorage.removeItem('pa_token')
      localStorage.removeItem('pa_user')
    }
    return Promise.reject(err)
  }
)

export default api
