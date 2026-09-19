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
    const esReserva = err.config?.method?.toLowerCase() === 'post' &&
      /\/citas\/?$/.test(err.config?.url || '')
    if (err.response?.status === 409 && esReserva) {
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
