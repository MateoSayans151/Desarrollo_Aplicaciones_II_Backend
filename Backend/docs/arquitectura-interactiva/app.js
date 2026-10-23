"use strict";

const architectures = {
  current: [
    ["UI", "Frontend React", "API real · sin mocks", "primary"],
    ["API", "Spring Boot", "JSESSIONID · problem+json", "primary"],
    ["BL", "Servicios de dominio", "Reglas y transacciones", ""],
    ["JPA", "Persistencia", "Spring Data / Hibernate", ""],
    ["DB", "PostgreSQL", "Supabase · readiness real", "external"]
  ],
  target: [
    ["UI", "Portales", "Alumno · Docente · Admin", "external pending"],
    ["GW", "Gateway CORE", "JWT + prefijo academic", "external pending"],
    ["API", "Academic API", "/api/v1/** + problem+json", "primary pending"],
    ["BL", "Dominio académico", "Idempotencia + eventos", "pending"],
    ["EXT", "CORE / Backoffice", "/internal/v1 + service token", "external pending"]
  ]
};

const flows = [
  {
    icon: "01", name: "Diseñar un plan", title: "De la carrera al plan de estudios",
    description: "Estructura curricular versionada y publicable para los portales.", status: "current", statusText: "Implementado",
    steps: [
      ["Crear la carrera", "El código respeta el estándar institucional y no puede repetirse."],
      ["Crear el plan", "Se vincula a una carrera existente y conserva su propia versión y estado."],
      ["Agregar asignaturas", "Cada materia queda ubicada por año dentro del plan y actualiza la duración calculada de la carrera."],
      ["Definir correlatividades", "La asignatura previa pertenece al mismo plan y no se permiten autorreferencias."],
      ["Publicar o exportar", "El plan completo puede consultarse y descargarse en PDF."]
    ]
  },
  {
    icon: "02", name: "Planificar un curso", title: "Oferta académica de un período",
    description: "Combina materia, calendario, espacio y equipo docente.", status: "current", statusText: "Implementado",
    steps: [
      ["Definir período", "Las fechas de inicio y fin delimitan la cursada y la inscripción."],
      ["Elegir sede y aula", "El aula debe estar activa, tener capacidad suficiente y una clave externa si se sincroniza."],
      ["Crear el curso", "Se valida asignatura, período, modalidad, cupo, fechas y horario."],
      ["Evitar superposiciones", "Los cursos se comparan con otros cursos del mismo período, aula, día y franja."],
      ["Asignar docentes", "Cada docente se incorpora con un rol docente explícito."]
    ]
  },
  {
    icon: "03", name: "Inscribir y calificar", title: "Del alta del alumno al resultado",
    description: "Protege cupo, correlatividades, estado de cursada y consistencia de notas.", status: "current", statusText: "Implementado",
    steps: [
      ["Abrir inscripción", "La fecha actual debe estar dentro de la ventana configurada para el período."],
      ["Validar elegibilidad", "Se controlan cupo, duplicados y correlativas aprobadas o regularizadas."],
      ["Registrar inscripción", "El vínculo alumno-curso nace con un estado académico válido."],
      ["Actualizar estado", "La cursada evoluciona entre inscripto, cursando, baja, regular, libre o resultado final."],
      ["Publicar resultado", "La nota 0–10 y el estado final se guardan juntos; el evento sale después del commit."]
    ]
  },
  {
    icon: "04", name: "Reservar un aula", title: "Asignación puntual de espacio",
    description: "Agenda una actividad académica sin crear una cursada completa.", status: "current", statusText: "Implementado",
    steps: [
      ["Seleccionar asignatura", "La actividad siempre referencia una asignatura existente."],
      ["Seleccionar espacio", "Sólo se usan ubicaciones activas de tipo AULA."],
      ["Definir fecha y hora", "La hora de fin debe ser posterior a la de inicio."],
      ["Validar capacidad", "La cantidad estimada de asistentes no puede superar el aula."],
      ["Consultar agenda", "El calendario mensual consulta por aula y rango, marca días ocupados y abre el detalle diario."]
    ]
  },
  {
    icon: "05", name: "Entrar desde CORE", title: "Autenticación e integración objetivo",
    description: "Flujo acordado con CORE; todavía no representa el runtime local.", status: "pending", statusText: "Pendiente",
    steps: [
      ["Autenticar en CORE", "El portal obtiene un access token emitido por el proveedor central."],
      ["Cruzar el gateway", "CORE enruta /api/v1/academic/* y agrega identidad y correlación."],
      ["Verificar otra vez", "Academic valida firma RS256, issuer, audience y expiración con JWKS."],
      ["Autorizar la actividad", "Se evalúa el permiso académico exacto que CORE todavía debe confirmar."],
      ["Responder trazablemente", "Los errores de aplicación ya usan problem+json; falta propagar X-Correlation-Id en logs, llamadas y eventos."]
    ]
  }
];

