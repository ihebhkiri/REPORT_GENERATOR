const pptxgen = require('pptxgenjs');
const JSZip = require('jszip');
const path = require('path');
const fs = require('fs');

const pptx = new pptxgen();
pptx.layout = 'LAYOUT_WIDE';
pptx.author = 'RHIS';
pptx.subject = 'Architecture et flux techniques du générateur de rapports RHIS';
pptx.title = 'RHIS Report Generator';
pptx.company = 'RHIS Solutions';
pptx.lang = 'fr-FR';
pptx.theme = {
  headFontFace: 'Cambria', bodyFontFace: 'Arial', lang: 'fr-FR'
};
const C = {
  navy: '102A43', navy2: '173F5F', teal: '00A6A6', mint: '5AD8C1',
  ink: '172B4D', muted: '5E6C84', pale: 'EAF5F5', bluePale: 'EAF1F8',
  white: 'FFFFFF', bg: 'F7F9FC', line: 'CFD8E3', green: '1F8A70',
  amber: 'D97706', red: 'B42318', darkBg: '0A2239'
};
const W = 13.333, H = 7.5;
const logo = path.resolve(__dirname, '../../Frontend/Rhis_report_gen/public/rhis-solutions-logo.png');
const output = path.resolve(__dirname, '../../docs/presentations/RHIS-presentation-technique.pptx');

function tx(slide, text, x, y, w, h, options = {}) {
  slide.addText(text, {
    x, y, w, h, fontFace: options.fontFace || 'Arial', fontSize: options.fontSize || 18,
    color: options.color || C.ink, bold: options.bold || false, margin: options.margin ?? 0,
    valign: options.valign || 'mid', align: options.align || 'left', breakLine: false,
    fit: 'shrink', ...options
  });
}
function rect(slide, x, y, w, h, fill, radius = 0.12, line = fill) {
  slide.addShape(pptx.ShapeType.roundRect, { x, y, w, h, rectRadius: radius, fill: { color: fill }, line: { color: line, width: 1 } });
}
function line(slide, x, y, w, h, color = C.line, width = 1.5, endArrowType) {
  slide.addShape(pptx.ShapeType.line, { x, y, w, h, line: { color, width, endArrowType } });
}
function circle(slide, x, y, d, fill, text, color = C.white, fontSize = 15) {
  slide.addShape(pptx.ShapeType.ellipse, { x, y, w: d, h: d, fill: { color: fill }, line: { color: fill } });
  tx(slide, text, x, y, d, d, { align: 'center', bold: true, color, fontSize });
}
function title(slide, t, sub) {
  slide.background = { color: C.bg };
  tx(slide, t, 0.65, 0.42, 11.9, 0.55, { fontFace: 'Cambria', fontSize: 30, bold: true, color: C.navy });
  if (sub) tx(slide, sub, 0.68, 1.02, 11.6, 0.34, { fontSize: 13.5, color: C.muted });
  tx(slide, 'RHIS', 0.62, 7.08, 0.7, 0.18, { fontSize: 8, bold: true, color: '7A8798' });
  tx(slide, `Architecture technique  ${pptx._slides.length}`, 10.75, 7.08, 1.95, 0.18, { fontSize: 8, color: '7A8798', align: 'right' });
}
function card(slide, x, y, w, h, heading, body, accent = C.teal) {
  rect(slide, x, y, w, h, C.white, 0.12, C.line);
  circle(slide, x + 0.22, y + 0.22, 0.34, accent, '');
  tx(slide, heading, x + 0.68, y + 0.18, w - 0.9, 0.38, { fontSize: 18, bold: true, color: C.navy });
  tx(slide, body, x + 0.22, y + 0.72, w - 0.44, h - 0.9, { fontSize: 14.5, color: C.muted, valign: 'top', breakLine: false });
}
function node(slide, x, y, w, h, heading, body, fill = C.white, border = C.line) {
  rect(slide, x, y, w, h, fill, 0.1, border);
  tx(slide, heading, x + 0.16, y + 0.12, w - 0.32, 0.3, { fontSize: 15.5, bold: true, color: C.navy, align: 'center' });
  if (body) tx(slide, body, x + 0.16, y + 0.5, w - 0.32, h - 0.58, { fontSize: 11.5, color: C.muted, align: 'center', valign: 'top' });
}
function note(slide, text) { slide.addNotes(text); }

