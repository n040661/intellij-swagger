package org.zalando.intellij.swagger.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intellij.psi.PsiFile;
import com.intellij.util.LocalFileUrl;
import org.jetbrains.annotations.NotNull;
import org.zalando.intellij.swagger.file.FileContentManipulator;
import org.zalando.intellij.swagger.file.FileDetector;
import org.zalando.intellij.swagger.file.SwaggerUiCreator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SwaggerFileService {

    final private Map<String, Path> convertedSwaggerDocuments = new HashMap<>();
    final private SwaggerUiCreator swaggerUiCreator = new SwaggerUiCreator(new FileContentManipulator());
    final private FileDetector fileDetector = new FileDetector();

    public Optional<Path> convertSwaggerToHtml(@NotNull final PsiFile swaggerFile) {
        final String specificationContentAsJson = getSpecificationContentAsJson(swaggerFile);
        Path htmlSwaggerContentsDirectory = convertedSwaggerDocuments.get(swaggerFile.getVirtualFile().getPath());

        if (htmlSwaggerContentsDirectory != null) {
            LocalFileUrl indexPath = new LocalFileUrl(Paths.get(htmlSwaggerContentsDirectory.toString(), "index.html").toString());
            swaggerUiCreator.updateSwaggerUiFile(indexPath, specificationContentAsJson);
        } else {
            htmlSwaggerContentsDirectory = swaggerUiCreator.createSwaggerUiFiles(specificationContentAsJson)
                    .map(Paths::get)
                    .orElse(null);
            if (htmlSwaggerContentsDirectory != null) {
                convertedSwaggerDocuments.put(swaggerFile.getVirtualFile().getPath(), htmlSwaggerContentsDirectory);
            }
        }

        return Optional.ofNullable(htmlSwaggerContentsDirectory);
    }

    private String getSpecificationContentAsJson(final @NotNull PsiFile psiFile) {
        final String content = psiFile.getText();

        return fileDetector.isMainSwaggerJsonFile(psiFile) ? content : yamlToJson(content);
    }

    private String yamlToJson(final String content) {
        try {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            final JsonNode jsonNode = mapper.readTree(content);
            return new ObjectMapper().writeValueAsString(jsonNode);
        } catch (final Exception e) {
            return "{}";
        }
    }
}
