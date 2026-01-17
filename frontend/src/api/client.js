import axios from 'axios';

const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api`,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true
});

// 요청 시 localStorage에 저장된 패스코드를 헤더에 포함
api.interceptors.request.use(config => {
  const passcode = localStorage.getItem('pam_passcode');
  if (passcode) {
    config.headers['X-PAM-Auth'] = passcode;
  }
  return config;
});

export const setStoredPasscode = (passcode) => {
  localStorage.setItem('pam_passcode', passcode);
};

export const clearStoredPasscode = () => {
  localStorage.removeItem('pam_passcode');
};

export default api;