// 1 — Cover
{
  const s = pptx.addSlide(); s.background = { color: C.darkBg };
  s.addShape(pptx.ShapeType.ellipse, { x: 9.0, y: 0.2, w: 4.1, h: 4.1, fill: { color: C.teal, transparency: 80 }, line: { color: C.teal, transparency: 100 } });
  s.addShape(pptx.ShapeType.ellipse, { x: 10.45, y: 4.05, w: 2.65, h: 2.65, fill: { color: C.mint, transparency: 84 }, line: { color: C.mint, transparency: 100 } });
  if (fs.existsSync(logo)) s.addImage({ path: logo, x: 0.7, y: 0.55, w: 1.75, h: 0.62, transparency: 0 });
  tx(s, 'RHIS Report Generator', 0.72, 2.0, 8.6, 0.9, { fontFace: 'Cambria', fontSize: 42, bold: true, color: C.white });
  tx(s, 'Architecture, génération de rapports et assistant LLM contrôlé', 0.76, 3.02, 7.8, 0.62, { fontSize: 21, color: 'D8E8F4' });
  tx(s, 'Angular 20, Spring Boot 4.1, PostgreSQL, XKiro', 0.76, 4.08, 7.7, 0.38, { fontSize: 15, color: C.mint, bold: true });
  tx(s, 'Présentation technique, 15 minutes', 0.76, 6.48, 4.2, 0.3, { fontSize: 12, color: 'B9CAD8' });
  note(s, 'Objectif : expliquer les frontières de responsabilité et les flux critiques du système, puis montrer comment le mode manuel et le mode LLM convergent vers le même pipeline sécurisé.');
}

// 2 — Business problem
{
  const s = pptx.addSlide(); title(s, 'Problème métier et objectif', 'Rendre les données RH exploitables sans exposer la complexité SQL');
  tx(s, 'Avant', 0.8, 1.65, 2.0, 0.38, { fontSize: 18, bold: true, color: C.red });
  tx(s, 'Rapports dépendants de requêtes techniques, peu réutilisables et difficiles à sécuriser.', 0.8, 2.1, 3.45, 1.55, { fontFace: 'Cambria', fontSize: 25, color: C.ink, valign: 'top' });
  line(s, 4.55, 2.75, 1.15, 0, C.teal, 3, 'triangle');
  tx(s, 'Objectif RHIS', 5.95, 1.65, 2.4, 0.38, { fontSize: 18, bold: true, color: C.green });
  tx(s, 'L’utilisateur choisit des données métier, configure le résultat et télécharge un fichier validé par le backend.', 5.95, 2.1, 5.65, 1.55, { fontFace: 'Cambria', fontSize: 25, color: C.navy, valign: 'top' });
  rect(s, 5.95, 4.35, 5.7, 1.05, C.pale, 0.12, C.pale);
  tx(s, 'Deux entrées, une seule chaîne de confiance', 6.25, 4.55, 5.1, 0.3, { fontSize: 19, bold: true, color: C.navy, align: 'center' });
  tx(s, 'Configuration manuelle  +  langage naturel', 6.25, 4.9, 5.1, 0.26, { fontSize: 14, color: C.teal, align: 'center' });
  note(s, 'Le besoin ne consiste pas seulement à produire un fichier. Il faut contrôler quelles tables et colonnes sont visibles, valider la définition, puis isoler chaque génération par utilisateur.');
}

