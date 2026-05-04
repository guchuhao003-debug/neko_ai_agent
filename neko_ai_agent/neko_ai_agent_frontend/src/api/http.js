import axios from 'axios'

const http = axios.create({
  baseURL: 'http://localhost:8123/api',
  timeout: 30000,
  withCredentials: true,
})

export const getApiUrl = (path, params = {}) => http.getUri({ url: path, params })

export default http
