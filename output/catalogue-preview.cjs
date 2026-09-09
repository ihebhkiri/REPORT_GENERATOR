const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../Frontend/Rhis_report_gen/dist/Rhis_report_gen/browser');
const configuration = {datasets: [{id: 1, displayName: 'Employés', active: true, displayMain: true,
  displayRelated: false, visibleFieldCount: 2, description: 'Informations sur le personnel du restaurant.',
  aliases: 'Salariés\nPersonnel', fields: [
    {id: 11, displayName: 'Nom', active: true, visible: true, description: 'Nom de famille du salarié.', aliases: 'Patronyme'},
    {id: 12, displayName: "Date d’embauche", active: true, visible: true, description: "Date de début d’emploi.", aliases: "Date d’entrée"},
    {id: 13, displayName: 'Référence interne', active: true, visible: false, description: '', aliases: ''}
  ]}]};
http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://127.0.0.1');
  if (url.pathname === '/auth/me') {
    res.setHeader('Content-Type', 'application/json');
    return res.end(JSON.stringify({email: 'demo@example.test', roles: ['ROLE_ADMIN']}));
  }
  if (url.pathname === '/admin/dataset-exposure') {
    if (req.method === 'PUT') {
      let body = ''; for await (const chunk of req) body += chunk;
      for (const update of JSON.parse(body).datasets) {
        const dataset = configuration.datasets.find(item => item.id === update.id);
        for (const field of update.fields) Object.assign(dataset.fields.find(item => item.id === field.id), field);
        const {fields, ...metadata} = update; Object.assign(dataset, metadata);
      }
    }
    res.setHeader('Content-Type', 'application/json'); return res.end(JSON.stringify(configuration));
  }
  const requested = path.resolve(root, '.' + decodeURIComponent(url.pathname));
  if (!requested.startsWith(root + path.sep)) { res.writeHead(404); return res.end(); }
  const file = fs.existsSync(requested) && fs.statSync(requested).isFile() ? requested : path.join(root, 'index.html');
  const mime = {'.html':'text/html', '.js':'text/javascript', '.css':'text/css', '.svg':'image/svg+xml', '.woff2':'font/woff2', '.png':'image/png'};
  res.setHeader('Content-Type', mime[path.extname(file)] || 'application/octet-stream');
  fs.createReadStream(file).pipe(res);
}).listen(4301, '127.0.0.1', () => console.log('Preview with synthetic data: http://127.0.0.1:4301/administration/datasets'));