// 3 — Scope
{
  const s = pptx.addSlide(); title(s, 'Périmètre fonctionnel réel', 'Fonctions présentes dans le code courant');
  card(s, 0.72, 1.55, 3.75, 1.55, 'Rapport manuel', 'Dataset principal et sources liées\nColonnes, filtres et tris\nPreview avant génération', C.teal);
  card(s, 4.78, 1.55, 3.75, 1.55, 'Assistant naturel', 'Demande en français\nClarification si nécessaire\nPlan structuré validé côté serveur', C.navy2);
  card(s, 8.84, 1.55, 3.75, 1.55, 'Administration', 'Activation des datasets\nExposition principale ou liée\nVisibilité des champs', C.amber);
  line(s, 1.7, 4.35, 9.9, 0, C.line, 2);
  const steps = [['1', 'Preview', 'Lecture limitée'], ['2', 'Génération', 'Job asynchrone'], ['3', 'Export', 'PDF ou XLSX'], ['4', 'Téléchargement', 'Fichier owner-scoped']];
  steps.forEach((v, i) => { const x = 1.0 + i * 3.0; circle(s, x, 4.08, 0.55, i === 3 ? C.green : C.teal, v[0]); tx(s, v[1], x - 0.25, 4.82, 1.1, 0.26, { align: 'center', bold: true, fontSize: 14 }); tx(s, v[2], x - 0.55, 5.18, 1.7, 0.28, { align: 'center', color: C.muted, fontSize: 11.5 }); });
  tx(s, 'Absent aujourd’hui : templates partagés persistants, historique conversationnel, worker externe', 1.2, 6.18, 10.9, 0.36, { align: 'center', fontSize: 13.5, color: C.muted });
  note(s, 'Sources : routes Angular, services de reporting, DataSetAdministrationController et documentation RHIS/docs/flows. Le brouillon manuel reste uniquement dans sessionStorage.');
}

// 4 — Global architecture
{
  const s = pptx.addSlide(); title(s, 'Architecture globale', 'Les frontières limitent la confiance accordée à chaque entrée');
  node(s, 0.65, 2.35, 2.2, 1.35, 'Angular 20', 'Standalone components\nSignals + RxJS', C.white, C.teal);
  node(s, 3.35, 2.0, 2.55, 2.05, 'API Spring Boot', 'SecurityFilterChain\nControllers REST\nServices métier', C.bluePale, C.navy2);
  node(s, 6.55, 1.45, 2.2, 1.25, 'PostgreSQL', 'Catalogue, jobs\net données RH', C.white, C.line);
  node(s, 6.55, 3.45, 2.2, 1.25, 'Filesystem', 'Snapshots et exports\ntemporaires', C.white, C.line);
  node(s, 9.55, 2.35, 2.4, 1.35, 'XKiro', 'Endpoint compatible OpenAI\nqwen/qwen3-max:free', C.pale, C.teal);
  line(s, 2.85, 3.02, 0.5, 0, C.teal, 2.2, 'triangle');
  line(s, 5.9, 2.55, 0.65, -0.45, C.navy2, 2, 'triangle');
  line(s, 5.9, 3.5, 0.65, 0.45, C.navy2, 2, 'triangle');
  line(s, 9.55, 3.02, -0.8, 0, C.teal, 2.2, 'triangle');
  tx(s, 'HTTPS + cookies', 2.82, 2.55, 0.92, 0.25, { fontSize: 10.5, color: C.muted, align: 'center' });
  tx(s, 'JPA + JDBC', 5.72, 1.72, 1.0, 0.25, { fontSize: 10.5, color: C.muted, align: 'center' });
  tx(s, 'structured output', 8.72, 2.55, 1.0, 0.25, { fontSize: 10.5, color: C.muted, align: 'center' });
  rect(s, 2.05, 5.45, 9.15, 0.72, C.navy, 0.12, C.navy);
  tx(s, 'Le backend construit le SQL et décide si une définition peut être exécutée', 2.35, 5.63, 8.55, 0.34, { align: 'center', fontSize: 18, bold: true, color: C.white });
  note(s, 'Le modèle est un participant externe, pas une source d’autorité. Les données métier ne quittent pas le backend.');
}