const endpoints = [
  ["GET", "/health/live", "Sistema", "Comprobar proceso vivo", "public", "Sin payload.", "No consulta dependencias externas."],
  ["GET", "/health/ready", "Sistema", "Comprobar servicio listo", "public", "Sin payload.", "Comprueba PostgreSQL; devuelve 503 DOWN cuando no está disponible."],
  ["POST", "/api/auth/login", "Autenticación", "Iniciar sesión local", "public", "usuario y contraseña.", "Crea sesión JSESSIONID; este flujo será reemplazado por CORE."],
  ["GET", "/api/auth/me", "Autenticación", "Consultar sesión", "protected", "Cookie de sesión válida.", "Devuelve identidad y autoridades locales."],
  ["POST", "/api/auth/logout", "Autenticación", "Cerrar sesión", "protected", "Cookie de sesión válida.", "Invalida la sesión local."],
  ["GET", "/swagger-ui.html", "Documentación", "Abrir Swagger UI", "docs", "Sin payload.", "Disponible sin autenticación en el perfil actual."],
  ["GET", "/v3/api-docs", "Documentación", "Descargar OpenAPI", "docs", "Sin payload.", "Describe la API; hoy anuncia Bearer aunque el runtime usa sesión."],

  ["POST", "/api/academica/carreras", "Estructura", "Crear carrera", "protected", "Código, nombre, facultad y título; no recibe duración.", "La duración se deriva de las materias: cinco por cuatrimestre, diez por año."],
  ["GET", "/api/academica/carreras", "Estructura", "Listar carreras", "protected", "Sin payload.", "Devuelve catálogo administrativo."],
  ["PUT", "/api/academica/carreras/{id}", "Estructura", "Modificar carrera", "protected", "Identificador y datos editables.", "La entidad debe existir; conserva unicidad del código."],
  ["DELETE", "/api/academica/carreras/{id}", "Estructura", "Desactivar carrera", "protected", "Identificador de carrera.", "Realiza una baja lógica y conserva sus relaciones."],
  ["POST", "/api/academica/carreras/{carreraId}/planes", "Estructura", "Crear plan", "protected", "Versión, nombre, estado y vigencia.", "La carrera debe existir."],
  ["GET", "/api/academica/carreras/{carreraId}/planes", "Estructura", "Listar planes de carrera", "protected", "Identificador de carrera.", "Acota el catálogo a la carrera."],
  ["GET", "/api/academica/planes/{planId}/pdf", "Estructura", "Exportar plan en PDF", "protected", "Identificador de plan.", "Incluye estructura y asignaturas del plan."],
  ["POST", "/api/academica/planes/{planId}/asignaturas", "Estructura", "Agregar asignatura", "protected", "Código, nombre, año, créditos y carga horaria.", "Código válido y único; el plan debe existir y la carrera recalcula su duración."],
  ["GET", "/api/academica/planes/{planId}/asignaturas", "Estructura", "Listar asignaturas", "protected", "Identificador de plan.", "Devuelve el orden curricular del plan."],
  ["POST", "/api/academica/asignaturas/{asignaturaId}/correlatividades", "Estructura", "Agregar correlatividad", "protected", "Asignatura previa y condición.", "Mismo plan, sin autorreferencia ni duplicados."],
  ["GET", "/api/academica/asignaturas/{asignaturaId}/correlatividades", "Estructura", "Listar correlatividades", "protected", "Identificador de asignatura.", "Expone requisitos para inscripción."],

  ["POST", "/api/planificacion/sedes", "Espacios", "Crear sede", "protected", "Nombre y dirección.", "Las referencias externas se completarán mediante la sincronización futura con Backoffice."],
  ["GET", "/api/planificacion/sedes", "Espacios", "Listar sedes", "protected", "Sin payload.", "Catálogo para formularios administrativos."],
  ["POST", "/api/planificacion/sedes/{sedeId}/aulas", "Espacios", "Crear aula", "protected", "Código, nombre opcional y capacidad.", "La sede debe existir; la ubicación local se crea con tipo AULA."],
  ["GET", "/api/planificacion/sedes/{sedeId}/aulas", "Espacios", "Listar aulas de sede", "protected", "Identificador de sede.", "Devuelve espacios asociados a la sede."],
  ["POST", "/api/planificacion/asignaciones", "Espacios", "Reservar aula", "protected", "Asignatura, aula, fecha, horario y asistentes.", "Capacidad, horario y ausencia de solapamientos."],
  ["GET", "/api/planificacion/asignaciones", "Espacios", "Listar asignaciones", "protected", "Sin payload.", "Devuelve reservas reales ordenadas por fecha y hora."],
  ["GET", "/api/planificacion/aulas/{aulaId}/agenda?fecha=AAAA-MM-DD o ?desde=&hasta=", "Espacios", "Consultar agenda del aula", "protected", "Aula y una fecha puntual o un rango ISO completo.", "Valida el aula y devuelve ocupaciones ordenadas; el rango es inclusivo."],
  ["GET", "/api/planificacion/asignaturas/{asignaturaId}/agenda?desde=&hasta=", "Espacios", "Consultar agenda de la asignatura", "protected", "Asignatura y rango ISO completo.", "Devuelve las aulas y horarios usados por la materia en el rango."],
  ["POST", "/api/planificacion/periodos", "Calendario", "Crear período académico", "protected", "Año, número y fechas de cursada e inscripción.", "Los rangos son coherentes y la inscripción cierra antes o al comenzar la cursada."],
  ["GET", "/api/planificacion/periodos", "Calendario", "Listar períodos", "protected", "Sin payload.", "Catálogo para cursos e inscripciones."],
  ["POST", "/api/planificacion/turnos-examen", "Calendario", "Crear turno de examen", "protected", "Nombre, fechas del turno y fechas de inscripción.", "Los rangos son coherentes y la inscripción cierra antes del turno."],
  ["GET", "/api/planificacion/turnos-examen", "Calendario", "Listar turnos", "protected", "Sin payload.", "Expone calendario de exámenes."],

  ["POST", "/api/planificacion/cursos", "Cursos", "Crear curso", "protected", "Asignatura, período, aula, modalidad, cupo, fechas y horario.", "Valida referencias, rangos, cupo y disponibilidad del aula."],
  ["GET", "/api/planificacion/cursos", "Cursos", "Listar cursos", "protected", "periodoId opcional.", "Permite filtrar la oferta por período."],
  ["GET", "/api/planificacion/cursos/{cursoId}", "Cursos", "Consultar curso", "protected", "Identificador de curso.", "Incluye configuración de la cursada."],
  ["PUT", "/api/planificacion/cursos/{cursoId}/estado", "Cursos", "Cambiar estado", "protected", "Nuevo EstadoCurso.", "Sólo acepta estados del contrato."],
  ["PUT", "/api/planificacion/cursos/{cursoId}/horario", "Cursos", "Cambiar horario", "protected", "Días, horas y aula.", "Revalida fechas y superposiciones."],
  ["POST", "/api/planificacion/cursos/{cursoId}/docentes", "Cursos", "Asignar docente", "protected", "docenteId y RolDocente.", "Curso existente y vínculo no duplicado."],
  ["POST", "/api/planificacion/cursos/{cursoId}/inscripciones", "Alumnos", "Inscribir alumno", "protected", "alumnoId.", "Ventana abierta, cupo, no duplicado y correlativas."],
  ["PUT", "/api/planificacion/cursos/{cursoId}/inscripciones/{alumnoId}", "Alumnos", "Actualizar inscripción", "protected", "Nuevo EstadoInscripcionCurso.", "Alumno previamente inscripto en el curso."],
  ["GET", "/api/planificacion/alumnos/{alumnoId}/inscripciones?periodoId={id}", "Alumnos", "Consultar cursadas", "protected", "Alumno y período opcional.", "Devuelve historial o recorte del período."],
  ["GET", "/api/planificacion/cursos/{cursoId}/notas", "Resultados", "Listar notas", "protected", "Identificador de curso.", "Incluye resultados registrados del curso."],
  ["GET", "/api/planificacion/cursos/{cursoId}/notas/{alumnoId}", "Resultados", "Consultar nota", "protected", "Curso y alumno.", "Requiere vínculo de inscripción."],
  ["PUT", "/api/planificacion/cursos/{cursoId}/notas/{alumnoId}", "Resultados", "Publicar nota", "protected", "Nota 0–10 y estado final.", "Persiste atómicamente y emite evento después del commit."],
  ["POST", "/api/regularidad/validar", "Regularidad", "Validar condición", "protected", "alumnoId, cursoId, asistencia, promedio y mínimos.", "Exige inscripción activa, identifica curso y materia y comprueba correlativas aprobadas; no persiste el resultado."]
];

