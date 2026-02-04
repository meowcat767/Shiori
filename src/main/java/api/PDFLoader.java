package api;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFLoader {

    public static List<BufferedImage> loadPDF(File file) throws IOException {
        List<BufferedImage> images = new ArrayList<>();

        // Use Loader.loadPDF(...) for PDFBox 3.x
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int totalPages = doc.getNumberOfPages();
            for (int i = 0; i < totalPages; i++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 150);
                images.add(pageImage);
            }
        }

        return images;
    }
}