// 5 — Frontend
{
  const s = pptx.addSlide(); title(s, 'Architecture frontend', 'État local explicite, orchestration asynchrone avec RxJS');
  const rows = [
    ['Routes et shell', 'Lazy loading par feature, layout partagé, guard ADMIN'],
    ['Pages métier', 'Sources, configuration, export, assistant, administration'],
    ['Composants ciblés', 'Sélection de colonnes, filtres, tris, preview'],
    ['Services HTTP', 'Contrats typés, withCredentials, polling génération/export'],
    ['État', 'Signals et computed pour l’UI, sessionStorage pour le brouillon'],
  ];
  rows.forEach((r, i) => { const y = 1.48 + i * 0.9; circle(s, 0.85, y + 0.08, 0.42, i < 2 ? C.teal : C.navy2, String(i + 1)); tx(s, r[0], 1.5, y, 2.3, 0.34, { fontSize: 17, bold: true, color: C.navy }); tx(s, r[1], 3.85, y, 7.9, 0.42, { fontSize: 15, color: C.muted }); if (i < rows.length - 1) line(s, 1.06, y + 0.5, 0, 0.42, C.line, 1.5); });
  rect(s, 8.75, 5.9, 3.4, 0.65, C.pale, 0.12, C.pale);
  tx(s, 'RxJS : HTTP, debounce, cancellation, retry', 8.95, 6.08, 3.0, 0.28, { fontSize: 12.5, bold: true, color: C.navy, align: 'center' });
  note(s, 'Sources : app.routes.ts, rapports.routes.ts, ConfigurationComponent, ExportComponent, ReportDraftStorageService et services HTTP associés.');
}

// 6 — Backend
{
  const s = pptx.addSlide(); title(s, 'Architecture backend', 'Une définition résolue alimente preview, génération et assistant');
  const xs = [0.65, 3.2, 5.75, 8.3, 10.85];
  const items = [
    ['Controllers', 'HTTP, principal, statuts'],
    ['Services', 'orchestration et transactions'],
    ['Resolver', 'catalogue, types, joins'],
    ['SQL / workers', 'JDBC, snapshots, exports'],
    ['Storage', 'PostgreSQL + filesystem'],
  ];
  items.forEach((it, i) => { node(s, xs[i], 2.15, 1.85, 1.55, it[0], it[1], i === 2 ? C.pale : C.white, i === 2 ? C.teal : C.line); if (i < items.length - 1) line(s, xs[i] + 1.85, 2.92, 0.7, 0, C.navy2, 1.8, 'triangle'); });
  tx(s, 'Trois chemins réutilisent les mêmes invariants', 0.78, 4.65, 4.8, 0.36, { fontFace: 'Cambria', fontSize: 22, bold: true, color: C.navy });
  const paths = [['Preview', 'synchrone, limite de lignes'], ['Génération', 'snapshot complet, job asynchrone'], ['Assistant', 'plan LLM puis validation identique']];
  paths.forEach((p, i) => { const x = 0.8 + i * 4.0; circle(s, x, 5.35, 0.44, i === 2 ? C.teal : C.navy2, ''); tx(s, p[0], x + 0.65, 5.27, 2.1, 0.3, { fontSize: 16, bold: true }); tx(s, p[1], x + 0.65, 5.65, 2.85, 0.45, { fontSize: 12.5, color: C.muted, valign: 'top' }); });
  note(s, 'Le ReportDefinitionResolver constitue le point de convergence. La génération et les exports sont dispatchés après commit afin d’éviter qu’un worker lise un job non validé.');
}

