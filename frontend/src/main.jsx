import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { SiteConfigProvider } from './context/SiteConfigContext'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <SiteConfigProvider>
      <App />
    </SiteConfigProvider>
  </StrictMode>,
)
