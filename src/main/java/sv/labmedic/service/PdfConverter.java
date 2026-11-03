package sv.labmedic.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/** Convierte DOCX a PDF usando LibreOffice en modo headless. */
@ApplicationScoped
public class PdfConverter {

    @ConfigProperty(name = "labmedic.libreoffice.path", defaultValue = "soffice")
    String soffice;

    // LibreOffice no tolera bien conversiones simultaneas con el mismo perfil: una a la vez
    private final ReentrantLock lock = new ReentrantLock();

    public Path convertir(Path docx, Path carpetaSalida) {
        lock.lock();
        try {
            Path log = carpetaSalida.resolve("soffice.log");
            Process proceso = new ProcessBuilder(
                    soffice, "--headless", "--convert-to", "pdf",
                    "--outdir", carpetaSalida.toString(), docx.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();

            if (!proceso.waitFor(90, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                throw new IllegalStateException("LibreOffice tardo mas de 90 segundos");
            }

            String nombrePdf = docx.getFileName().toString().replaceAll("(?i)\\.docx$", ".pdf");
            Path pdf = carpetaSalida.resolve(nombrePdf);
            if (!Files.exists(pdf)) {
                String detalle = Files.exists(log) ? Files.readString(log) : "(sin log)";
                throw new IllegalStateException("No se genero el PDF. Salida de LibreOffice: " + detalle);
            }
            return pdf;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo ejecutar LibreOffice (" + soffice + "). Revisa labmedic.libreoffice.path", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Conversion interrumpida", e);
        } finally {
            lock.unlock();
        }
    }
}