// 7 — Data model
{
  const s = pptx.addSlide(); title(s, 'Modèle de données et métadonnées', 'Le catalogue d’exposition sépare le langage métier des noms SQL');
  node(s, 0.7, 1.55, 2.25, 1.15, 'users', 'owner des générations', C.white, C.line);
  node(s, 0.7, 3.35, 2.25, 1.15, 'roles', 'USER / ADMIN', C.white, C.line);
  node(s, 4.0, 1.55, 2.45, 1.15, 'report_generation', 'definition_json · status\nprogress · expiration', C.bluePale, C.navy2);
  node(s, 4.0, 3.35, 2.45, 1.15, 'report_export', 'format · status\nfile_location', C.bluePale, C.navy2);
  node(s, 7.55, 1.55, 2.25, 1.15, 'datasets', 'display_name · source_name\nexposition', C.pale, C.teal);
  node(s, 7.55, 3.35, 2.25, 1.15, 'dataset_fields', 'type · position\nvisible · nullable', C.pale, C.teal);
  node(s, 10.65, 2.45, 2.0, 1.15, 'Tables RH', 'employés, contrats,\npointages, absences…', C.white, C.line);
  line(s, 2.95, 2.12, 1.05, 0, C.navy2, 1.8, 'triangle');
  line(s, 5.22, 2.7, 0, 0.65, C.navy2, 1.8, 'triangle');
  line(s, 1.82, 2.7, 0, 0.65, C.line, 1.5);
  line(s, 8.67, 2.7, 0, 0.65, C.teal, 1.8, 'triangle');
  line(s, 9.8, 2.12, 0.85, 0.6, C.teal, 1.8, 'triangle');
  rect(s, 3.65, 5.35, 6.2, 0.8, C.white, 0.12, C.line);
  tx(s, 'Foreign keys PostgreSQL, relations autorisées et chemins de jointure', 3.95, 5.56, 5.6, 0.35, { fontSize: 16, bold: true, align: 'center', color: C.navy });
  note(s, 'Le diagramme se limite aux entités utiles au reporting. Les tables métier sont nombreuses; leurs foreign keys alimentent la découverte des relations via information_schema.');
}

// 8 — User workflow
{
  const s = pptx.addSlide(); title(s, 'Parcours utilisateur principal', 'Chaque étape réduit l’ambiguïté avant l’exécution complète');
  const labels = [
    ['1', 'Sources', 'Dataset principal\net relations visibles'],
    ['2', 'Configuration', 'Colonnes, filtres\net tris'],
    ['3', 'Preview', 'Échantillon limité\net erreurs explicites'],
    ['4', 'Génération', 'Job owner-scoped\net suivi de progression'],
    ['5', 'Export', 'PDF / XLSX\npuis téléchargement'],
  ];
  line(s, 1.35, 3.1, 10.5, 0, C.line, 3);
  labels.forEach((v, i) => { const x = 0.75 + i * 2.55; circle(s, x + 0.62, 2.62, 0.95, i === 4 ? C.green : C.teal, v[0], C.white, 20); tx(s, v[1], x, 3.82, 2.2, 0.34, { fontSize: 17, bold: true, align: 'center', color: C.navy }); tx(s, v[2], x, 4.28, 2.2, 0.8, { fontSize: 12.5, align: 'center', color: C.muted, valign: 'top' }); });
  rect(s, 2.4, 5.75, 8.55, 0.62, C.pale, 0.12, C.pale);
  tx(s, 'Le brouillon local restaure la configuration, mais ne crée pas de template partagé', 2.65, 5.91, 8.05, 0.3, { fontSize: 14, align: 'center', color: C.navy });
  note(s, 'Flux réel : /rapports, /rapports/configuration/:datasetId, puis /rapports/export/:generationId.');
}

