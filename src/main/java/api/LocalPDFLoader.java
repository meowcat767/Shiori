package api;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class LocalPDFLoader {

    public static void loadIntoReader(
            File pdf,
            ui.ReaderPanel reader,
            LocalPDFStore store
    ) {
        store.add(pdf);
        reader.clearPages();

        SwingWorker<Void, ImageIcon> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (PDDocument doc = Loader.loadPDF(pdf)) {
                    PDFRenderer renderer = new PDFRenderer(doc);
                    for (int i = 0; i < doc.getNumberOfPages(); i++) {
                        BufferedImage img =
                                renderer.renderImageWithDPI(i, 150);
                        publish(new ImageIcon(img));
                    }
                }
                return null;
            }

            @Override
            protected void process(List<ImageIcon> pages) {
                for (ImageIcon icon : pages) {
                    reader.addPage(icon);
                }
            }

            @Override
            protected void done() {
                reader.onLoadComplete(pdf.getName());
            }
        };

        worker.execute();
    }
}