package com.cv_jd_matching.HR.parser;

import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.pdfbox.pdmodel.PDDocument;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FileTextExtractor {

    public static String extractTextFromFile(InputStream inputStream, String fileName) throws IOException {
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".docx")) {
            return extractTextFromDocx(inputStream);
        } else if (lowerName.endsWith(".pdf")) {
            return extractTextFromPdf(inputStream);
        } else if (lowerName.endsWith(".txt")) {
            return extractTextFromTxt(inputStream);
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
            return extractTextFromImage(inputStream);
        } else {
            throw new IOException("Unsupported file type for CV: " + fileName);
        }
    }

    public static String extractTextFromDocx(InputStream inputStream) throws IOException {
        StringBuilder text = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                text.append(para.getText()).append("\n");
            }
        }
        return text.toString();
    }

    public static String extractTextFromPdf(InputStream pdfInputStream) throws IOException {
        try (PDDocument document = PDDocument.load(pdfInputStream)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            StringBuilder text = new StringBuilder();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(i, 300);
                text.append(extractTextFromImage(bufferedImageToInputStream(pageImage))).append("\n");
            }

            return text.toString();
        }
    }

    private static InputStream bufferedImageToInputStream(BufferedImage image) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    public static String extractTextFromTxt(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String extractTextFromImage(InputStream inputStream) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            throw new IOException("Could not read image from file.");
        }

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("src/main/resources/tessdata");
        tesseract.setLanguage("ron+eng");

        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            throw new IOException("OCR failed: " + e.getMessage(), e);
        }
    }

}