// 9 — Generation/export pipeline
{
  const s = pptx.addSlide(); title(s, 'Pipeline de génération et d’export', 'La transaction précède toujours le travail asynchrone');
  const left = [
    ['1', 'Resolve', 'IDs, types, opérateurs, joins'],
    ['2', 'Persist PENDING', 'définition JSON + idempotency key'],
    ['3', 'Dispatch après commit', 'TaskExecutor dans la JVM'],
    ['4', 'Snapshot', 'lecture JDBC par lots'],
  ];
  left.forEach((v, i) => { const y = 1.45 + i * 1.18; circle(s, 0.8, y, 0.55, C.navy2, v[0]); tx(s, v[1], 1.62, y - 0.02, 2.25, 0.3, { fontSize: 17, bold: true }); tx(s, v[2], 1.62, y + 0.34, 3.6, 0.33, { fontSize: 12.5, color: C.muted }); if (i < 3) line(s, 1.08, y + 0.57, 0, 0.61, C.line, 1.5); });
  line(s, 5.25, 3.15, 1.0, 0, C.teal, 3, 'triangle');
  node(s, 6.55, 1.65, 2.2, 1.25, 'PDF', 'JasperReports\ntable dynamique', C.white, C.red);
  node(s, 6.55, 3.65, 2.2, 1.25, 'XLSX', 'Apache POI\nstreaming', C.white, C.green);
  node(s, 9.55, 2.65, 2.45, 1.25, 'Téléchargement', 'Polling état\ncontrôle owner\nfichier binaire', C.pale, C.teal);
  line(s, 8.75, 2.25, 0.8, 0.8, C.navy2, 1.8, 'triangle');
  line(s, 8.75, 4.25, 0.8, -0.8, C.navy2, 1.8, 'triangle');
  tx(s, 'PENDING, RUNNING, READY ou FAILED', 6.52, 5.75, 5.5, 0.36, { fontSize: 15, bold: true, color: C.navy, align: 'center' });
  note(s, 'La génération produit un snapshot complet. Un export ne démarre que si la génération est READY. Les métadonnées restent en base, les fichiers expirent et le cleanup les supprime.');
}

// 10 — LLM
{
  const s = pptx.addSlide(); title(s, 'Assistant LLM sous contrôle', 'XKiro propose une définition. Le backend décide si elle est exécutable.');
  const items = [
    ['Catalogue autorisé', 'IDs métier, labels, types, opérateurs et relations'],
    ['XKiro', 'structured output ou question de clarification'],
    ['Validation backend', 'resolver complet, identique au parcours manuel'],
    ['Pipeline existant', 'création du job puis export standard'],
  ];
  items.forEach((v, i) => { const x = 0.6 + i * 3.17; node(s, x, 2.2, 2.45, 1.55, v[0], v[1], i === 1 ? C.pale : C.white, i === 1 ? C.teal : C.line); if (i < 3) line(s, x + 2.45, 2.98, 0.72, 0, i === 1 ? C.teal : C.navy2, 2, 'triangle'); });
  rect(s, 1.15, 4.75, 3.25, 1.15, C.bluePale, 0.12, C.bluePale);
  tx(s, 'Jamais envoyé au modèle', 1.45, 4.95, 2.65, 0.3, { fontSize: 16, bold: true, align: 'center', color: C.navy });
  tx(s, 'lignes RH, noms SQL, requête SQL', 1.45, 5.38, 2.65, 0.26, { fontSize: 12.5, align: 'center', color: C.muted });
  rect(s, 4.95, 4.75, 3.25, 1.15, C.pale, 0.12, C.pale);
  tx(s, 'Correction limitée', 5.25, 4.95, 2.65, 0.3, { fontSize: 16, bold: true, align: 'center', color: C.navy });
  tx(s, 'un second appel, puis échec 422', 5.25, 5.38, 2.65, 0.26, { fontSize: 12.5, align: 'center', color: C.muted });
  rect(s, 8.75, 4.75, 3.25, 1.15, 'FFF4E5', 0.12, 'FFF4E5');
  tx(s, 'Clarification', 9.05, 4.95, 2.65, 0.3, { fontSize: 16, bold: true, align: 'center', color: C.navy });
  tx(s, 'aucun job tant que la demande reste ambiguë', 9.05, 5.3, 2.65, 0.42, { fontSize: 12.5, align: 'center', color: C.muted });
  note(s, 'XKiro est configuré comme endpoint compatible OpenAI. Le nom du modèle courant est qwen/qwen3-max:free. READY dans la réponse bot signifie que le plan est validé et que le job a été créé, pas que le fichier est déjà disponible.');
}

