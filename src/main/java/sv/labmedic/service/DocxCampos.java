package sv.labmedic.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detecta los marcadores {{campo}} dentro de un .docx. */
public final class DocxCampos {

    // Solo marcadores simples de texto: {{nombre_paciente}}
    private static final Pattern MARCADOR =
            Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\}\\}");

    private DocxCampos() {
    }

    public static List<String> extraer(byte[] docx) throws IOException {
        Set<String> campos = new LinkedHashSet<>();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            recolectar(doc.getParagraphs(), doc.getTables(), campos);
            doc.getHeaderList().forEach(h -> recolectar(h.getParagraphs(), h.getTables(), campos));
            doc.getFooterList().forEach(f -> recolectar(f.getParagraphs(), f.getTables(), campos));
        }
        return new ArrayList<>(campos);
    }

    private static void recolectar(List<XWPFParagraph> parrafos, List<XWPFTable> tablas, Set<String> out) {
        for (XWPFParagraph p : parrafos) {
            // getText() ya une los "runs" en que Word parte el texto
            Matcher m = MARCADOR.matcher(p.getText());
            while (m.find()) {
                out.add(m.group(1));
            }
        }
        for (XWPFTable t : tablas) {
            for (XWPFTableRow fila : t.getRows()) {
                for (XWPFTableCell celda : fila.getTableCells()) {
                    recolectar(celda.getParagraphs(), celda.getTables(), out);
                }
            }
        }
    }
}
