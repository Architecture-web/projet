package com.skillquest.gamification.service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.skillquest.gamification.config.GamificationProperties;
import com.skillquest.gamification.domain.Certificate;

import org.springframework.stereotype.Component;

/** Génère le certificat officiel au format PDF (A4 paysage). */
@Component
public class CertificatePdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH).withZone(ZoneOffset.UTC);

    private final String verificationBaseUrl;

    public CertificatePdfGenerator(GamificationProperties props) {
        this.verificationBaseUrl = props.certificate().verificationBaseUrl();
    }

    public byte[] generate(Certificate certificate) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 60, 60, 60, 60);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.addTitle("Certificat SkillQuest - " + certificate.getCourseTitle());
            document.addAuthor("SkillQuest");
            document.open();
            drawBorder(writer, document);

            Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new java.awt.Color(0x1F, 0x4E, 0x79));
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 34);
            Font text = FontFactory.getFont(FontFactory.HELVETICA, 14);
            Font name = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, new java.awt.Color(0x1F, 0x4E, 0x79));
            Font course = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font small = FontFactory.getFont(FontFactory.COURIER, 9, java.awt.Color.DARK_GRAY);

            add(document, "SKILLQUEST", brand, 24);
            add(document, "CERTIFICAT DE RÉUSSITE", title, 28);
            add(document, "Il est certifié que", text, 12);
            add(document, certificate.getStudentFullName(), name, 12);
            add(document, "a complété avec succès le cours", text, 12);
            add(document, certificate.getCourseTitle(), course, 20);
            add(document, "Délivré le " + DATE_FORMAT.format(certificate.getIssuedAt()), text, 28);

            add(document, "N° " + certificate.getId(), small, 2);
            add(document, "Empreinte (HMAC-SHA256) : " + certificate.getCertificateHash(), small, 2);
            add(document, "Vérification : " + verificationBaseUrl + "/api/certifications/"
                    + certificate.getId() + "/verify?hash=" + certificate.getCertificateHash(), small, 0);

            document.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Unable to generate certificate PDF", ex);
        }
        return out.toByteArray();
    }

    private static void add(Document document, String content, Font font, float spacingAfter)
            throws DocumentException {
        Paragraph p = new Paragraph(content, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(spacingAfter);
        document.add(p);
    }

    private static void drawBorder(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        Rectangle page = document.getPageSize();
        canvas.setColorStroke(new java.awt.Color(0x1F, 0x4E, 0x79));
        canvas.setLineWidth(3f);
        canvas.rectangle(25, 25, page.getWidth() - 50, page.getHeight() - 50);
        canvas.setLineWidth(1f);
        canvas.rectangle(32, 32, page.getWidth() - 64, page.getHeight() - 64);
        canvas.stroke();
    }
}