// 11 — Security
{
  const s = pptx.addSlide(); title(s, 'Validation, sécurité et autorisation', 'Plusieurs contrôles indépendants protègent données et fichiers');
  const controls = [
    ['Authentification', 'JWT en cookies\n401 pour les APIs métier'],
    ['Autorisation', 'ADMIN pour exposition\ndes datasets et utilisateurs'],
    ['Ownership', 'générations et exports\nfiltrés par owner_id'],
    ['Intégrité', 'idempotency key unique\ncapacité par utilisateur'],
    ['Définition', 'IDs visibles, types, valeurs,\ntris et chemins de jointure'],
  ];
  controls.forEach((v, i) => { const x = 0.55 + i * 2.55; circle(s, x + 0.72, 1.7, 0.7, i === 4 ? C.teal : C.navy2, String(i + 1)); tx(s, v[0], x, 2.65, 2.15, 0.34, { fontSize: 16, bold: true, align: 'center' }); tx(s, v[1], x, 3.12, 2.15, 0.82, { fontSize: 12.5, align: 'center', color: C.muted, valign: 'top' }); });
  rect(s, 1.2, 4.85, 10.95, 1.1, 'FFF0F0', 0.12, 'F5C2C0');
  circle(s, 1.5, 5.15, 0.5, C.red, '!');
  tx(s, 'Risque actuel', 2.25, 5.0, 1.55, 0.3, { fontSize: 16, bold: true, color: C.red });
  tx(s, 'La clé XKiro est présente en clair dans application.yaml. Révocation et variable d’environnement requises avant déploiement.', 3.85, 4.96, 7.85, 0.54, { fontSize: 14, color: C.ink });
  note(s, 'Le backend protège datasets, reports, générations, exports et bot par authenticated(). Les endpoints admin utilisent le rôle ADMIN. La désactivation CSRF constitue un point à reconsidérer avec une authentification par cookies.');
}

// 12 — Decisions/challenges
{
  const s = pptx.addSlide(); title(s, 'Décisions techniques et difficultés résolues', 'Des choix simples concentrent les règles dans les composants qui les connaissent');
  const rows = [
    ['SQL côté serveur', 'Le LLM et le navigateur manipulent des IDs métier, jamais du SQL libre.', 'Injection et schéma incontrôlé'],
    ['Resolver partagé', 'Preview, génération et assistant passent par les mêmes invariants.', 'Divergence entre parcours'],
    ['Foreign keys comme graphe', 'Le backend cherche un chemin de jointure entre datasets visibles.', 'Jointures codées en dur'],
    ['Snapshot avant export', 'PDF et XLSX partent du même résultat stable.', 'Résultats différents entre formats'],
    ['Jobs in-process', 'TaskExecutor, limites par utilisateur et dispatch après commit.', 'Complexité broker prématurée'],
  ];
  tx(s, 'Décision', 0.8, 1.42, 2.1, 0.28, { fontSize: 13, bold: true, color: C.muted });
  tx(s, 'Implémentation', 3.1, 1.42, 6.2, 0.28, { fontSize: 13, bold: true, color: C.muted });
  tx(s, 'Risque réduit', 10.15, 1.42, 2.15, 0.28, { fontSize: 13, bold: true, color: C.muted });
  rows.forEach((r, i) => { const y = 1.82 + i * 0.92; if (i % 2 === 0) rect(s, 0.65, y - 0.08, 12.0, 0.78, C.white, 0.06, C.white); tx(s, r[0], 0.82, y, 2.0, 0.36, { fontSize: 15, bold: true, color: C.navy }); tx(s, r[1], 3.1, y, 6.3, 0.48, { fontSize: 13.5, color: C.ink }); tx(s, r[2], 10.15, y, 2.15, 0.48, { fontSize: 13, color: C.teal, bold: true }); });
  note(s, 'Le choix d’un worker in-process convient au périmètre actuel mais limite la montée en charge horizontale. Les décisions présentées correspondent aux classes ReportDefinitionResolver, ReportSqlBuilder, ReportGenerationService et aux workers.');
}

