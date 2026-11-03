package sv.labmedic.service;

import com.deepoove.poi.XWPFTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import sv.labmedic.entity.Plantilla;
import sv.labmedic.entity.TipoExamen;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@ApplicationScoped
public class PlantillaService {

    @ConfigProperty(name = "labmedic.storage.root")
    String storageRoot;

    @Inject
    PdfConverter pdfConverter;

    /** Registra una plantilla Word nueva (o una nueva version de un examen existente). */
    @Transactional
    public Plantilla registrar(String codigo, String nombre, String categoria, Path temporal) throws IOException {
        byte[] bytes = Files.readAllBytes(temporal);

        List<String> campos;
        try {
            campos = DocxCampos.extraer(bytes);
        } catch (Exception e) {
            throw new BadRequestException("El archivo no es un .docx valido");
        }
        if (campos.isEmpty()) {
            throw new BadRequestException("El Word no contiene marcadores con el formato {{campo}}");
        }

        TipoExamen tipo = TipoExamen.porCodigo(codigo).orElseGet(() -> {
            TipoExamen t = new TipoExamen();
            t.codigo = codigo;
            t.nombre = nombre;
            t.categoria = categoria;
            t.persist();
            return t;
        });

        int version = (int) Plantilla.count("tipoExamen.id", tipo.id) + 1;

        // Solo puede haber una version activa por examen
        Plantilla.update("activa = false where tipoExamen.id = ?1 and activa = true", tipo.id);

        String nombreArchivo = codigo.replaceAll("[^A-Za-z0-9_-]", "_") + "_v" + version + ".docx";
        Path carpeta = Path.of(storageRoot, "plantillas");
        Files.createDirectories(carpeta);
        Files.write(carpeta.resolve(nombreArchivo), bytes);

        Map<String, Object> definicion = new HashMap<>();
        definicion.put("campos", campos);

        Plantilla p = new Plantilla();
        p.tipoExamen = tipo;
        p.version = version;
        p.definicion = definicion;
        p.archivoBase = "plantillas/" + nombreArchivo;
        p.hashSha256 = sha256(bytes);
        p.activa = true;
        p.persist();
        return p;
    }

    public List<Plantilla> listarActivas() {
        return Plantilla.list("activa = true order by tipoExamen.codigo");
    }

    public Plantilla obtener(Long id) {
        Plantilla p = Plantilla.findById(id);
        if (p == null) {
            throw new NotFoundException("Plantilla " + id + " no existe");
        }
        return p;
    }

    /** Rellena el Word con los datos, lo convierte a PDF, lo guarda y devuelve los bytes. */
    public byte[] generarPdf(Long plantillaId, Map<String, Object> datos) throws IOException {
        Plantilla p = obtener(plantillaId);
        Path docxBase = Path.of(storageRoot).resolve(p.archivoBase);
        if (!Files.exists(docxBase)) {
            throw new IllegalStateException("No se encuentra el archivo de plantilla: " + docxBase);
        }

        // poi-tl trabaja mejor con texto: todo a String, nulos a vacio
        Map<String, Object> valores = new HashMap<>();
        for (String campo : p.campos()) {
            Object v = datos.get(campo);
            valores.put(campo, v == null ? "" : String.valueOf(v));
        }

        Path tmp = Files.createTempDirectory("labmedic-");
        try {
            Path docxRelleno = tmp.resolve("resultado.docx");
            try (XWPFTemplate plantilla = XWPFTemplate.compile(docxBase.toFile());
                 OutputStream out = Files.newOutputStream(docxRelleno)) {
                plantilla.render(valores).write(out);
            }

            Path pdf = pdfConverter.convertir(docxRelleno, tmp);
            byte[] bytesPdf = Files.readAllBytes(pdf);

            // Copia en la carpeta de documentos (carpeta compartida)
            String marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path destino = Path.of(storageRoot, "documentos");
            Files.createDirectories(destino);
            Files.write(destino.resolve(p.tipoExamen.codigo + "_" + marca + ".pdf"), bytesPdf);

            return bytesPdf;
        } finally {
            try (Stream<Path> restos = Files.walk(tmp)) {
                restos.sorted((a, b) -> b.compareTo(a)).forEach(f -> f.toFile().delete());
            }
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
