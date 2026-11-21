import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, errMsg } from '../api.js'
import { useAuth } from './AuthContext.jsx'
import { toMin } from '../utils.js'

const DataContext = createContext(null)

const DEFAULT_CONFIG = { clinica: 'OdontoCare', abertura: '08:00', fechamento: '18:00', intervalo: 30, sabado: false }
const EMPTY = { pacientes: [], dentistas: [], procedimentos: [], agendamentos: [], config: DEFAULT_CONFIG }

const PATHS = { pacientes: '/pacientes', dentistas: '/dentistas', procedimentos: '/procedimentos', agendamentos: '/agendamentos' }

// O back-end usa ids numéricos; as telas comparam com o value dos <select> (string).
const str = (v) => (v === null || v === undefined ? v : String(v))

const normalize = {
  pacientes: (p) => ({ ...p, id: str(p.id), email: p.email || '', nascimento: p.nascimento || '', obs: p.obs || '' }),
  dentistas: (d) => ({ ...d, id: str(d.id), telefone: d.telefone || '' }),
  procedimentos: (p) => ({ ...p, id: str(p.id), valor: Number(p.valor) }),
  agendamentos: (a) => ({
    ...a,
    id: str(a.id),
    pacienteId: str(a.pacienteId),
    dentistaId: str(a.dentistaId),
    procedimentoId: str(a.procedimentoId),
    valor: a.valor == null ? null : Number(a.valor),
    obs: a.obs || '',
  }),
}

// Campos que vão para a API (o resto do objeto da tela é ignorado).
const toApi = {
  pacientes: (p) => ({
    nome: p.nome, cpf: p.cpf, telefone: p.telefone, email: p.email || null,
    nascimento: p.nascimento || null, convenio: p.convenio, obs: p.obs || null,
  }),
  dentistas: (d) => ({ nome: d.nome, cro: d.cro, especialidade: d.especialidade, telefone: d.telefone || null, cor: d.cor, ativo: d.ativo !== false }),
  procedimentos: (p) => ({ nome: p.nome, duracao: Number(p.duracao), valor: Number(p.valor), ativo: p.ativo !== false }),
  agendamentos: (a) => ({
    pacienteId: a.pacienteId, dentistaId: a.dentistaId, procedimentoId: a.procedimentoId,
    data: a.data, hora: a.hora, status: a.status, obs: a.obs || null,
  }),
}

