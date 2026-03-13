package dev.ak.ai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public Mono<Void> ingest(FilePart filePart) {

        return Mono.fromCallable(() -> processFile(filePart))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Void processFile(FilePart filePart) throws Exception {

        File tempFile = File.createTempFile("upload-", filePart.filename());

        filePart.transferTo(tempFile).block();

        String content = extractText(tempFile, filePart.filename());

        List<Document> documents = List.of(new Document(content));

        TextSplitter splitter = new TokenTextSplitter();

        List<Document> chunks = splitter.apply(documents);

        vectorStore.add(chunks);

        tempFile.delete();

        return null;
    }

    private String extractText(File file, String filename) throws Exception {

        filename = filename.toLowerCase();

        if (filename.endsWith(".txt") || filename.endsWith(".md")) {

            return Files.readString(file.toPath());
        }

        if (filename.endsWith(".pdf")) {

            try (PDDocument pdf = Loader.loadPDF(file)) {

                PDFTextStripper stripper = new PDFTextStripper();

                return stripper.getText(pdf);
            }
        }

        throw new RuntimeException("Unsupported file type: " + filename);
    }


    public void ingest(MultipartFile file) throws IOException {

        List<Document> documents = readFile(file);

        TextSplitter splitter = new TokenTextSplitter();

        List<Document> chunks = splitter.apply(documents);

        vectorStore.add(chunks);
    }

    private List<Document> readFile(MultipartFile file) throws IOException {

        String filename = file.getOriginalFilename();

        assert filename != null;

        if (filename.endsWith(".txt") || filename.endsWith(".md")) {

            String content = new String(file.getBytes());

            return List.of(new Document(content));
        }

        if (filename.endsWith(".pdf")) {

            try (PDDocument pdf = Loader.loadPDF(file.getInputStream().readAllBytes())) {

                PDFTextStripper stripper = new PDFTextStripper();

                String text = stripper.getText(pdf);

                return List.of(new Document(text));
            }
        }

        throw new RuntimeException("Unsupported file type");
    }
}