const rules = [
  ["Códigos institucionales", "Carreras, asignaturas, planes y cursos aceptan exactamente tres letras mayúsculas, un dígito opcional y un correlativo de tres dígitos.", "^[A-Z]{3}[0-9]?-[0-9]{3}$"],
  ["Identidad única", "Los códigos no se reutilizan dentro del catálogo que identifica cada entidad; las referencias deben existir antes de asociarse."],
  ["Correlatividades sanas", "La materia previa pertenece al mismo plan; no puede ser la misma asignatura ni repetirse la relación."],
  ["Calendario coherente", "Inicio no supera fin y la ventana de inscripción queda alineada con el período académico."],
  ["Aulas compatibles", "Una actividad sólo ocupa ubicaciones de tipo AULA y nunca supera su capacidad declarada."],
  ["Sin superposiciones", "Las asignaciones puntuales se comparan entre sí por fecha y hora; los cursos, entre sí por día, período y horario."],
  ["Inscripción protegida", "Se exige ventana abierta, cupo disponible, ausencia de duplicado y correlativas satisfechas."],
  ["Estados cerrados", "Carreras, cursos e inscripciones usan enums explícitos; el cliente no debe inventar strings."],
  ["Resultados atómicos", "La nota va de 0 a 10, se guarda con el estado final y el evento se publica sólo después del commit."],
  ["Duración derivada", "La carrera no recibe duración manual: cada bloque de cinco materias suma un cuatrimestre y las fracciones se redondean al cuatrimestre necesario."],
  ["Regularidad contextual", "La validación identifica alumno, curso y materia, exige inscripción activa y correlativas aprobadas para habilitar el final."],
  ["Responsabilidad de datos", "Academic conserva claves locales; UUID de personas, sites y locations pertenecen a CORE o Backoffice."]
];

