package sv.labmedic.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import sv.labmedic.dto.PlantillaDto;
import sv.labmedic.service.PlantillaService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Path("/api/plantillas")
public class PlantillaResource {

    @Inject
    PlantillaService service;

    @GET
    @Path("/test") 
    @Produces(MediaType.APPLICATION_JSON)
    public Response test(){
        return Response.ok("Quarkus levantado con exito").build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PlantillaDto> listar() {
        return service.listarActivas().stream().map(PlantillaDto::de).toList();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public PlantillaDto obtener(@PathParam("id") Long id) {
        return PlantillaDto.de(service.obtener(id));
    }

    /** Sube un .docx, detecta los {{campos}} y lo registra como nueva version. */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public PlantillaDto subir(@RestForm String codigo,
                              @RestForm String nombre,
                              @RestForm String categoria,
                              @RestForm("archivo") FileUpload archivo) throws IOException {
        if (codigo == null || codigo.isBlank() || nombre == null || nombre.isBlank()) {
            throw new BadRequestException("codigo y nombre son obligatorios");
        }
        if (archivo == null || archivo.fileName() == null
                || !archivo.fileName().toLowerCase().endsWith(".docx")) {
            throw new BadRequestException("Debes subir un archivo .docx");
        }
        return PlantillaDto.de(service.registrar(
                codigo.trim().toUpperCase(), nombre.trim(), categoria, archivo.uploadedFile()));
    }

    /** Recibe {"campo": "valor", ...} y devuelve el PDF generado. */
    @POST
    @Path("/{id}/generar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/pdf")
    public Response generar(@PathParam("id") Long id, Map<String, Object> datos) throws IOException {
        byte[] pdf = service.generarPdf(id, datos == null ? Map.of() : datos);
        return Response.ok(pdf)
                .header("Content-Disposition", "inline; filename=\"resultado.pdf\"")
                .build();
    }
}
