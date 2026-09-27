// Dev-server proxy: the browser calls /api on the Angular server, which forwards to Spring Boot.
// BACKEND_URL points at the backend service inside Docker; locally it defaults to localhost.
export default {
  '/api': {
    target: process.env.BACKEND_URL ?? 'http://localhost:8080',
    secure: false,
    changeOrigin: true,
  },
};
