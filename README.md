# Labmedic API (prototipo)

Backend Quarkus: sube plantillas Word con marcadores `{{campo}}`, detecta los campos
y genera PDFs rellenos usando LibreOffice.

## Requisitos
- JDK 21
- Maven 3.9+ (verifica con `mvn -v`)
- PostgreSQL con el esquema `lab` ya creado (labmedic_schema.sql)
- LibreOffice instalado (para convertir a PDF)
- VS Code con las extensiones recomendadas (te las sugiere al abrir la carpeta)

## Configurar
Edita `src/main/resources/application.properties`:
1. `quarkus.datasource.password`
2. `labmedic.libreoffice.path` (en Windows: `C:/Program Files/LibreOffice/program/soffice.exe`)
3. `labmedic.storage.root` (carpeta de plantillas y PDFs; luego la carpeta compartida)

## Levantar
    mvn quarkus:dev

Queda en http://localhost:8080 con recarga en caliente.

## Probar
1. Crea en Word un `hemograma.docx` con texto como: `Paciente: {{nombre_paciente}}`,
   `Fecha: {{fecha_toma}}`, `Hemoglobina: {{hemoglobina}}`. Ponlo junto a `requests.http`.
2. Abre `requests.http` en VS Code y pulsa "Send Request" sobre cada bloque, en orden:
   subir plantilla -> generar PDF.
3. En `data/plantillas` queda el Word y en `data/documentos` el PDF generado.

## Notas
- Hibernate NO modifica la BD (`generation=none`); los cambios de esquema los corres tu.
- Si la conversion falla, cierra cualquier ventana abierta de LibreOffice y reintenta.
- Solo se detectan marcadores simples `{{nombre}}` (letras, numeros y guion bajo).
