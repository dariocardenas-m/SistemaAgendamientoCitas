import { test, expect } from '@playwright/test'

const meses = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
]

test('paciente cancela su cita y el agendador consulta el reporte mensual', async ({ page }) => {
  await page.goto('/login')
  await expect.poll(async () => page.evaluate(async () => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'juan.perez@email.com', password: 'paciente123' })
    })
    return response.status
  }), { timeout: 30_000, message: 'El backend debe terminar de cargar el usuario semilla' }).toBe(200)
  await page.getByLabel('Correo electrónico').fill('juan.perez@email.com')
  await page.getByLabel('Contraseña').fill('paciente123')
  await page.getByRole('button', { name: 'Iniciar Sesión' }).click()
  await expect(page.getByRole('heading', { name: 'Agendar Cita Médica' })).toBeVisible()

  await page.getByRole('button', { name: /Medicina General/ }).first().click()
  await page.getByRole('button', { name: /Continuar al Paso 2/ }).click()
  await page.getByRole('button', { name: /Laura Martínez/ }).click()
  await page.getByRole('button', { name: /Continuar al Paso 3/ }).click()

  const minimo = new Date()
  minimo.setDate(minimo.getDate() + 2)
  const fechaMinima = `${minimo.getFullYear()}-${String(minimo.getMonth() + 1).padStart(2, '0')}-${String(minimo.getDate()).padStart(2, '0')}`
  const calendario = await page.evaluate(async () => {
    const medicos = await (await fetch('/api/medicos')).json()
    const medico = medicos.find(item => item.nombreCompleto.includes('Laura'))
    const fechas = await (await fetch(`/api/citas/fechas-habiles?medicoId=${medico.id}`)).json()
    return { medico, fechas }
  })
  const indiceFecha = calendario.fechas.findIndex(fecha => fecha >= fechaMinima)
  expect(indiceFecha, `Debe haber una fecha hábil desde ${fechaMinima}`).toBeGreaterThanOrEqual(0)
  const fechaElegida = calendario.fechas[indiceFecha]
  await page.locator('.grid-cols-2 button').nth(indiceFecha).click()
  await page.getByRole('button', { name: /Continuar al Paso 4/ }).click()

  const horaLibre = page.locator('.grid-cols-3 button:not([disabled])').first()
  const horaElegida = (await horaLibre.innerText()).trim()
  await horaLibre.click()
  await page.getByRole('button', { name: /Continuar al Paso 5/ }).click()
  await page.getByRole('button', { name: 'Confirmar Cita' }).click()
  await expect(page.getByRole('heading', { name: 'Cita agendada correctamente' })).toBeVisible()
  await page.getByRole('button', { name: 'Ver mis citas' }).click()
  await expect(page.getByRole('heading', { name: 'Mis Citas Médicas' })).toBeVisible()

  await page.getByRole('button', { name: 'Cancelar Cita' }).click()
  await page.getByLabel('Justificación').fill('Prueba de aceptación del tercer corte')
  await page.getByRole('button', { name: 'Confirmar cancelación' }).click()
  await expect(page.getByText('Justificación de cancelación: Prueba de aceptación del tercer corte')).toBeVisible()

  const disponibilidad = await page.evaluate(async ({ medicoId, fecha }) => {
    const response = await fetch(`/api/citas/disponibilidad?medicoId=${medicoId}&fecha=${fecha}`)
    return response.json()
  }, { medicoId: calendario.medico.id, fecha: fechaElegida })
  expect(disponibilidad.libres.map(hora => hora.slice(0, 5))).toContain(horaElegida.slice(0, 5))

  await page.locator('button[title="Cerrar sesión"]').click()
  await page.goto('/login')
  await page.getByLabel('Correo electrónico').fill('carlos.mendoza@piedrazul.com')
  await page.getByLabel('Contraseña').fill('agendador123')
  await page.getByRole('button', { name: 'Iniciar Sesión' }).click()
  await page.getByRole('link', { name: 'Reporte anual' }).click()
  await expect(page.getByRole('heading', { name: 'Citas por mes y profesional' })).toBeVisible()

  const anio = Number(fechaElegida.slice(0, 4))
  await page.getByLabel('Año').fill(String(anio))
  await page.getByRole('button', { name: 'Generar reporte' }).click()
  await expect(page.getByRole('heading', { name: `Actividad mensual · ${anio}` })).toBeVisible()
  await page.getByRole('button', { name: 'Mensual' }).click()
  await page.getByLabel('Mes').selectOption(String(Number(fechaElegida.slice(5, 7))))
  await expect(page.getByRole('heading', { name: `Actividad de ${meses[Number(fechaElegida.slice(5, 7)) - 1]} · ${anio}` })).toBeVisible()

  const filaLaura = page.getByRole('row').filter({ hasText: 'Laura Martínez' })
  const celdas = await filaLaura.locator('td').allTextContents()
  expect(celdas.map(texto => texto.trim())).toEqual(['0', '0', '1', '1'])
})
