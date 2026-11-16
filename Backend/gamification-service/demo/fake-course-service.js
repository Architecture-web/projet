// Faux course-service pour la démo : répond à GET /api/courses/:courseId avec un titre.
// Usage : node demo/fake-course-service.js   (écoute sur le port 8082)
const http = require('http');

const TITLES = {
  '1': 'Spring Boot et Microservices',
  '2': 'Algorithmique en Java',
};

http.createServer((req, res) => {
  const m = req.url.match(/^\/api\/courses\/([^/?]+)/);
  if (req.method === 'GET' && m) {
    const id = decodeURIComponent(m[1]);
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ id, title: TITLES[id] || `Cours ${id}` }));
    console.log(`GET ${req.url} -> 200`);
  } else {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'not found' }));
  }
}).listen(8082, () => console.log('fake course-service on http://localhost:8082'));
