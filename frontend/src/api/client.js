import axios from 'axios';

const api = axios.create({
  // VITE_API_URL이 http://localhost:8080 이라면 자동으로 뒤에 /api를 붙임
  baseURL: `${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api`,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true
});

export default api;
