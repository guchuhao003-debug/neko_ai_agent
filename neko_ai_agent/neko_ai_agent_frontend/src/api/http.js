import axios from 'axios'
import JSONBig from 'json-bigint'

const JSONBigInt = JSONBig({ storeAsString: true })
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  transformResponse: [
    (data) => {
      try {
        return JSONBigInt.parse(data)
      } catch {
        return data
      }
    }
  ],
})

export const getApiUrl = (path, params = {}) => http.getUri({ url: path, params })

export default http
