import axios from 'axios'
import JSONBig from 'json-bigint'

const JSONBigInt = JSONBig({ storeAsString: true })

const http = axios.create({
  baseURL: 'http://localhost:8123/api',
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
