package com.yawar.nextforgeai.service;


import com.yawar.nextforgeai.dto.StreamResponse;
import reactor.core.publisher.Flux;

import java.util.Optional;

public interface AiGenerationService {

    Flux<StreamResponse> streamResponse(String message, String projectId);
}