const architectureCanvas = document.querySelector("#architectureCanvas");
const architectureButtons = document.querySelectorAll("[data-architecture]");
const flowTabs = document.querySelector("#flowTabs");
const flowDetail = document.querySelector("#flowDetail");
const methodFilters = document.querySelector("#methodFilters");
const endpointList = document.querySelector("#endpointList");
const endpointCount = document.querySelector("#endpointCount");
const searchInput = document.querySelector("#endpointSearch");
const accessFilter = document.querySelector("#accessFilter");

let selectedMethod = "ALL";

function renderArchitecture(key) {
  architectureCanvas.innerHTML = architectures[key].map(([icon, title, text, classes]) => `
    <article class="arch-node ${classes}">
      <span class="arch-icon">${icon}</span>
      <strong>${title}</strong>
      <small>${text}</small>
    </article>`).join("");
  architectureButtons.forEach(button => button.classList.toggle("active", button.dataset.architecture === key));
}

architectureButtons.forEach(button => button.addEventListener("click", () => renderArchitecture(button.dataset.architecture)));

function renderFlow(index) {
  const flow = flows[index];
  [...flowTabs.children].forEach((button, position) => {
    const selected = position === index;
    button.classList.toggle("active", selected);
    button.setAttribute("aria-selected", selected);
    button.tabIndex = selected ? 0 : -1;
  });
  flowDetail.innerHTML = `
    <header>
      <div><h3>${flow.title}</h3><p>${flow.description}</p></div>
      <span class="badge ${flow.status}">${flow.statusText}</span>
    </header>
    <div class="flow-steps">
      ${flow.steps.map(([title, text], position) => `
        <div class="flow-step">
          <span class="step-index">${String(position + 1).padStart(2, "0")}</span>
          <div><strong>${title}</strong><p>${text}</p></div>
        </div>`).join("")}
    </div>`;
}

