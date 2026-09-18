import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    // Multipart uploads need a browser-generated boundary. The API client has a
    // JSON default for ordinary requests, so clear it before an upload leaves
    // the browser instead of sending FormData as application/json.
    if (typeof FormData !== "undefined" && config.data instanceof FormData) {
      if (typeof config.headers?.setContentType === "function") {
        config.headers.setContentType(undefined);
      } else if (config.headers) {
        delete config.headers["Content-Type"];
        delete config.headers["content-type"];
      }
    }

    const token = localStorage.getItem("algosphere_token");
    const assessmentGrant = sessionStorage.getItem("verdixa_assessment_grant");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    if (assessmentGrant) config.headers["X-Assessment-Access"] = assessmentGrant;

    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthRequest = error.config?.url?.startsWith("/auth/");
    if (error.response?.status === 401 && !isAuthRequest) {
      localStorage.removeItem("algosphere_token");
      localStorage.removeItem("algosphere_username");
      localStorage.removeItem("algosphere_role");
      if (window.location.pathname !== "/login") window.location.assign("/login");
    }
    return Promise.reject(error);
  }
);

export default api;
