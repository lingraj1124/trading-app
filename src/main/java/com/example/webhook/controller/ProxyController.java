import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String TARGET_BASE = "http://64.227.143.158";

    @RequestMapping(value = "/**")
    public ResponseEntity<?> forward(
            HttpMethod method,
            HttpServletRequest request,
            @RequestBody(required = false) String body
    ) {

        try {
            // Extract path after /dhan
            String path = request.getRequestURI().replaceFirst("/proxy", "");

            String query = request.getQueryString();
            String url = TARGET_BASE + path + (query != null ? "?" + query : "");

            HttpEntity<String> entity = new HttpEntity<>(body);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    method,
                    entity,
                    String.class
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}
