package com.lenaneshcheret.taskmanager.controller;

import com.lenaneshcheret.taskmanager.controller.dto.ExportJobResponse;
import com.lenaneshcheret.taskmanager.controller.dto.ExportRequest;
import com.lenaneshcheret.taskmanager.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController {

  private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv");

  private final ExportService exportService;

  @PostMapping
  public ResponseEntity<ExportJobResponse> create(
      @Valid @RequestBody ExportRequest request,
      JwtAuthenticationToken authentication
  ) {
    ExportJobResponse response = ExportJobResponse.from(
        exportService.requestExport(request.projectId(), request.type(), authentication)
    );
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
  }

  @GetMapping("/{jobId}")
  public ExportJobResponse getStatus(@PathVariable Long jobId, JwtAuthenticationToken authentication) {
    return ExportJobResponse.from(exportService.getStatus(jobId, authentication));
  }

  @GetMapping("/{jobId}/download")
  public ResponseEntity<byte[]> download(@PathVariable Long jobId, JwtAuthenticationToken authentication) {
    ExportService.ExportDownload exportDownload = exportService.download(jobId, authentication);

    return ResponseEntity.ok()
        .contentType(CSV_MEDIA_TYPE)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(exportDownload.fileName()).build().toString()
        )
        .body(exportDownload.content());
  }
}