// 13 — Demo, limitations, future
{
  const s = pptx.addSlide(); s.background = { color: C.darkBg };
  tx(s, 'Démonstration et prochaine étape', 0.7, 0.52, 11.8, 0.58, { fontFace: 'Cambria', fontSize: 32, bold: true, color: C.white });
  tx(s, 'Démo en 4 minutes', 0.78, 1.52, 3.0, 0.36, { fontSize: 19, bold: true, color: C.mint });
  const demo = ['Connexion et sélection des sources', 'Configuration + preview', 'Génération et export XLSX/PDF', 'Demande équivalente via l’assistant'];
  demo.forEach((d, i) => { circle(s, 0.82, 2.1 + i * 0.77, 0.4, C.teal, String(i + 1), C.white, 12); tx(s, d, 1.5, 2.05 + i * 0.77, 4.25, 0.42, { fontSize: 15, color: C.white }); });
  tx(s, 'Limites actuelles', 6.5, 1.52, 2.6, 0.36, { fontSize: 19, bold: true, color: 'F6C177' });
  const limits = ['Pas de templates persistants', 'Filesystem local et jobs dans la JVM', 'Pas de refresh Angular automatique', 'Pas d’historique conversationnel'];
  limits.forEach((d, i) => { circle(s, 6.55, 2.1 + i * 0.77, 0.4, C.amber, ''); tx(s, d, 7.22, 2.05 + i * 0.77, 4.9, 0.42, { fontSize: 15, color: C.white }); });
  rect(s, 1.55, 5.65, 10.15, 0.75, C.teal, 0.12, C.teal);
  tx(s, 'Priorité : externaliser les secrets, puis découpler les workers lorsque la charge le justifie', 1.9, 5.84, 9.45, 0.34, { fontSize: 17, bold: true, color: C.white, align: 'center' });
  tx(s, 'Évolutions possibles : templates partagés, stockage objet, queue externe, refresh centralisé', 1.15, 6.72, 11.05, 0.3, { fontSize: 12.5, color: 'B9CAD8', align: 'center' });
  note(s, 'Terminer par une démonstration concrète. Les améliorations proposées restent conditionnelles : externaliser le secret est immédiat; le stockage objet et la queue externe deviennent utiles avec un déploiement multi-instance ou une charge mesurée.');
}

fs.mkdirSync(path.dirname(output), { recursive: true });
async function writeDeck() {
  await pptx.writeFile({ fileName: output });
  const zip = await JSZip.loadAsync(fs.readFileSync(output));
  const contentTypes = await zip.file('[Content_Types].xml').async('string');
  const cleanedContentTypes = contentTypes.replace(
    /<Override PartName="\/ppt\/slideMasters\/slideMaster(?:[2-9]|1[0-3])\.xml" ContentType="application\/vnd\.openxmlformats-officedocument\.presentationml\.slideMaster\+xml"\/>/g,
    '',
  );
  zip.file('[Content_Types].xml', cleanedContentTypes);
  fs.writeFileSync(output, await zip.generateAsync({ type: 'nodebuffer' }));
  console.log(output);
}

writeDeck().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