export function DataProvider({ children }) {
  const { user } = useAuth()
  const [db, setDb] = useState(EMPTY)
  const [state, setState] = useState({ status: 'idle', error: null })

  const reload = useCallback(async () => {
    setState((s) => ({ status: s.status === 'ready' ? 'ready' : 'loading', error: null }))
    try {
      const [pacientes, dentistas, procedimentos, agendamentos, config] = await Promise.all([
        api('/pacientes'), api('/dentistas'), api('/procedimentos'), api('/agendamentos'), api('/configuracoes'),
      ])
      setDb({
        pacientes: pacientes.map(normalize.pacientes),
        dentistas: dentistas.map(normalize.dentistas),
        procedimentos: procedimentos.map(normalize.procedimentos),
        agendamentos: agendamentos.map(normalize.agendamentos),
        config: { ...DEFAULT_CONFIG, ...config },
      })
      setState({ status: 'ready', error: null })
    } catch (e) {
      setState({ status: 'error', error: errMsg(e) })
    }
  }, [])

  useEffect(() => {
    if (user) reload()
    else {
      setDb(EMPTY)
      setState({ status: 'idle', error: null })
    }
  }, [user, reload])

  const api_ = useMemo(() => {
    const byId = (col, id) => db[col].find((x) => x.id === str(id))

    const put = (col, item) => setDb((s) => ({ ...s, [col]: s[col].some((x) => x.id === item.id) ? s[col].map((x) => (x.id === item.id ? item : x)) : [...s[col], item] }))

    const crud = (col) => ({
      add: async (item) => {
        const novo = normalize[col](await api(PATHS[col], { method: 'POST', body: toApi[col](item) }))
        put(col, novo)
        return novo
      },
      update: async (id, data) => {
        const keys = Object.keys(data)
        let salvo
        if (col === 'agendamentos' && keys.length === 1 && keys[0] === 'status') {
          salvo = await api(`/agendamentos/${id}/status`, { method: 'PATCH', body: { status: data.status } })
        } else if (col === 'dentistas' && keys.length === 1 && keys[0] === 'ativo') {
          salvo = await api(`/dentistas/${id}`, { method: 'PATCH', body: { ativo: data.ativo } })
        } else {
          const atual = byId(col, id) || {}
          salvo = await api(`${PATHS[col]}/${id}`, { method: 'PUT', body: toApi[col]({ ...atual, ...data }) })
        }
        const item = normalize[col](salvo)
        put(col, item)
        return item
      },
      remove: async (id) => {
        await api(`${PATHS[col]}/${id}`, { method: 'DELETE' })
        setDb((s) => ({ ...s, [col]: s[col].filter((x) => x.id !== str(id)) }))
      },
    })

    /** Checagem local para pintar os horários livres; o back-end valida de novo ao salvar (409). */
    const findConflict = ({ id, data, hora, dentistaId, pacienteId, procedimentoId }) => {
      const proc = byId('procedimentos', procedimentoId)
      const ini = toMin(hora)
      const fim = ini + (proc?.duracao || 30)
      return db.agendamentos.find((a) => {
        if (a.id === id || a.data !== data || a.status === 'cancelado') return false
        if (a.dentistaId !== dentistaId && a.pacienteId !== pacienteId) return false
        const aIni = toMin(a.hora)
        const aFim = a.horaFim ? toMin(a.horaFim) : aIni + (byId('procedimentos', a.procedimentoId)?.duracao || 30)
        return ini < aFim && aIni < fim
      })
    }

    return {
      ...db,
      reload,
      pacientesApi: crud('pacientes'),
      dentistasApi: crud('dentistas'),
      procedimentosApi: crud('procedimentos'),
      agendamentosApi: crud('agendamentos'),
      byId,
      findConflict,
      setConfig: async (c) => {
        const salvo = await api('/configuracoes', { method: 'PUT', body: { ...db.config, ...c } })
        setDb((s) => ({ ...s, config: { ...DEFAULT_CONFIG, ...salvo } }))
      },
      // o back-end apaga os agendamentos em cascata; aqui só espelhamos no estado
      removePaciente: async (id) => {
        await api(`/pacientes/${id}`, { method: 'DELETE' })
        setDb((s) => ({ ...s, pacientes: s.pacientes.filter((x) => x.id !== id), agendamentos: s.agendamentos.filter((a) => a.pacienteId !== id) }))
      },
      removeDentista: async (id) => {
        await api(`/dentistas/${id}`, { method: 'DELETE' })
        setDb((s) => ({ ...s, dentistas: s.dentistas.filter((x) => x.id !== id), agendamentos: s.agendamentos.filter((a) => a.dentistaId !== id) }))
      },
      resetAll: async () => {
        await api('/backup/restaurar-exemplo', { method: 'POST' })
        await reload()
      },
      exportJSON: async () => JSON.stringify(await api('/backup'), null, 2),
      importJSON: async (txt) => {
        const d = JSON.parse(txt)
        if (!d.pacientes || !d.dentistas || !d.procedimentos || !d.agendamentos) throw new Error('Arquivo inválido')
        await api('/backup', { method: 'POST', body: d })
        await reload()
      },
    }
  }, [db, reload])

  if (user && state.status !== 'ready') {
    return (
      <div className="login-page">
        <div className="login-card" style={{ textAlign: 'center' }}>
          <div className="login-logo">🦷</div>
          {state.status === 'error' ? (
            <>
              <h2>Não foi possível carregar</h2>
              <p className="muted">{state.error}</p>
              <button className="btn primary block" onClick={reload}>Tentar de novo</button>
            </>
          ) : (
            <>
              <h2>Carregando…</h2>
              <p className="muted">Buscando os dados da clínica</p>
              <div style={{ display: 'grid', placeItems: 'center', marginTop: 12 }}><span className="spinner" /></div>
            </>
          )}
        </div>
      </div>
    )
  }

  return <DataContext.Provider value={api_}>{children}</DataContext.Provider>
}

export const useData = () => useContext(DataContext)
