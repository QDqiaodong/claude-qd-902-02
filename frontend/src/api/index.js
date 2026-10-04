import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 统一拆掉 ApiResponse 包装：失败时抛出后端给的中文提示
http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.ok === false) {
      return Promise.reject(new Error(body.message || '业务校验未通过'))
    }
    return body
  },
  (err) => {
    const data = err.response && err.response.data
    const message = data && data.message ? data.message : err.message
    return Promise.reject(new Error(message))
  }
)

const get = (url, params) => http.get(url, { params })
const post = (url, data) => http.post(url, data)

export const intakeApi = {
  records: () => get('/intake/records'),
  dailyStat: () => get('/intake/daily-stat'),
  sourceSummary: () => get('/intake/source-summary'),
  dict: () => get('/intake/dict'),
  create: (data) => post('/intake/records', data),
  update: (id, data) => http.put(`/intake/records/${id}`, data),
  remove: (id) => http.delete(`/intake/records/${id}`)
}

export const pitApi = {
  list: () => get('/pits'),
  dict: () => get('/pits/dict'),
  unload: (pitNo, data) => post(`/pits/${pitNo}/unload`, data),
  feed: (pitNo, data) => post(`/pits/${pitNo}/feed`, data),
  leachate: (pitNo, data) => post(`/pits/${pitNo}/leachate`, data),
  ferment: (pitNo, days, tempC) => post(`/pits/${pitNo}/ferment?days=${days}&tempC=${tempC}`)
}

export const furnaceApi = {
  list: () => get('/incinerators'),
  dict: () => get('/incinerators/dict'),
  run: (code, data) => post(`/incinerators/${code}/run`, data),
  judge: (code) => post(`/incinerators/${code}/judge`, {}),
  statsByType: () => get('/incinerators/stats/type'),
  monthly: () => get('/incinerators/stats/monthly'),
  fastGrate: (speed) => get('/incinerators/fast-grate', { speed }),
  logs: (code) => get('/incinerators/logs', { furnaceCode: code })
}

export const powerApi = {
  boilers: () => get('/power/boilers'),
  turbines: () => get('/power/turbines'),
  dict: () => get('/power/dict'),
  adjustBoiler: (boilerNo, data) => post(`/power/boilers/${boilerNo}/adjust`, data),
  settlements: (unitNo) => get('/power/daily-settlements', unitNo ? { unitNo } : undefined),
  settle: (unitNo, data) => post(`/power/turbines/${unitNo}/daily-settlement`, data),
  verifyMeter: (id, data) => post(`/power/daily-settlements/${id}/verify`, data),
  traces: (id) => get(`/power/daily-settlements/${id}/traces`),
  unitTraces: (unitNo) => get(`/power/turbines/${unitNo}/meter-traces`)
}

export const envApi = {
  dashboard: () => get('/environment/dashboard'),
  emissions: () => get('/environment/emissions'),
  gauges: () => get('/environment/gauges'),
  dict: () => get('/environment/dict'),
  register: (data) => post('/environment/emissions', data),
  alarms: () => get('/environment/alarms'),
  handleAlarm: (id, note) => post(`/environment/alarms/${id}/handle`, { handleNote: note }),
  exceedRank: () => get('/environment/exceed-rank'),
  ash: () => get('/environment/ash'),
  registerAsh: (data) => post('/environment/ash', data)
}

export const disposalApi = {
  list: (activeOnly = true) => get('/disposals', { activeOnly }),
  detail: (id) => get(`/disposals/${id}`),
  evidences: (id) => get(`/disposals/${id}/evidences`),
  traces: (id) => get(`/disposals/${id}/traces`),
  condition: (id) => get(`/disposals/${id}/condition`),
  frozenFurnaces: () => get('/disposals/frozen-furnaces'),
  dict: () => get('/disposals/dict'),
  open: (data) => post('/disposals/open', data),
  measure: (id, data) => post(`/disposals/${id}/measures`, data),
  review: (id, data) => post(`/disposals/${id}/review`, data),
  close: (id, data) => post(`/disposals/${id}/close`, data),
  missingChoice: (id, data) => post(`/disposals/${id}/missing-choice`, data)
}

export default http
