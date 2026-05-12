import http from './http'

export function getModelList() {
  return http.get('/ai/models')
}
