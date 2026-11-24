import { defineConfig, devices } from '@playwright/test'
import { fileURLToPath } from 'node:url'

const frontendDir = fileURLToPath(new URL('.', import.meta.url))
const backendDir = fileURLToPath(new URL('../backend/', import.meta.url))
const ci = Boolean(process.env.CI)

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: ci,
  retries: ci ? 1 : 0,
  workers: ci ? 1 : undefined,
  reporter: ci ? [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]] : 'list',
  use: {
    ...devices['Desktop Chrome'],
    baseURL: 'http://127.0.0.1:15173',
    timezoneId: 'America/Bogota',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure'
  },
  webServer: [
    {
      command: 'mvn -q spring-boot:run',
      cwd: backendDir,
      url: 'http://127.0.0.1:18080/api/especialidades',
      env: { PORT: '18080', APP_CORS_ALLOWED_ORIGINS: 'http://127.0.0.1:15173' },
      reuseExistingServer: false,
      timeout: 120_000
    },
    {
      command: 'npm run dev -- --host 127.0.0.1 --port 15173',
      cwd: frontendDir,
      url: 'http://127.0.0.1:15173',
      env: { VITE_API_PROXY_TARGET: 'http://127.0.0.1:18080', VITE_DEV_PORT: '15173' },
      reuseExistingServer: false,
      timeout: 120_000
    }
  ]
})