flows.forEach((flow, index) => {
  const button = document.createElement("button");
  button.type = "button";
  button.role = "tab";
  button.innerHTML = `<span>${flow.icon}</span>${flow.name}`;
  button.addEventListener("click", () => renderFlow(index));
  flowTabs.append(button);
});

function accessLabel(access) {
  return { public: "Público", protected: "Administrativo", docs: "Docs" }[access];
}

function renderEndpoints() {
  const query = searchInput.value.trim().toLocaleLowerCase("es");
  const access = accessFilter.value;
  const filtered = endpoints.filter(endpoint => {
    const [method, path, domain, action, endpointAccess] = endpoint;
    const searchable = `${method} ${path} ${domain} ${action}`.toLocaleLowerCase("es");
    return (selectedMethod === "ALL" || method === selectedMethod)
      && (access === "all" || endpointAccess === access)
      && (!query || searchable.includes(query));
  });

  endpointCount.textContent = `${filtered.length} de ${endpoints.length} endpoints`;
  if (!filtered.length) {
    endpointList.innerHTML = `<div class="empty-api">No hay endpoints para esos filtros.</div>`;
    return;
  }

  endpointList.innerHTML = filtered.map(([method, path, domain, action, accessType, contract, rule], index) => `
    <article class="endpoint-row">
      <button class="endpoint-trigger" type="button" aria-expanded="false" aria-controls="endpoint-${index}">
        <span class="method ${method}">${method}</span>
        <span class="endpoint-path">${path}</span>
        <span class="endpoint-action">${action}</span>
        <span class="badge access ${accessType}">${accessLabel(accessType)}</span>
        <span class="chevron">›</span>
      </button>
      <div class="endpoint-detail" id="endpoint-${index}">
        <div><h4>Dominio y contrato</h4><p><b>${domain}.</b> ${contract}</p></div>
        <div><h4>Regla principal</h4><p>${rule}</p></div>
      </div>
    </article>`).join("");

  endpointList.querySelectorAll(".endpoint-trigger").forEach(button => button.addEventListener("click", () => {
    const row = button.closest(".endpoint-row");
    const open = row.classList.toggle("open");
    button.setAttribute("aria-expanded", open);
  }));
}

["ALL", "GET", "POST", "PUT", "DELETE"].forEach(method => {
  const button = document.createElement("button");
  button.type = "button";
  button.textContent = method;
  button.classList.toggle("active", method === selectedMethod);
  button.addEventListener("click", () => {
    selectedMethod = method;
    [...methodFilters.children].forEach(item => item.classList.toggle("active", item === button));
    renderEndpoints();
  });
  methodFilters.append(button);
});

searchInput.addEventListener("input", renderEndpoints);
accessFilter.addEventListener("change", renderEndpoints);

document.querySelector("#rulesGrid").innerHTML = rules.map(([title, text, code], index) => `
  <article class="rule-card reveal" data-index="${String(index + 1).padStart(2, "0")}">
    <h3>${title}</h3><p>${text}</p>${code ? `<code>${code}</code>` : ""}
  </article>`).join("");

const themeToggle = document.querySelector("#themeToggle");
const savedTheme = localStorage.getItem("academic-map-theme");
if (savedTheme === "dark") document.documentElement.dataset.theme = "dark";
themeToggle.addEventListener("click", () => {
  const dark = document.documentElement.dataset.theme !== "dark";
  document.documentElement.dataset.theme = dark ? "dark" : "light";
  localStorage.setItem("academic-map-theme", dark ? "dark" : "light");
});

const observer = new IntersectionObserver(entries => entries.forEach(entry => {
  if (entry.isIntersecting) {
    entry.target.classList.add("visible");
    observer.unobserve(entry.target);
  }
}), { threshold: 0.08 });
document.querySelectorAll(".reveal").forEach(element => observer.observe(element));

renderArchitecture("current");
renderFlow(0);
renderEndpoints();
