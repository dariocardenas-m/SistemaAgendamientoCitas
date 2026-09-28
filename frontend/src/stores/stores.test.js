import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './authStore'
import { useCitasStore } from './citasStore'
import api from '../services/api'
vi.mock('../services/api', () => ({ default: { get: vi.fn(), post: vi.fn() } }))

beforeEach(() => {
  vi.resetAllMocks()
  const data = new Map()
  vi.stubGlobal('localStorage', {
    getItem: k => data.get(k) ?? null,
    setItem: (k, v) => data.set(k, String(v)),
    removeItem: k => data.delete(k)
  })
  setActivePinia(createPinia())
})
describe('Sesion y roles', () => {
  it('sesion inicial no autenticada', () => expect(useAuthStore().isAuthenticated).toBe(false))
  it('login conserva token y perfil', async () => {
    api.post.mockResolvedValue({ data: { token: 'jwt-test', usuario: { id: 1, rol: 'PACIENTE' } } })
    const s = useAuthStore(); await s.login('p@example.test', 'test123')
    expect(api.post).toHaveBeenCalledWith('/auth/login', { email: 'p@example.test', password: 'test123' })
    expect(s.isAuthenticated).toBe(true); expect(localStorage.getItem('pa_token')).toBe('jwt-test')
  })
  it('error de login no crea sesion', async () => {
    api.post.mockRejectedValue(new Error('401')); const s = useAuthStore()
    await expect(s.login('p@example.test', 'incorrecta')).rejects.toThrow('401')
    expect(s.isAuthenticated).toBe(false)
  })
  it('registro inicia sesion con el perfil devuelto', async () => {
    const usuario = { id: 2, rol: 'PACIENTE' }
    api.post.mockResolvedValue({ data: { token: 'jwt-test', usuario } })
    const s = useAuthStore(); expect(await s.registro({ nombreCompleto: 'Prueba' })).toEqual(usuario)
    expect(s.usuario).toEqual(usuario)
  })
  it.each([['PACIENTE','/agendar'],['AGENDADOR','/agendador'],['ADMINISTRADOR','/admin'],['MEDICO_TERAPISTA','/']])
    ('dirige %s a %s', (rol, ruta) => {
      localStorage.setItem('pa_user', JSON.stringify({ rol })); expect(useAuthStore().homePorRol()).toBe(ruta)
    })
  it('logout elimina credenciales y estado', async () => {
    api.post.mockResolvedValue({ data: { token: 'jwt-test', usuario: { rol: 'PACIENTE' } } })
    const s = useAuthStore(); await s.login('p','clave'); s.logout()
    expect(s.isAuthenticated).toBe(false); expect(s.usuario).toBeNull(); expect(localStorage.getItem('pa_token')).toBeNull()
  })
})
describe('Flujo de citas', () => {
  function preparado() { const s = useCitasStore(); s.medico={ id: 3 }; s.fecha='2027-01-05'; s.hora='08:00'; return s }
  it('confirmar envia seleccion y conserva comprobante', async () => {
    api.post.mockResolvedValue({ data: { id: 7, codigo: 'CITA-7' } }); const s = preparado()
    expect(await s.confirmar()).toEqual({ id: 7, codigo: 'CITA-7' })
    expect(api.post).toHaveBeenCalledWith('/citas', expect.objectContaining({ medicoId: 3, fecha: '2027-01-05', hora: '08:00' }))
    expect(s.citaConfirmada.id).toBe(7)
  })
  it('conflicto informa y no confirma cita', async () => {
    api.post.mockRejectedValue({ esConflictoReserva: true }); const s = preparado()
    await expect(s.confirmar()).rejects.toMatchObject({ esConflictoReserva: true })
    expect(s.conflictoReserva).toContain('ocupada'); expect(s.citaConfirmada).toBeNull()
    s.cerrarConflictoReserva(); expect(s.conflictoReserva).toBe('')
  })
  it('error de red no se presenta como conflicto de reserva', async () => {
    api.post.mockRejectedValue(new Error('network')); const s=preparado()
    await expect(s.confirmar()).rejects.toThrow('network'); expect(s.conflictoReserva).toBe('')
  })
  it('reinicia seleccion y comprobante', () => {
    const s=preparado(); s.paso=4; s.citaConfirmada={id:7}; s.resetFlujo()
    expect(s.paso).toBe(1); expect(s.medico).toBeNull(); expect(s.hora).toBe(''); expect(s.citaConfirmada).toBeNull()
  })
  it('consulta disponibilidad con profesional y fecha', async () => {
    const datos={libres:['08:00'],ocupadas:['08:30'],intervaloMinutos:30}; api.get.mockResolvedValue({data:datos})
    const s=preparado(); await s.cargarDisponibilidad(3,'2027-01-05')
    expect(api.get).toHaveBeenCalledWith('/citas/disponibilidad',{params:{medicoId:3,fecha:'2027-01-05'}})
    expect(s.disponibilidad).toEqual(datos)
  })
  it('carga citas del paciente autenticado', async () => {
    api.get.mockResolvedValue({data:[{id:1}]}); const s=useCitasStore(); await s.cargarMisCitas()
    expect(api.get).toHaveBeenCalledWith('/citas/mias'); expect(s.misCitas).toEqual([{id:1}])
  })
})
